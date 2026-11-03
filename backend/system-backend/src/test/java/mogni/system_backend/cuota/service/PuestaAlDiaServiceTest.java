package mogni.system_backend.cuota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.plan.service.PlanAsignadoService;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

/** Nueva HU "Gestionar alumnos en mora": cancelación de turnos en mora (sin baja automática, decidido 24/10). */
@ExtendWith(MockitoExtension.class)
class PuestaAlDiaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final LocalDateTime AHORA = HOY.atTime(LocalTime.of(9, 0));

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private CancelacionPorSistemaService cancelacionPorSistemaService;

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private PlanAsignadoService planAsignadoService;

    private PuestaAlDiaService puestaAlDiaService;
    private final Map<Long, Cuota> ultimas = new HashMap<>();

    @BeforeEach
    void setUp() {
        puestaAlDiaService = new PuestaAlDiaService(cuotaRepository, inscripcionRepository, cancelacionPorSistemaService,
                planAsignadoService);
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

    // --- Activación de inscripciones programadas ---

    /** El alumno pagó por adelantado "Full" desde {@code inicio}: su programada, con la cuota pagada. */
    private Inscripcion programadaDesde(Alumno alumno, LocalDate inicio) {
        Inscripcion programada = Inscripcion.nueva(alumno, new Membresia(8L, "Full", 38000.0, 5, null), inicio,
                EstadoInscripcion.PROGRAMADA);
        programada.agregarCuota(inicio);
        return programada;
    }

    @Test
    void el_dia_que_empieza_la_programada_pasa_a_activa_y_la_anterior_termina_ese_dia() {
        Alumno alumno = alumnoQueVencioHace(1L, 0);
        Inscripcion anterior = alumno.getInscripciones().get(0);
        Inscripcion programada = programadaDesde(alumno, HOY);
        when(inscripcionRepository.findByEstadoAndFechaInicioLessThanEqual(EstadoInscripcion.PROGRAMADA, HOY))
                .thenReturn(List.of(programada));

        puestaAlDiaService.ponerAlDia(AHORA);

        assertEquals(EstadoInscripcion.ACTIVA, programada.getEstado());
        assertNull(programada.getFechaFin());
        assertEquals(EstadoInscripcion.CANCELADA, anterior.getEstado());
        assertEquals(HOY, anterior.getFechaFin());
        assertEquals("Full", alumno.buscarInscripcionActiva().orElseThrow().getMembresia().getNombre());
    }

    @Test
    void si_un_dia_no_corrio_la_activa_despues_y_la_anterior_termina_el_dia_que_empezaba() {
        Alumno alumno = alumnoQueVencioHace(1L, 3);
        Inscripcion anterior = alumno.getInscripciones().get(0);
        Inscripcion programada = programadaDesde(alumno, HOY.minusDays(3));
        when(inscripcionRepository.findByEstadoAndFechaInicioLessThanEqual(EstadoInscripcion.PROGRAMADA, HOY))
                .thenReturn(List.of(programada));

        puestaAlDiaService.ponerAlDia(AHORA);

        assertEquals(EstadoInscripcion.ACTIVA, programada.getEstado());
        assertEquals(HOY.minusDays(3), anterior.getFechaFin());
    }

    @Test
    void activa_las_programadas_antes_de_revisar_la_mora() {
        puestaAlDiaService.ponerAlDia(AHORA);

        InOrder orden = inOrder(inscripcionRepository, cuotaRepository);
        orden.verify(inscripcionRepository).findByEstadoAndFechaInicioLessThanEqual(EstadoInscripcion.PROGRAMADA, HOY);
        orden.verify(cuotaRepository).buscarUltimasCuotas();
    }

    @Test
    void con_el_mes_siguiente_pagado_en_una_programada_no_se_le_cancelan_los_turnos() {
        Alumno alumno = alumnoQueVencioHace(1L, 3); // la cuota de la activa venció hace 3 días...
        // ...pero la última cuota pagada es la de la programada, que cubre hoy
        Inscripcion programada = programadaDesde(alumno, HOY.minusDays(3));
        ultimas.put(1L, programada.getCuotas().get(0));

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
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
    void no_hay_baja_automatica_aunque_lleve_muchos_dias_en_mora() {
        Alumno alumno = alumnoQueVencioHace(1L, 32); // 30 días en mora

        puestaAlDiaService.ponerAlDia(AHORA);

        // La baja la hace el propietario a mano: solo se le cancelan los turnos
        assertTrue(alumno.getCuenta().isActiva());
        assertNull(alumno.getCuenta().getFechaBaja());
        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(0).getEstado());
        verify(cancelacionPorSistemaService).cancelarReservasFuturas(1L, AHORA);
    }

    @Test
    void es_idempotente_se_puede_llamar_varias_veces() {
        Alumno alumno = alumnoQueVencioHace(1L, 9);

        puestaAlDiaService.ponerAlDia(AHORA);
        puestaAlDiaService.ponerAlDia(AHORA);

        // Cancelar las reservas futuras otra vez no hace nada: ya no le quedan reservas RESERVADA
        assertTrue(alumno.getCuenta().isActiva());
        verify(cancelacionPorSistemaService, times(2)).cancelarReservasFuturas(1L, AHORA);
    }

    @Test
    void si_renueva_en_mora_deja_de_cancelarle_turnos() {
        Alumno alumno = alumnoQueVencioHace(1L, 5);
        // Renovó en mora: la cuota nueva empieza hoy
        Cuota nueva = alumno.getInscripciones().get(0).agregarCuota(HOY);
        ultimas.put(1L, nueva);

        puestaAlDiaService.ponerAlDia(AHORA.plusDays(4));

        assertTrue(alumno.getCuenta().isActiva());
        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
    }

    // --- HU-266: planes de entrenamiento vencidos ---

    @Test
    void cierra_los_planes_de_entrenamiento_vencidos_con_la_fecha_del_dia() {
        when(planAsignadoService.cerrarVencidos(HOY)).thenReturn(2);

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(planAsignadoService).cerrarVencidos(HOY);
    }

    @Test
    void cierra_los_planes_vencidos_aunque_nadie_este_en_mora() {
        alumnoQueVencioHace(1L, -5);

        puestaAlDiaService.ponerAlDia(AHORA);

        verify(planAsignadoService, times(1)).cerrarVencidos(HOY);
        verify(cancelacionPorSistemaService, never()).cancelarReservasFuturas(anyLong(), any());
    }
}
