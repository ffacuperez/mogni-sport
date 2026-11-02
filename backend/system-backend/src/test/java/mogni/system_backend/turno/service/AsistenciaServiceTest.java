package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.AsistenciaResponse;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-262. El repositorio de reservas se simula con una lista en memoria. "Ahora" es el martes 27/10/2026 17:20,
 * con el turno de 17 a 18 en curso.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    private static final LocalDate MARTES = LocalDate.of(2026, 10, 27);
    private static final LocalDate MIERCOLES = MARTES.plusDays(1);
    private static final LocalDateTime AHORA = MARTES.atTime(17, 20);

    @Mock
    private ReservaRepository reservaRepository;

    private AsistenciaService asistenciaService;
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        asistenciaService = new AsistenciaService(reservaRepository);
        lenient().when(reservaRepository.findByIdConBloqueo(anyLong()))
                .thenAnswer(inv -> reservas.stream().filter(r -> r.getId().equals(inv.getArgument(0))).findFirst());
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reservaRepository.findByTurnoIdInAndEstado(anyCollection(), any(EstadoReserva.class)))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(0);
                    EstadoReserva estado = inv.getArgument(1);
                    return reservas.stream()
                            .filter(r -> ids.contains(r.getTurno().getId()) && r.getEstado() == estado)
                            .toList();
                });
    }

    // --- Armado del escenario ---

    private Turno turno(LocalDate fecha, String inicio) {
        LocalTime horaInicio = LocalTime.parse(inicio);
        return new Turno(proximoId++, fecha, horaInicio, horaInicio.plusHours(1), 8, EstadoTurno.DISPONIBLE);
    }

    private Reserva reservar(Turno turno, EstadoReserva estado) {
        Alumno alumno = new Alumno();
        alumno.setId(proximoId++);
        Reserva reserva = new Reserva(proximoId++, alumno, turno, turno.getFecha().minusDays(1).atTime(9, 0), estado);
        reservas.add(reserva);
        return reserva;
    }

    private Reserva reservar(Turno turno) {
        return reservar(turno, EstadoReserva.RESERVADA);
    }

    // --- Pruebas de aceptación ---

    // Prueba 1: marcar como «Presente» a un alumno en un turno que ya comenzó (pasa)
    @Test
    void marcar_presente_en_un_turno_que_ya_comenzo() {
        Turno enCurso = turno(MARTES, "17:00");
        Reserva reserva = reservar(enCurso);

        AsistenciaResponse respuesta = asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA);

        assertEquals(Asistencia.PRESENTE, reserva.getAsistencia());
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
        assertEquals(reserva.getId(), respuesta.getReservaId());
        assertEquals(enCurso.getId(), respuesta.getTurnoId());
        assertEquals(Asistencia.PRESENTE, respuesta.getAsistencia());
        verify(reservaRepository).save(reserva);
    }

    // Prueba 2: marcar como «Ausente» a un alumno en un turno que ya comenzó (pasa)
    @Test
    void marcar_ausente_en_un_turno_que_ya_comenzo() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));

        asistenciaService.registrar(reserva.getId(), Asistencia.AUSENTE, AHORA);

        assertEquals(Asistencia.AUSENTE, reserva.getAsistencia());
    }

    @Test
    void se_puede_marcar_justo_a_la_hora_de_inicio() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));

        asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, MARTES.atTime(17, 0));

        assertEquals(Asistencia.PRESENTE, reserva.getAsistencia());
    }

    @Test
    void se_puede_marcar_un_turno_de_hoy_que_ya_termino() {
        Reserva reserva = reservar(turno(MARTES, "09:00"));

        asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA);

        assertEquals(Asistencia.PRESENTE, reserva.getAsistencia());
    }

    // Prueba 3: registrar asistencia en un turno que todavía no comenzó (falla)
    @Test
    void no_se_puede_registrar_en_un_turno_que_todavia_no_comenzo() {
        Reserva reserva = reservar(turno(MARTES, "18:00"));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA));

        assertTrue(error.getMessage().contains("18:00"));
        assertNull(reserva.getAsistencia());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void no_se_puede_registrar_un_minuto_antes_del_inicio() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));

        assertThrows(ConflictoException.class,
                () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, MARTES.atTime(16, 59)));
    }

    @Test
    void no_se_puede_registrar_en_un_turno_de_maniana() {
        Reserva reserva = reservar(turno(MIERCOLES, "08:00"));

        assertThrows(ConflictoException.class,
                () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA));
    }

    // Prueba 4: corregir una marca el mismo día (pasa)
    @Test
    void se_puede_corregir_la_marca_el_mismo_dia() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));
        asistenciaService.registrar(reserva.getId(), Asistencia.AUSENTE, AHORA);

        asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, MARTES.atTime(23, 59));

        assertEquals(Asistencia.PRESENTE, reserva.getAsistencia());
    }

    // Prueba 4: corregir una marca al día siguiente (falla)
    @Test
    void no_se_puede_corregir_la_marca_al_dia_siguiente() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));
        asistenciaService.registrar(reserva.getId(), Asistencia.AUSENTE, AHORA);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, MIERCOLES.atTime(0, 1)));

        assertTrue(error.getMessage().contains("final del día"));
        assertEquals(Asistencia.AUSENTE, reserva.getAsistencia());
    }

    // Prueba 5: un alumno ausente no recupera el turno en su contador semanal y no recibe clase extra (pasa)
    @Test
    void ausente_sigue_reservada_y_no_genera_clase_extra() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));

        asistenciaService.registrar(reserva.getId(), Asistencia.AUSENTE, AHORA);

        // El contador semanal (HU-259/254) y los lugares cuentan las RESERVADA: la ausente sigue contando.
        // AsistenciaService no depende de ClaseExtraRepository: marcar ausente no puede dar una clase extra.
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
        assertEquals(Asistencia.AUSENTE, reserva.getAsistencia());
    }

    @Test
    void no_se_puede_registrar_en_una_reserva_cancelada() {
        Turno enCurso = turno(MARTES, "17:00");
        for (EstadoReserva estado : List.of(EstadoReserva.CANCELADA, EstadoReserva.CANCELADA_POR_GIMNASIO,
                EstadoReserva.CANCELADA_POR_SISTEMA)) {
            Reserva reserva = reservar(enCurso, estado);

            assertThrows(ConflictoException.class,
                    () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA));
            assertNull(reserva.getAsistencia());
        }
    }

    @Test
    void no_se_puede_registrar_en_un_turno_cancelado() {
        Turno cancelado = turno(MARTES, "17:00");
        cancelado.cancelar(MotivoCancelacionTurno.MANUAL);
        Reserva reserva = reservar(cancelado);

        assertThrows(ConflictoException.class,
                () -> asistenciaService.registrar(reserva.getId(), Asistencia.PRESENTE, AHORA));
    }

    @Test
    void reserva_inexistente_responde_no_encontrado() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> asistenciaService.registrar(999L, Asistencia.PRESENTE, AHORA));
    }

    @Test
    void sin_asistencia_indicada_es_invalido() {
        Reserva reserva = reservar(turno(MARTES, "17:00"));

        assertThrows(RegistroInvalidoException.class,
                () -> asistenciaService.registrar(reserva.getId(), null, AHORA));
    }

    // Prueba 7 (apoyo): la respuesta trae el conteo del turno para actualizar la lista de la agenda
    @Test
    void la_respuesta_trae_presentes_ausentes_y_sin_marcar_del_turno() {
        Turno enCurso = turno(MARTES, "17:00");
        Reserva lucia = reservar(enCurso);
        Reserva martin = reservar(enCurso);
        reservar(enCurso);
        reservar(enCurso, EstadoReserva.CANCELADA);
        reservar(turno(MARTES, "16:00"));
        asistenciaService.registrar(lucia.getId(), Asistencia.PRESENTE, AHORA);

        AsistenciaResponse respuesta = asistenciaService.registrar(martin.getId(), Asistencia.AUSENTE, AHORA);

        assertEquals(1, respuesta.getPresentes());
        assertEquals(1, respuesta.getAusentes());
        assertEquals(1, respuesta.getSinMarcar());
    }

    // --- Cierre del día ---

    // Prueba 6: una reserva sin marcar queda como «Ausente» al finalizar el día (pasa)
    @Test
    void el_cierre_pasa_a_ausente_las_reservas_sin_marcar_de_dias_anteriores() {
        when(reservaRepository.registrarAsistenciaSinMarcarAntesDe(MIERCOLES, EstadoReserva.RESERVADA,
                Asistencia.AUSENTE)).thenReturn(3);

        assertEquals(3, asistenciaService.cerrarDiasAnteriores(MIERCOLES));
    }

    @Test
    void la_asistencia_efectiva_de_un_dia_terminado_sin_marcar_es_ausente() {
        Reserva sinMarcarDeAyer = reservar(turno(MARTES, "17:00"));
        Reserva sinMarcarDeHoy = reservar(turno(MIERCOLES, "08:00"));
        Reserva presenteDeAyer = reservar(turno(MARTES, "18:00"));
        presenteDeAyer.setAsistencia(Asistencia.PRESENTE);

        assertEquals(Asistencia.AUSENTE, AsistenciaService.asistenciaEfectiva(sinMarcarDeAyer, MIERCOLES));
        assertNull(AsistenciaService.asistenciaEfectiva(sinMarcarDeHoy, MIERCOLES));
        assertEquals(Asistencia.PRESENTE, AsistenciaService.asistenciaEfectiva(presenteDeAyer, MIERCOLES));
    }

    @Test
    void se_puede_registrar_solo_desde_el_inicio_y_hasta_el_final_del_dia() {
        Turno enCurso = turno(MARTES, "17:00");
        Turno cancelado = turno(MARTES, "16:00");
        cancelado.cancelar(MotivoCancelacionTurno.MANUAL);
        Turno sinGenerar = new Turno(MARTES, new mogni.system_backend.turno.entity.HorarioTurno(
                LocalTime.of(15, 0), LocalTime.of(16, 0)), 8);

        assertFalse(AsistenciaService.sePuedeRegistrar(enCurso, MARTES.atTime(16, 59)));
        assertTrue(AsistenciaService.sePuedeRegistrar(enCurso, MARTES.atTime(17, 0)));
        assertTrue(AsistenciaService.sePuedeRegistrar(enCurso, MARTES.atTime(23, 59)));
        assertFalse(AsistenciaService.sePuedeRegistrar(enCurso, MIERCOLES.atTime(0, 0)));
        assertFalse(AsistenciaService.sePuedeRegistrar(cancelado, AHORA));
        assertFalse(AsistenciaService.sePuedeRegistrar(sinGenerar, AHORA));
    }
}
