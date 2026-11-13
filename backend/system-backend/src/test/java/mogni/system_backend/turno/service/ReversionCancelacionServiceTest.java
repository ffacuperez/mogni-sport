package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.turno.dto.ReversionCancelacionResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

@ExtendWith(MockitoExtension.class)
class ReversionCancelacionServiceTest {

    /** Un martes. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime AHORA = MARTES.atTime(7, 0);
    private static final int CUPO_VIGENTE = 8;

    @Mock private TurnoRepository turnoRepository;
    @Mock private FranjaHorariaRepository franjaHorariaRepository;
    @Mock private ConfiguracionGimnasioService configuracionGimnasioService;
    @Mock private EntityManager entityManager;

    private final List<Turno> turnos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private long siguienteId = 100;
    private ReversionCancelacionService service;

    @BeforeEach
    void setUp() {
        service = new ReversionCancelacionService(turnoRepository, franjaHorariaRepository,
                configuracionGimnasioService, entityManager);
        lenient().when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(CUPO_VIGENTE);
        lenient().when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.TUESDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        lenient().when(turnoRepository.existsById(anyLong())).thenAnswer(inv -> turnos.stream()
                .anyMatch(turno -> turno.getId().equals(inv.<Long>getArgument(0))));
        lenient().when(turnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv -> turnos.stream()
                .filter(turno -> turno.getId().equals(inv.<Long>getArgument(0))).findFirst());
    }

    private Turno turno(String hora, EstadoTurno estado, MotivoCancelacionTurno motivo) {
        LocalTime inicio = LocalTime.parse(hora);
        Turno turno = new Turno(siguienteId++, MARTES, inicio, inicio.plusHours(1), 3, estado, motivo);
        turnos.add(turno);
        return turno;
    }

    private Turno cancelado(String hora) {
        return turno(hora, EstadoTurno.CANCELADO, MotivoCancelacionTurno.MANUAL);
    }

    private Reserva reserva(long alumnoId, Turno turno, EstadoReserva estado) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Reserva reserva = new Reserva(siguienteId++, alumno, turno, AHORA.minusDays(1), estado);
        reservas.add(reserva);
        return reserva;
    }

    @Test
    void reactivaUnTurnoCanceladoManualmenteConElCupoVigente() {
        Turno turno = cancelado("09:00");

        ReversionCancelacionResponse respuesta = service.revertir(turno.getId(), AHORA);

        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
        assertNull(turno.getMotivoCancelacion());
        assertEquals(CUPO_VIGENTE, turno.getCupoMaximo());
        assertEquals(turno.getId(), respuesta.getTurnoId());
        verify(turnoRepository).save(turno);
    }

    @Test
    void tambienReactivaUnTurnoCanceladoSinMotivoDeVersionesAnteriores() {
        Turno turno = turno("09:00", EstadoTurno.CANCELADO, null);

        service.revertir(turno.getId(), AHORA);

        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
    }

    @Test
    void lasReservasCanceladasNoSeRestauranNiSeAvisaALosAlumnos() {
        Turno turno = cancelado("09:00");
        Reserva primera = reserva(1L, turno, EstadoReserva.CANCELADA_POR_GIMNASIO);
        Reserva segunda = reserva(2L, turno, EstadoReserva.CANCELADA_POR_GIMNASIO);

        service.revertir(turno.getId(), AHORA);

        // Conservan su clase a recuperar y, si quieren, lo reservan de nuevo
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, primera.getEstado());
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, segunda.getEstado());
        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
    }

    @Test
    void noCreaClasesExtraNiTocaLasReservasActivas() {
        Turno turno = cancelado("09:00");
        Reserva nueva = reserva(1L, turno, EstadoReserva.RESERVADA);

        service.revertir(turno.getId(), AHORA);

        assertEquals(EstadoReserva.RESERVADA, nueva.getEstado());
    }

    @Test
    void unTurnoCanceladoPorCambioDeFranjaNoSePuedeReactivarAMano() {
        Turno turno = turno("09:00", EstadoTurno.CANCELADO, MotivoCancelacionTurno.FRANJA_ELIMINADA);
        reserva(1L, turno, EstadoReserva.CANCELADA_POR_GIMNASIO);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> service.revertir(turno.getId(), AHORA));

        assertTrue(ex.getMessage().contains("ya no está en la agenda"));
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @Test
    void unTurnoQueNoEstaCanceladoDaConflicto() {
        Turno turno = turno("09:00", EstadoTurno.DISPONIBLE, null);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> service.revertir(turno.getId(), AHORA));

        assertEquals("Este turno no está cancelado.", ex.getMessage());
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void unTurnoQueYaEmpezoNoSePuedeReactivar() {
        Turno turno = cancelado("09:00");

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> service.revertir(turno.getId(), MARTES.atTime(9, 0)));

        assertEquals("Este turno ya empezó o ya pasó y no se puede reactivar.", ex.getMessage());
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @Test
    void unTurnoQueEmpiezaEnUnMinutoTodaviaSePuedeReactivar() {
        Turno turno = cancelado("09:00");

        service.revertir(turno.getId(), MARTES.atTime(8, 59));

        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
    }

    @Test
    void sinUnaFranjaQueLoRespaldeDaConflicto() {
        Turno turno = cancelado("15:00");

        ConflictoException ex = assertThrows(ConflictoException.class, () -> service.revertir(turno.getId(), AHORA));

        assertTrue(ex.getMessage().startsWith("No hay una franja horaria que incluya este turno."));
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @Test
    void unTurnoInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.revertir(12345L, AHORA));
    }

    @Test
    void siOtroProcesoLoReactivoAntesDeConseguirElLockDaConflicto() {
        Turno turno = cancelado("09:00");
        reserva(1L, turno, EstadoReserva.CANCELADA_POR_GIMNASIO);
        // Al releer el turno bloqueado ya no está cancelado
        org.mockito.Mockito.doAnswer(inv -> {
            turno.reactivar(CUPO_VIGENTE);
            return null;
        }).when(entityManager).refresh(turno);

        assertThrows(ConflictoException.class, () -> service.revertir(turno.getId(), AHORA));

        verify(turnoRepository, never()).save(any());
    }
}
