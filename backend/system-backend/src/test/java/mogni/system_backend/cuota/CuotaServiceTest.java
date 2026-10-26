package mogni.system_backend.cuota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

/**
 * MOGNI-46: una fila por alumno activo con su última cuota y su situación de pago, ordenadas: en mora, vencidas y
 * después por vencimiento más cercano.
 */
@ExtendWith(MockitoExtension.class)
class CuotaServiceTest {

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private CuotaService cuotaService;

    private final LocalDate hoy = ZonaHoraria.hoy();
    private final List<Alumno> alumnos = new java.util.ArrayList<>();
    private final Map<Long, Cuota> ultimas = new HashMap<>();

    /** Alumno activo con una cuota que vence dentro de {@code diasHastaVencimiento} días (null = sin cuotas). */
    private Alumno alumno(long id, String apellido, Integer diasHastaVencimiento) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setNombre("Nombre" + id);
        alumno.setApellido(apellido);
        alumno.setDni("3000000" + id);
        alumno.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, true, null));
        Membresia membresia = new Membresia(7L, "Pro", 25000.0, 3, null);
        Inscripcion inscripcion = new Inscripcion(id, hoy.minusDays(60), null, EstadoInscripcion.ACTIVA, alumno, membresia);
        alumno.getInscripciones().add(inscripcion);
        if (diasHastaVencimiento != null) {
            Cuota cuota = inscripcion.agregarCuota(hoy.plusDays(diasHastaVencimiento).minusDays(30));
            cuota.setId(100 + id);
            ultimas.put(id, cuota);
        }
        alumnos.add(alumno);
        return alumno;
    }

    private List<CuotaResponse> listar() {
        when(alumnoRepository.findByCuentaActiva(true)).thenReturn(alumnos);
        when(cuotaRepository.buscarUltimasCuotas()).thenReturn(ultimas);
        return cuotaService.listarCuotas();
    }

    @Test
    void una_fila_por_alumno_con_los_datos_de_su_ultima_cuota() {
        alumno(1L, "Perez", 20);

        CuotaResponse fila = listar().get(0);

        assertEquals(1L, fila.getAlumnoId());
        assertEquals("Perez", fila.getAlumnoApellido());
        assertEquals(101L, fila.getCuotaId());
        assertEquals("Pro", fila.getMembresiaNombre());
        assertEquals(hoy.minusDays(10), fila.getFechaInicio());
        assertEquals(hoy.plusDays(20), fila.getFechaVencimiento());
        assertEquals(new BigDecimal("25000.00"), fila.getPrecioMembresia());
        assertEquals(SituacionPago.AL_DIA, fila.getSituacion());
        assertNull(fila.getDiasEnMora());
        assertFalse(fila.isPuedeRenovar());
        assertEquals(hoy.plusDays(10), fila.getRenovableDesde());
        assertEquals(hoy.plusDays(10), fila.getRenovableDesde());
    }

    @Test
    void con_un_cambio_programado_muestra_la_membresia_de_hoy_la_programada_y_el_precio_a_renovar() {
        Alumno alumno = alumno(1L, "Perez", 3);
        // Pagó por adelantado "Full": la cuota nueva (la última) está en la programada
        Inscripcion programada = Inscripcion.nueva(alumno, new Membresia(8L, "Full", 38000.0, 5, null),
                hoy.plusDays(3), EstadoInscripcion.PROGRAMADA);
        Cuota pagada = programada.agregarCuota(hoy.plusDays(3));
        pagada.setId(200L);
        ultimas.put(1L, pagada);

        CuotaResponse fila = listar().get(0);

        assertEquals("Pro", fila.getMembresiaNombre());
        assertEquals("Full", fila.getMembresiaProgramada());
        assertEquals(hoy.plusDays(3), fila.getProgramadaDesde());
        assertEquals(new BigDecimal("38000.00"), fila.getPrecioMembresia());
        assertEquals(hoy.plusDays(33), fila.getFechaVencimiento());
        assertEquals(SituacionPago.AL_DIA, fila.getSituacion());
        assertFalse(fila.isPuedeRenovar());
    }

    @Test
    void ordena_en_mora_vencidas_y_despues_por_vencimiento_mas_cercano() {
        alumno(1L, "AlDia", 25);
        alumno(2L, "Proxima", 3);
        alumno(3L, "Vencida", -1);
        alumno(4L, "MoraNueva", -2);
        alumno(5L, "MoraVieja", -6);
        alumno(6L, "SinCuotas", null);

        List<String> orden = listar().stream().map(CuotaResponse::getAlumnoApellido).toList();

        assertEquals(List.of("MoraVieja", "MoraNueva", "Vencida", "Proxima", "AlDia", "SinCuotas"), orden);
    }

    @Test
    void en_mora_muestra_los_dias_desde_que_entro() {
        alumno(1L, "Hoy", -2);
        alumno(2L, "Hace3", -5);

        List<CuotaResponse> filas = listar();

        assertEquals(3L, filas.get(0).getDiasEnMora());
        assertEquals(0L, filas.get(1).getDiasEnMora());
        assertTrue(filas.get(0).isPuedeRenovar());
    }

    @Test
    void puede_renovar_desde_10_dias_antes_del_vencimiento() {
        alumno(1L, "Diez", 10);
        alumno(2L, "Once", 11);

        List<CuotaResponse> filas = listar();

        assertTrue(filas.get(0).isPuedeRenovar());
        assertFalse(filas.get(1).isPuedeRenovar());
    }

    @Test
    void alumno_sin_cuotas_aparece_sin_situacion_y_sin_romper() {
        alumno(1L, "Viejo", null);

        CuotaResponse fila = listar().get(0);

        assertNull(fila.getSituacion());
        assertNull(fila.getFechaVencimiento());
        assertEquals("Pro", fila.getMembresiaNombre());
        assertTrue(fila.isPuedeRenovar());
    }

    @Test
    void solo_pide_los_alumnos_activos() {
        assertTrue(listar().isEmpty());
        // Los inactivos no se piden: findByCuentaActiva(false) nunca se llama (Mockito strict lo marcaría)
    }
}
