package mogni.system_backend.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.pago.dto.HistorialPagoResponse;
import mogni.system_backend.pago.dto.PagoResponse;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.dto.RenovacionResponse;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.pago.service.PagoService;

/** MOGNI-45 (renovación), MOGNI-80 (recargo por mora) y MOGNI-48 (historial). */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private PagoService pagoService;

    private final LocalDate hoy = ZonaHoraria.hoy();
    private Alumno alumno;
    private Membresia membresia;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        alumno = new Alumno();
        alumno.setId(10L);
        alumno.setNombre("Juan");
        alumno.setApellido("Perez");
        alumno.setDni("30111222");
        alumno.setCuenta(new Cuenta(1L, "juan@email.com", "hash", Rol.ALUMNO, false, true, null));
        membresia = new Membresia(7L, "Pro", 15420.0, 3, null);
        inscripcion = new Inscripcion(5L, hoy.minusDays(60), null, EstadoInscripcion.ACTIVA, alumno, membresia);
        alumno.getInscripciones().add(inscripcion);
    }

    /** Última cuota del alumno: vence dentro de {@code dias} días (negativo si ya venció). */
    private Cuota cuotaQueVenceEn(int dias) {
        Cuota cuota = inscripcion.agregarCuota(hoy.plusDays(dias).minusDays(30));
        cuota.setId(100L);
        return cuota;
    }

    private RegistrarPagoRequest pago(MedioDePago medio) {
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setAlumnoId(10L);
        request.setMedioDePago(medio);
        return request;
    }

    private PagoResponse registrar() {
        when(alumnoRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(inv -> {
            Cuota c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        return pagoService.registrarPago(pago(MedioDePago.EFECTIVO));
    }

    private void registrarYEsperarRechazo(String parteDelMensaje) {
        when(alumnoRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(alumno));
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago(MedioDePago.EFECTIVO)));
        assertTrue(ex.getMessage().contains(parteDelMensaje), ex.getMessage());
        verify(cuotaRepository, never()).save(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void renovar_3_dias_antes_empieza_en_el_vencimiento_anterior_al_precio_vigente() {
        cuotaQueVenceEn(3);

        PagoResponse response = registrar();

        assertEquals(hoy.plusDays(3), response.getFechaInicio());
        assertEquals(hoy.plusDays(33), response.getFechaVencimiento());
        assertEquals(new BigDecimal("15420.00"), response.getPrecio());
        assertEquals(new BigDecimal("0.00"), response.getRecargo());
        assertEquals(new BigDecimal("15420.00"), response.getMonto());
        assertEquals(hoy, response.getFechaPago());
        assertEquals(MedioDePago.EFECTIVO, response.getMedioDePago());
        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(2, inscripcion.getCuotas().size());
    }

    @Test
    void renovar_10_dias_antes_se_permite() {
        cuotaQueVenceEn(10);

        assertEquals(hoy.plusDays(10), registrar().getFechaInicio());
    }

    @Test
    void renovar_11_dias_antes_falla() {
        cuotaQueVenceEn(11);

        registrarYEsperarRechazo("se habilita 10 días antes del vencimiento");
    }

    @Test
    void renovar_vencida_mantiene_la_cadena_y_no_cobra_recargo() {
        cuotaQueVenceEn(-1);

        PagoResponse response = registrar();

        assertEquals(hoy.minusDays(1), response.getFechaInicio());
        assertEquals(new BigDecimal("0.00"), response.getRecargo());
    }

    @Test
    void renovar_en_mora_empieza_hoy_y_cobra_recargo_redondeado() {
        cuotaQueVenceEn(-5);

        PagoResponse response = registrar();

        assertEquals(hoy, response.getFechaInicio());
        assertEquals(hoy.plusDays(30), response.getFechaVencimiento());
        assertEquals(new BigDecimal("15420.00"), response.getPrecio());
        assertEquals(new BigDecimal("1550.00"), response.getRecargo());
        assertEquals(new BigDecimal("16970.00"), response.getMonto());
    }

    @Test
    void el_pago_se_guarda_con_la_cuota_nueva_el_recargo_y_el_total() {
        cuotaQueVenceEn(-2);

        registrar();

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        Pago pago = captor.getValue();
        assertEquals(200L, pago.getCuota().getId());
        assertEquals(new BigDecimal("1550.00"), pago.getRecargo());
        assertEquals(new BigDecimal("16970.00"), pago.getMonto());
        assertEquals(hoy, pago.getFechaPago());
    }

    @Test
    void renovar_dos_veces_seguidas_por_adelantado_la_segunda_falla() {
        cuotaQueVenceEn(5);
        registrar();

        // La cuota nueva vence en 35 días: ya no está dentro de la ventana
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago(MedioDePago.EFECTIVO)));
        assertTrue(ex.getMessage().contains("Todavía no se puede registrar el pago"));
    }

    @Test
    void registrar_un_pago_a_un_alumno_inactivo_falla() {
        cuotaQueVenceEn(-1);
        alumno.getCuenta().setActiva(false);

        registrarYEsperarRechazo("alumno inactivo");
    }

    @Test
    void registrar_un_pago_sin_inscripcion_activa_falla() {
        inscripcion.cancelar(hoy);

        registrarYEsperarRechazo("no tiene una membresía activa");
    }

    @Test
    void alumno_inexistente_da_no_encontrado() {
        when(alumnoRepository.findByIdConBloqueo(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.registrarPago(pago(MedioDePago.EFECTIVO)));
    }

    @Test
    void sin_cuotas_previas_el_periodo_empieza_hoy_sin_recargo() {
        PagoResponse response = registrar();

        assertEquals(hoy, response.getFechaInicio());
        assertEquals(new BigDecimal("0.00"), response.getRecargo());
    }

    @Test
    void vista_previa_en_mora_muestra_periodo_precio_recargo_y_total() {
        cuotaQueVenceEn(-3);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L);

        assertTrue(vista.isPuedeRenovar());
        assertEquals(SituacionPago.EN_MORA, vista.getSituacion());
        assertEquals("Juan", vista.getNombre());
        assertEquals("30111222", vista.getDni());
        assertEquals("Pro", vista.getMembresiaNombre());
        assertEquals(hoy, vista.getFechaInicio());
        assertEquals(hoy.plusDays(30), vista.getFechaVencimiento());
        assertEquals(new BigDecimal("15420.00"), vista.getPrecio());
        assertEquals(new BigDecimal("1550.00"), vista.getRecargo());
        assertEquals(new BigDecimal("16970.00"), vista.getTotal());
    }

    @Test
    void vista_previa_fuera_de_la_ventana_dice_por_que() {
        cuotaQueVenceEn(20);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L);

        assertFalse(vista.isPuedeRenovar());
        assertEquals(SituacionPago.AL_DIA, vista.getSituacion());
        assertTrue(vista.getMotivo().contains("se habilita 10 días antes"));
    }

    @Test
    void historial_muestra_periodo_membresia_y_desglose_del_recargo() {
        Cuota cuota = cuotaQueVenceEn(25);
        Pago pago = Pago.de(cuota, new BigDecimal("1550.00"), MedioDePago.TRANSFERENCIA, hoy.minusDays(5));
        pago.setId(50L);
        when(alumnoRepository.existsById(10L)).thenReturn(true);
        when(pagoRepository.buscarPorAlumno(10L)).thenReturn(List.of(pago));

        HistorialPagoResponse item = pagoService.listarHistorialDePagos(10L).get(0);

        assertEquals(50L, item.getPagoId());
        assertEquals(hoy.minusDays(5), item.getFechaPago());
        assertEquals(MedioDePago.TRANSFERENCIA, item.getMedioDePago());
        assertEquals(cuota.getFechaInicio(), item.getFechaInicio());
        assertEquals(cuota.getFechaVencimiento(), item.getFechaVencimiento());
        assertEquals("Pro", item.getMembresiaNombre());
        assertEquals(new BigDecimal("15420.00"), item.getPrecio());
        assertEquals(new BigDecimal("1550.00"), item.getRecargo());
        assertEquals(new BigDecimal("16970.00"), item.getMonto());
    }

    @Test
    void historial_muestra_la_membresia_de_cada_cuota_aunque_despues_cambie() {
        Cuota cuota = cuotaQueVenceEn(25);
        cuota.setMembresia(new Membresia(8L, "Vieja", 10000.0, 2, null));
        Pago pago = Pago.de(cuota, BigDecimal.ZERO, MedioDePago.EFECTIVO, hoy);
        when(alumnoRepository.existsById(10L)).thenReturn(true);
        when(pagoRepository.buscarPorAlumno(10L)).thenReturn(List.of(pago));

        assertEquals("Vieja", pagoService.listarHistorialDePagos(10L).get(0).getMembresiaNombre());
    }

    @Test
    void historial_de_alumno_inexistente_da_no_encontrado() {
        when(alumnoRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.listarHistorialDePagos(99L));
    }
}
