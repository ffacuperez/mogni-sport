package mogni.system_backend.cuota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.service.BajaAlumnoService;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

/** Nueva HU "Gestionar alumnos en mora": cancelación de turnos en mora y baja automática a los 7 días. */
@ExtendWith(MockitoExtension.class)
class PuestaAlDiaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final LocalDateTime AHORA = HOY.atTime(LocalTime.of(9, 0));

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private CancelacionPorSistemaService cancelacionPorSistemaService;

    private PuestaAlDiaService puestaAlDiaService;
    private final Map<Long, Cuota> ultimas = new HashMap<>();

    @BeforeEach
    void setUp() {
        // La baja real (con la cancelación mockeada), así se ve que desactiva la cuenta y cancela la inscripción
        puestaAlDiaService = new PuestaAlDiaService(cuotaRepository, cancelacionPorSistemaService,
                new BajaAlumnoService(cancelacionPorSistemaService));
        when(cuotaRepository.buscarUltimasCuotas()).thenReturn(ultimas);
    }

    /** Alumno activo cuya última cuota venció hace {@code diasDesdeVencimiento} días (negativo = todavía no venció). */
    private Alumno alumnoQueVencioHace(long id, int diasDesdeVencimiento) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, true, null));
        Inscripcion inscripcion = new Inscripcion(id, HOY.minusDays(90), null, EstadoInscripcion.ACTIVA, alumno,
                new Membresia(7L, "Pro", 25000.0, 3, null));
        alumno.getInscripciones().add(inscripcion);
        Cuota cuota = inscripcion.agregarCuota(HOY.minusDays(diasDesdeVencimiento).minusDays(30));
        ultimas.put(id, cuota);
        return alumno;
    }

    @Test
    void en_mora_cancela_los_turnos_sin_darlo_de_baja() {
        Alumno alumno = alumnoQueVencioHace(1L, 2); // entró en mora hoy (día 0)

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(cancelacionPorSistemaService).cancelarReservasFuturas(1L, AHORA);
        assertTrue(alumno.getCuenta().isActiva());
    }

    @Test
    void vencida_conserva_sus_reservas() {
        alumnoQueVencioHace(1L, 1);

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
    }

    @Test
    void al_dia_y_proxima_a_vencer_no_se_tocan() {
        alumnoQueVencioHace(1L, -20);
        alumnoQueVencioHace(2L, 0);

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
    }

    @Test
    void con_6_dias_en_mora_sigue_activo() {
        Alumno alumno = alumnoQueVencioHace(1L, 8); // 8 días desde el vencimiento = 6 días en mora

        puestaAlDiaService.ponerAlDia(AHORA);

        assertTrue(alumno.getCuenta().isActiva());
        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(0).getEstado());
    }

    @Test
    void con_7_dias_en_mora_se_da_de_baja_igual_que_la_baja_manual() {
        Alumno alumno = alumnoQueVencioHace(1L, 9); // 7 días en mora

        puestaAlDiaService.ponerAlDia(AHORA);

        assertFalse(alumno.getCuenta().isActiva());
        assertEquals(HOY, alumno.getCuenta().getFechaBaja());
        assertEquals(EstadoInscripcion.CANCELADA, alumno.getInscripciones().get(0).getEstado());
        assertEquals(HOY, alumno.getInscripciones().get(0).getFechaFin());
        verify(cancelacionPorSistemaService).cancelarReservasFuturas(1L, AHORA);
    }

    @Test
    void es_idempotente_un_alumno_ya_dado_de_baja_no_se_vuelve_a_procesar() {
        Alumno alumno = alumnoQueVencioHace(1L, 9);
        puestaAlDiaService.ponerAlDia(AHORA);
        // La consulta de últimas cuotas solo trae alumnos con cuenta activa
        ultimas.clear();

        puestaAlDiaService.ponerAlDia(AHORA);

        assertFalse(alumno.getCuenta().isActiva());
        verify(cancelacionPorSistemaService, times(1)).cancelarReservasFuturas(anyLong(), any());
    }

    @Test
    void si_renueva_antes_de_los_7_dias_no_se_da_de_baja() {
        Alumno alumno = alumnoQueVencioHace(1L, 5);
        // Renovó en mora: la cuota nueva empieza hoy
        Cuota nueva = alumno.getInscripciones().get(0).agregarCuota(HOY);
        ultimas.put(1L, nueva);

        puestaAlDiaService.ponerAlDia(AHORA.plusDays(4));

        assertTrue(alumno.getCuenta().isActiva());
        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
    }
}
