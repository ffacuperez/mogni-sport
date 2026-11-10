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

import mogni.system_backend.cuota.CuotasDePrueba;
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
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
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

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private InscripcionRepository inscripcionRepository;

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
        Cuota cuota = CuotasDePrueba.queVence(inscripcion, hoy.plusDays(dias));
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
        assertEquals(hoy.plusDays(3).plusMonths(1), response.getFechaVencimiento());
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
        assertEquals(hoy.plusMonths(1), response.getFechaVencimiento());
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

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L, null);

        assertTrue(vista.isPuedeRenovar());
        assertEquals(SituacionPago.EN_MORA, vista.getSituacion());
        assertEquals("Juan", vista.getNombre());
        assertEquals("30111222", vista.getDni());
        assertEquals("Pro", vista.getMembresiaNombre());
        assertEquals(hoy, vista.getFechaInicio());
        assertEquals(hoy.plusMonths(1), vista.getFechaVencimiento());
        assertEquals(new BigDecimal("15420.00"), vista.getPrecio());
        assertEquals(new BigDecimal("1550.00"), vista.getRecargo());
        assertEquals(new BigDecimal("16970.00"), vista.getTotal());
    }

    @Test
    void vista_previa_fuera_de_la_ventana_dice_por_que() {
        cuotaQueVenceEn(20);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L, null);

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
    void historial_despues_de_un_cambio_muestra_cada_pago_con_su_membresia_y_marca_el_del_cambio() {
        // Pagó en mora con "Pro" ($15.420 + recargo) y después pasó a una más cara: la diferencia es otra cuota y otro pago
        Cuota original = cuotaQueVenceEn(25);
        Pago pagoOriginal = Pago.de(original, new BigDecimal("1550.00"), MedioDePago.TRANSFERENCIA, hoy.minusDays(5));
        inscripcion.cancelar(hoy);
        Inscripcion nueva = new Inscripcion(6L, hoy, null, EstadoInscripcion.ACTIVA, alumno,
                new Membresia(8L, "Full", 38000.0, 5, null));
        Cuota diferencia = nueva.agregarCuotaPorCambio(hoy, original.getFechaVencimiento(), new BigDecimal("22580"));
        diferencia.setId(101L);
        Pago pagoDiferencia = Pago.de(diferencia, new BigDecimal("0.00"), MedioDePago.EFECTIVO, hoy);
        when(alumnoRepository.existsById(10L)).thenReturn(true);
        when(pagoRepository.buscarPorAlumno(10L)).thenReturn(List.of(pagoDiferencia, pagoOriginal));

        List<HistorialPagoResponse> historial = pagoService.listarHistorialDePagos(10L);

        assertEquals("Full", historial.get(0).getMembresiaNombre());
        assertEquals(new BigDecimal("22580.00"), historial.get(0).getMonto());
        assertEquals(hoy, historial.get(0).getFechaInicio());
        assertTrue(historial.get(0).isCambioDeMembresia());
        assertEquals("Pro", historial.get(1).getMembresiaNombre());
        assertEquals(new BigDecimal("16970.00"), historial.get(1).getMonto());
        assertEquals(new BigDecimal("1550.00"), historial.get(1).getRecargo());
        assertFalse(historial.get(1).isCambioDeMembresia());
    }

    // --- Renovar con otra membresía ---

    private final Membresia full = new Membresia(8L, "Full", 38000.0, 5, null);

    private PagoResponse registrarCon(Membresia elegida) {
        when(alumnoRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(elegida.getId())).thenReturn(Optional.of(elegida));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        RegistrarPagoRequest request = pago(MedioDePago.TRANSFERENCIA);
        request.setMembresiaId(elegida.getId());
        return pagoService.registrarPago(request);
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado) {
        return alumno.getInscripciones().stream().filter(i -> i.getEstado() == estado).findFirst().orElseThrow();
    }

    @Test
    void por_adelantado_con_otra_membresia_queda_programada_desde_el_vencimiento() {
        cuotaQueVenceEn(3);

        PagoResponse response = registrarCon(full);

        // La actual sigue activa hasta el vencimiento; la nueva queda programada con la cuota y el pago
        assertEquals(inscripcion, inscripcionCon(EstadoInscripcion.ACTIVA));
        Inscripcion programada = inscripcionCon(EstadoInscripcion.PROGRAMADA);
        assertEquals(full, programada.getMembresia());
        assertEquals(hoy.plusDays(3), programada.getFechaInicio());
        assertEquals(1, programada.getCuotas().size());
        verify(inscripcionRepository).save(programada);
        assertEquals("Full", response.getMembresiaNombre());
        assertEquals(hoy.plusDays(3), response.getFechaInicio());
        assertEquals(hoy.plusDays(3).plusMonths(1), response.getFechaVencimiento());
        assertEquals(new BigDecimal("38000.00"), response.getMonto());
        // Hasta cuándo pagó incluye la programada: sigue "Al día" y la ventana se cerró
        assertEquals(hoy.plusDays(3).plusMonths(1), alumno.buscarUltimaCuotaPagada().orElseThrow().getFechaVencimiento());
        assertEquals("Pro", alumno.buscarInscripcionActiva().orElseThrow().getMembresia().getNombre());
    }

    @Test
    void con_una_programada_no_puede_volver_a_pagar_el_mismo_mes() {
        cuotaQueVenceEn(3);
        registrarCon(full);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago(MedioDePago.EFECTIVO)));
        assertTrue(ex.getMessage().contains("Todavía no se puede registrar el pago"));
    }

    @Test
    void en_mora_con_otra_membresia_cambia_hoy_y_el_recargo_es_sobre_el_precio_nuevo() {
        cuotaQueVenceEn(-5);

        PagoResponse response = registrarCon(full);

        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        assertEquals(hoy, inscripcion.getFechaFin());
        Inscripcion nueva = inscripcionCon(EstadoInscripcion.ACTIVA);
        assertEquals(full, nueva.getMembresia());
        assertEquals(hoy, nueva.getFechaInicio());
        assertTrue(alumno.buscarInscripcionProgramada().isEmpty());
        assertEquals(hoy, response.getFechaInicio());
        assertEquals(new BigDecimal("38000.00"), response.getPrecio());
        // 10% de $38.000 = $3.800 (el total $41.800 ya es múltiplo de 10)
        assertEquals(new BigDecimal("3800.00"), response.getRecargo());
        assertEquals(new BigDecimal("41800.00"), response.getMonto());
    }

    @Test
    void vencida_con_otra_membresia_cambia_hoy_mantiene_la_cadena_y_sin_recargo() {
        cuotaQueVenceEn(-1);

        PagoResponse response = registrarCon(full);

        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        assertEquals(full, inscripcionCon(EstadoInscripcion.ACTIVA).getMembresia());
        assertEquals(hoy.minusDays(1), response.getFechaInicio());
        assertEquals(new BigDecimal("0.00"), response.getRecargo());
    }

    @Test
    void elegir_la_misma_membresia_es_una_renovacion_comun() {
        cuotaQueVenceEn(3);
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(inv -> inv.getArgument(0));

        registrarCon(membresia);

        assertEquals(1, alumno.getInscripciones().size());
        assertEquals(2, inscripcion.getCuotas().size());
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void vista_previa_con_otra_membresia_por_adelantado_dice_desde_cuando_rige() {
        cuotaQueVenceEn(4);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(8L)).thenReturn(Optional.of(full));

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L, 8L);

        assertTrue(vista.isPuedeRenovar());
        assertTrue(vista.isCambioDeMembresia());
        assertTrue(vista.isProgramada());
        assertEquals("Pro", vista.getMembresiaActual());
        assertEquals("Full", vista.getMembresiaNombre());
        assertEquals(hoy.plusDays(4), vista.getRigeDesde());
        assertEquals(new BigDecimal("38000.00"), vista.getTotal());
    }

    @Test
    void vista_previa_con_otra_membresia_en_mora_rige_desde_hoy() {
        cuotaQueVenceEn(-3);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(8L)).thenReturn(Optional.of(full));

        RenovacionResponse vista = pagoService.previsualizarRenovacion(10L, 8L);

        assertTrue(vista.isCambioDeMembresia());
        assertFalse(vista.isProgramada());
        assertEquals(hoy, vista.getRigeDesde());
        assertEquals(new BigDecimal("3800.00"), vista.getRecargo());
    }

    @Test
    void renovar_con_una_membresia_que_no_existe_falla() {
        cuotaQueVenceEn(3);
        when(alumnoRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(99L)).thenReturn(Optional.empty());
        RegistrarPagoRequest request = pago(MedioDePago.EFECTIVO);
        request.setMembresiaId(99L);

        assertThrows(RegistroInvalidoException.class, () -> pagoService.registrarPago(request));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void historial_de_alumno_inexistente_da_no_encontrado() {
        when(alumnoRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.listarHistorialDePagos(99L));
    }
}
