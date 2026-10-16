package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.entity.Modalidad;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.turno.dto.MiTurnoResponse;
import mogni.system_backend.turno.dto.MisTurnosResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-256. El repositorio se simula con una lista en memoria que filtra por alumno, estado y fecha como la consulta
 * real, pero SIN ordenar: el orden lo tiene que dar el servicio.
 */
@ExtendWith(MockitoExtension.class)
class MisTurnosServiceTest {

    private static final String EMAIL = "lucia@mail.com";
    /** Martes 13/10/2026 a las 10:30. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);
    private static final LocalDateTime MARTES_10_30 = MARTES.atTime(10, 30);

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private MisTurnosService misTurnosService;

    private Alumno alumno;
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true);

        lenient().when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenAnswer(inv -> Optional.of(alumno));
        lenient().when(reservaRepository.findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(
                        anyLong(), any(EstadoReserva.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    Long alumnoId = inv.getArgument(0);
                    EstadoReserva estado = inv.getArgument(1);
                    LocalDate desde = inv.getArgument(2);
                    return reservas.stream()
                            .filter(r -> r.getAlumno().getId().equals(alumnoId) && r.getEstado() == estado)
                            .filter(r -> !r.getTurno().getFecha().isBefore(desde))
                            .toList();
                });
    }

    // --- Armado del escenario ---

    private static Alumno alumno(Long id, Modalidad modalidad, boolean activa) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setModalidad(modalidad);
        a.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, activa));
        return a;
    }

    private Turno turno(LocalDate fecha, String inicio, String fin) {
        return new Turno(proximoId++, fecha, LocalTime.parse(inicio), LocalTime.parse(fin), 5, EstadoTurno.DISPONIBLE);
    }

    private Reserva reservar(Alumno quien, Turno turno, EstadoReserva estado) {
        Reserva reserva = new Reserva(proximoId++, quien, turno, MARTES.atStartOfDay(), estado);
        reservas.add(reserva);
        return reserva;
    }

    private Reserva reservar(Turno turno) {
        return reservar(alumno, turno, EstadoReserva.RESERVADA);
    }

    private MisTurnosResponse consultar() {
        return misTurnosService.consultarMisTurnos(EMAIL, MARTES_10_30);
    }

    private static List<String> inicios(MisTurnosResponse response) {
        return response.getTurnos().stream().map(t -> t.getFecha() + " " + t.getHoraInicio()).toList();
    }

    // --- Lista ---

    @Test
    void muestraLosTurnosReservadosConDiaFechaYHoras() {
        Reserva reserva = reservar(turno(MARTES.plusDays(1), "17:00", "18:00"));

        MisTurnosResponse response = consultar();

        assertEquals(MARTES, response.getHoy());
        assertEquals(1, response.getTurnos().size());
        MiTurnoResponse mio = response.getTurnos().get(0);
        assertEquals(reserva.getId(), mio.getReservaId());
        assertEquals(reserva.getTurno().getId(), mio.getTurnoId());
        assertEquals(MARTES.plusDays(1), mio.getFecha());
        assertEquals(DayOfWeek.WEDNESDAY, mio.getDiaSemana());
        assertEquals("17:00", mio.getHoraInicio());
        assertEquals("18:00", mio.getHoraFin());
    }

    @Test
    void ordenaDelMasCercanoAlMasLejano() {
        // Se cargan desordenados a propósito
        reservar(turno(MARTES.plusDays(1), "08:00", "09:00"));
        reservar(turno(MARTES, "19:00", "20:00"));
        reservar(turno(MARTES, "12:00", "13:00"));

        assertEquals(List.of("2026-10-13 12:00", "2026-10-13 19:00", "2026-10-14 08:00"), inicios(consultar()));
    }

    @Test
    void unTurnoDeHoyQueTodaviaNoTerminoSeMuestra() {
        reservar(turno(MARTES, "19:00", "20:00"));

        assertEquals(1, consultar().getTurnos().size());
    }

    @Test
    void unTurnoDeHoyEnCursoSeMuestraHastaQueTermina() {
        // Empezó a las 10:00 y termina a las 11:00: a las 10:30 todavía se muestra
        reservar(turno(MARTES, "10:00", "11:00"));

        assertEquals(1, consultar().getTurnos().size());
    }

    @Test
    void unTurnoDeHoyQueYaTerminoNoSeMuestra() {
        reservar(turno(MARTES, "08:00", "09:00"));

        assertTrue(consultar().getTurnos().isEmpty());
    }

    @Test
    void unTurnoDeHoyQueTerminaJustoAhoraNoSeMuestra() {
        reservar(turno(MARTES, "09:30", "10:30"));

        assertTrue(consultar().getTurnos().isEmpty());
    }

    @Test
    void unTurnoDeAyerNoSeMuestra() {
        reservar(turno(MARTES.minusDays(1), "19:00", "20:00"));

        assertTrue(consultar().getTurnos().isEmpty());
    }

    // --- Estados ---

    @Test
    void unTurnoQueElAlumnoCanceloNoAparece() {
        reservar(alumno, turno(MARTES.plusDays(1), "17:00", "18:00"), EstadoReserva.CANCELADA);

        assertTrue(consultar().getTurnos().isEmpty());
    }

    @Test
    void unTurnoCanceladoPorElGimnasioNoAparece() {
        reservar(alumno, turno(MARTES.plusDays(1), "17:00", "18:00"), EstadoReserva.CANCELADA_POR_GIMNASIO);

        assertTrue(consultar().getTurnos().isEmpty());
    }

    @Test
    void soloAparecenLasReservadasCuandoHayDeTodosLosEstados() {
        reservar(alumno, turno(MARTES.plusDays(1), "08:00", "09:00"), EstadoReserva.CANCELADA);
        reservar(alumno, turno(MARTES.plusDays(1), "09:00", "10:00"), EstadoReserva.RESERVADA);
        reservar(alumno, turno(MARTES.plusDays(1), "10:00", "11:00"), EstadoReserva.CANCELADA_POR_GIMNASIO);

        assertEquals(List.of("2026-10-14 09:00"), inicios(consultar()));
    }

    // --- Sin turnos y otros alumnos ---

    @Test
    void unAlumnoSinTurnosRecibeUnaListaVacia() {
        MisTurnosResponse response = consultar();

        assertEquals(MARTES, response.getHoy());
        assertTrue(response.getTurnos().isEmpty());
    }

    @Test
    void noVeLosTurnosReservadosPorOtroAlumno() {
        Alumno otro = alumno(20L, Modalidad.PRESENCIAL, true);
        reservar(otro, turno(MARTES.plusDays(1), "17:00", "18:00"), EstadoReserva.RESERVADA);
        Reserva mia = reservar(turno(MARTES.plusDays(1), "18:00", "19:00"));

        List<MiTurnoResponse> turnos = consultar().getTurnos();

        assertEquals(1, turnos.size());
        assertEquals(mia.getId(), turnos.get(0).getReservaId());
    }

    // --- Acceso ---

    @Test
    void alumnoOnlineNoPuedeConsultar() {
        alumno = alumno(10L, Modalidad.ONLINE, true);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, this::consultar);
        assertEquals("La reserva de turnos es solo para alumnos presenciales.", ex.getMessage());
    }

    @Test
    void alumnoInactivoNoPuedeConsultar() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, this::consultar);
        assertEquals("Tu cuenta está inactiva.", ex.getMessage());
    }

    @Test
    void emailSinAlumnoDa404() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> misTurnosService.consultarMisTurnos("otro@mail.com", MARTES_10_30));
    }
}
