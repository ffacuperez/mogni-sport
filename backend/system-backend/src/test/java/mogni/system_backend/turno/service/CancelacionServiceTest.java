package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

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

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.entity.Modalidad;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.turno.dto.CancelacionResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * HU-255. Los repositorios se simulan con listas en memoria. Para la prueba "otro alumno puede reservar el cupo
 * liberado" se arma también el ReservaService real sobre las mismas listas.
 */
@ExtendWith(MockitoExtension.class)
class CancelacionServiceTest {

    private static final String EMAIL = "lucia@mail.com";
    private static final String EMAIL_OTRO = "pedro@mail.com";
    /** Martes 13/10/2026 a las 10:30. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);
    private static final LocalDateTime MARTES_10_30 = MARTES.atTime(10, 30);

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClaseExtraRepository claseExtraRepository;

    private CancelacionService cancelacionService;
    private ReservaService reservaService;

    private Alumno alumno;
    private Alumno otroAlumno;
    private final List<Turno> turnos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        cancelacionService = new CancelacionService(alumnoRepository, reservaRepository, claseExtraRepository);
        reservaService = new ReservaService(alumnoRepository, turnoRepository, reservaRepository, claseExtraRepository);
        alumno = alumno(10L, "a10@mail.com");
        otroAlumno = alumno(20L, "a20@mail.com");

        lenient().when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenAnswer(inv -> Optional.of(alumno));
        lenient().when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL_OTRO)).thenAnswer(inv -> Optional.of(otroAlumno));
        lenient().when(alumnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv ->
                Optional.of(inv.<Long>getArgument(0).equals(alumno.getId()) ? alumno : otroAlumno));
        lenient().when(turnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv ->
                turnos.stream().filter(t -> t.getId().equals(inv.<Long>getArgument(0))).findFirst());
        lenient().when(reservaRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv ->
                reservas.stream().filter(r -> r.getId().equals(inv.<Long>getArgument(0))).findFirst());
        lenient().when(reservaRepository.countByTurnoIdAndEstado(anyLong(), any(EstadoReserva.class)))
                .thenAnswer(inv -> reservas.stream()
                        .filter(r -> r.getTurno().getId().equals(inv.<Long>getArgument(0))
                                && r.getEstado() == inv.<EstadoReserva>getArgument(1))
                        .count());
        lenient().when(reservaRepository.findByAlumnoIdAndEstadoAndTurnoFechaBetween(
                        anyLong(), any(EstadoReserva.class), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    Long alumnoId = inv.getArgument(0);
                    EstadoReserva estado = inv.getArgument(1);
                    LocalDate desde = inv.getArgument(2);
                    LocalDate hasta = inv.getArgument(3);
                    return reservas.stream()
                            .filter(r -> r.getAlumno().getId().equals(alumnoId) && r.getEstado() == estado)
                            .filter(r -> !r.getTurno().getFecha().isBefore(desde) && !r.getTurno().getFecha().isAfter(hasta))
                            .toList();
                });
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva reserva = inv.getArgument(0);
            if (reserva.getId() == null) {
                reserva.setId(proximoId++);
                reservas.add(reserva);
            }
            return reserva;
        });
    }

    // --- Armado del escenario ---

    private static Alumno alumno(Long id, String email) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setModalidad(Modalidad.PRESENCIAL);
        a.setCuenta(new Cuenta(id, email, "hash", Rol.ALUMNO, false, true, null));
        Membresia membresia = new Membresia(1L, "Plan", 30000.0, 3, null);
        a.agregarInscripcion(new Inscripcion(1L, MARTES.minusMonths(1), null, EstadoInscripcion.ACTIVA, a, membresia));
        return a;
    }

    private Turno turno(LocalDate fecha, String inicio, int cupo) {
        LocalTime horaInicio = LocalTime.parse(inicio);
        Turno turno = new Turno(proximoId++, fecha, horaInicio, horaInicio.plusHours(1), cupo, EstadoTurno.DISPONIBLE);
        turnos.add(turno);
        return turno;
    }

    private Reserva reservar(Alumno quien, Turno turno, EstadoReserva estado) {
        Reserva reserva = new Reserva(proximoId++, quien, turno, MARTES.atStartOfDay(), estado);
        reservas.add(reserva);
        return reserva;
    }

    private Reserva reservar(Turno turno) {
        return reservar(alumno, turno, EstadoReserva.RESERVADA);
    }

    private CancelacionResponse cancelar(Reserva reserva) {
        return cancelacionService.cancelar(EMAIL, reserva.getId(), MARTES_10_30);
    }

    // --- Cancelar con anticipación (pasa) ---

    @Test
    void cancelarConMasDeDosHorasDeAnticipacionPasaLaReservaACancelada() {
        Reserva reserva = reservar(turno(MARTES, "19:00", 5));

        CancelacionResponse response = cancelar(reserva);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals(EstadoReserva.CANCELADA, response.getEstado());
        assertEquals(reserva.getId(), response.getReservaId());
        assertEquals("19:00", response.getHoraInicio());
        assertEquals("20:00", response.getHoraFin());
    }

    @Test
    void laReservaNoSeBorraQuedaCancelada() {
        Reserva reserva = reservar(turno(MARTES.plusDays(1), "08:00", 5));

        cancelar(reserva);

        assertEquals(1, reservas.size());
        assertEquals(EstadoReserva.CANCELADA, reservas.get(0).getEstado());
    }

    @Test
    void cancelarJustoDosHorasAntesTodaviaSePuede() {
        // Turno a las 12:30 y son las 10:30: faltan exactamente 2 horas
        Reserva reserva = reservar(turno(MARTES, "12:30", 5));

        cancelar(reserva);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void cancelarUnTurnoDeManianaTempranoSePuede() {
        Reserva reserva = reservar(turno(MARTES.plusDays(1), "08:00", 5));

        cancelar(reserva);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    // --- Cancelar con menos de 2 horas (falla) ---

    @Test
    void cancelarConMenosDeDosHorasFalla() {
        Reserva reserva = reservar(turno(MARTES, "12:29", 5));

        ConflictoException ex = assertThrows(ConflictoException.class, () -> cancelar(reserva));
        assertEquals("Solo podés cancelar hasta 2 horas antes del turno.", ex.getMessage());
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
    }

    @Test
    void cancelarUnMinutoAntesDeEmpezarFalla() {
        Reserva reserva = reservar(turno(MARTES, "10:31", 5));

        assertThrows(ConflictoException.class, () -> cancelar(reserva));
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
    }

    // --- Cancelar un turno que ya pasó (falla) ---

    @Test
    void cancelarUnTurnoQueYaEmpezoFalla() {
        Reserva reserva = reservar(turno(MARTES, "10:00", 5));

        ConflictoException ex = assertThrows(ConflictoException.class, () -> cancelar(reserva));
        assertEquals("Este turno ya empezó o ya pasó, no se puede cancelar.", ex.getMessage());
        assertEquals(EstadoReserva.RESERVADA, reserva.getEstado());
    }

    @Test
    void cancelarUnTurnoQueEmpiezaJustoAhoraFalla() {
        Reserva reserva = reservar(turno(MARTES, "10:30", 5));

        assertThrows(ConflictoException.class, () -> cancelar(reserva));
    }

    @Test
    void cancelarUnTurnoDeAyerFalla() {
        Reserva reserva = reservar(turno(MARTES.minusDays(1), "19:00", 5));

        ConflictoException ex = assertThrows(ConflictoException.class, () -> cancelar(reserva));
        assertEquals("Este turno ya empezó o ya pasó, no se puede cancelar.", ex.getMessage());
    }

    // --- Solo las propias ---

    @Test
    void unAlumnoNoPuedeCancelarLaReservaDeOtro() {
        Reserva deOtro = reservar(otroAlumno, turno(MARTES, "19:00", 5), EstadoReserva.RESERVADA);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> cancelar(deOtro));
        assertEquals("Solo podés cancelar tus propios turnos.", ex.getMessage());
        assertEquals(EstadoReserva.RESERVADA, deOtro.getEstado());
    }

    @Test
    void reservaInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> cancelacionService.cancelar(EMAIL, 999L, MARTES_10_30));
    }

    @Test
    void emailSinAlumnoDa404() {
        Reserva reserva = reservar(turno(MARTES, "19:00", 5));

        assertThrows(RecursoNoEncontradoException.class,
                () -> cancelacionService.cancelar("nadie@mail.com", reserva.getId(), MARTES_10_30));
    }

    // --- Estados ---

    @Test
    void cancelarUnaReservaYaCanceladaDa409() {
        Reserva reserva = reservar(alumno, turno(MARTES, "19:00", 5), EstadoReserva.CANCELADA);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> cancelar(reserva));
        assertEquals("Esta reserva ya fue cancelada.", ex.getMessage());
    }

    @Test
    void cancelarDosVecesSeguidasFallaLaSegunda() {
        Reserva reserva = reservar(turno(MARTES, "19:00", 5));

        cancelar(reserva);

        assertThrows(ConflictoException.class, () -> cancelar(reserva));
    }

    @Test
    void cancelarUnaReservaCanceladaPorElGimnasioDa409() {
        Reserva reserva = reservar(alumno, turno(MARTES, "19:00", 5), EstadoReserva.CANCELADA_POR_GIMNASIO);

        assertThrows(ConflictoException.class, () -> cancelar(reserva));
    }

    // --- Efectos de cancelar ---

    @Test
    void trasCancelarOtroAlumnoPuedeReservarElCupoLiberado() {
        Turno turno = turno(MARTES, "19:00", 1);
        Reserva reserva = reservar(turno);

        // El turno está completo: el otro alumno no puede reservar
        assertThrows(ConflictoException.class,
                () -> reservaService.reservar(EMAIL_OTRO, turno.getId(), MARTES_10_30));

        cancelar(reserva);

        reservaService.reservar(EMAIL_OTRO, turno.getId(), MARTES_10_30);
        long activas = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.RESERVADA).count();
        assertEquals(1, activas);
        assertEquals(otroAlumno, reservas.stream().filter(r -> r.getEstado() == EstadoReserva.RESERVADA)
                .findFirst().orElseThrow().getAlumno());
    }

    @Test
    void laReservaCanceladaNoCuentaParaElLimiteSemanal() {
        // Membresía de 3 por semana: 3 reservas activas la agotan; cancelar una libera un turno
        Reserva primera = reservar(turno(MARTES.plusDays(1), "08:00", 5));
        reservar(turno(MARTES.plusDays(2), "08:00", 5));
        reservar(turno(MARTES.plusDays(3), "08:00", 5));
        Turno nuevo = turno(MARTES, "19:00", 5);
        assertThrows(ConflictoException.class, () -> reservaService.reservar(EMAIL, nuevo.getId(), MARTES_10_30));

        cancelar(primera);

        reservaService.reservar(EMAIL, nuevo.getId(), MARTES_10_30);
        assertEquals(3, reservas.stream()
                .filter(r -> r.getAlumno().equals(alumno) && r.getEstado() == EstadoReserva.RESERVADA).count());
    }

    // --- puedeCancelar (lo usa Mis turnos para mostrar o deshabilitar el botón) ---

    @Test
    void sePuedeCancelarSoloHastaDosHorasAntes() {
        assertTrue(CancelacionService.sePuedeCancelar(turno(MARTES, "19:00", 5), MARTES_10_30));
        assertTrue(CancelacionService.sePuedeCancelar(turno(MARTES, "12:30", 5), MARTES_10_30));
        assertFalse(CancelacionService.sePuedeCancelar(turno(MARTES, "12:29", 5), MARTES_10_30));
        assertFalse(CancelacionService.sePuedeCancelar(turno(MARTES, "10:00", 5), MARTES_10_30));
    }
}
