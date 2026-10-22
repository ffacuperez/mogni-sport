package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
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
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.turno.dto.ReservaResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.repository.ClaseExtraRepository;

/**
 * HU-254. Los repositorios se simulan con listas en memoria, igual que en ConsultaTurnosServiceTest. La seguridad
 * ante concurrencia depende del bloqueo de filas de la base (FOR UPDATE): acá solo se verifica que se pida el
 * bloqueo antes de validar y guardar.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final String EMAIL = "lucia@mail.com";
    /** Martes 13/10/2026 a las 10:30. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);
    private static final LocalDate MIERCOLES = MARTES.plusDays(1);
    private static final LocalDateTime MARTES_10_30 = MARTES.atTime(10, 30);

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClaseExtraRepository claseExtraRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Alumno alumno;
    private final List<Turno> turnos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 3);

        lenient().when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenAnswer(inv -> Optional.of(alumno));
        lenient().when(alumnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv -> Optional.of(alumno));
        lenient().when(turnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv ->
                turnos.stream().filter(t -> t.getId().equals(inv.<Long>getArgument(0))).findFirst());
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
            reserva.setId(proximoId++);
            reservas.add(reserva);
            return reserva;
        });
    }

    // --- Armado del escenario ---

    private static Alumno alumno(Long id, Modalidad modalidad, boolean activa, Integer frecuenciaSemanal) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setModalidad(modalidad);
        a.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, activa, null));
        if (frecuenciaSemanal != null) {
            Membresia membresia = new Membresia(1L, "Plan", 30000.0, frecuenciaSemanal, null);
            a.agregarInscripcion(new Inscripcion(1L, MARTES.minusMonths(1), null, EstadoInscripcion.ACTIVA, a, membresia));
        }
        return a;
    }

    private static Alumno otroAlumno(Long id) {
        return alumno(id, Modalidad.PRESENCIAL, true, 5);
    }

    private Turno turno(LocalDate fecha, String inicio, int cupo) {
        return turno(fecha, inicio, cupo, EstadoTurno.DISPONIBLE);
    }

    private Turno turno(LocalDate fecha, String inicio, int cupo, EstadoTurno estado) {
        LocalTime horaInicio = LocalTime.parse(inicio);
        Turno turno = new Turno(proximoId++, fecha, horaInicio, horaInicio.plusHours(1), cupo, estado);
        turnos.add(turno);
        return turno;
    }

    private void yaReservado(Alumno quien, Turno turno, EstadoReserva estado) {
        reservas.add(new Reserva(proximoId++, quien, turno, MARTES.atStartOfDay(), estado));
    }

    private ReservaResponse reservar(Turno turno) {
        return reservaService.reservar(EMAIL, turno.getId(), MARTES_10_30);
    }

    // --- Reserva exitosa ---

    @Test
    void reservaUnTurnoDeHoyYDevuelveLosTurnosRestantes() {
        Turno turno = turno(MARTES, "19:00", 5);

        ReservaResponse response = reservar(turno);

        assertEquals(turno.getId(), response.getTurnoId());
        assertEquals(MARTES, response.getFecha());
        assertEquals("19:00", response.getHoraInicio());
        assertEquals("20:00", response.getHoraFin());
        assertEquals(EstadoReserva.RESERVADA, response.getEstado());
        assertEquals(2, response.getTurnosRestantesSemana());
        assertEquals(1, reservas.size());
        assertEquals(MARTES_10_30, reservas.get(0).getFechaHoraReserva());
        assertEquals(alumno, reservas.get(0).getAlumno());
    }

    @Test
    void reservaUnTurnoDeManiana() {
        Turno turno = turno(MIERCOLES, "08:00", 5);

        assertEquals(MIERCOLES, reservar(turno).getFecha());
    }

    @Test
    void sePuedeReservarElUltimoLugarDelTurno() {
        Turno turno = turno(MARTES, "19:00", 2);
        yaReservado(otroAlumno(20L), turno, EstadoReserva.RESERVADA);

        reservar(turno);

        assertEquals(2, reservas.size());
    }

    @Test
    void usaUnaClaseExtraCuandoYaAlcanzoElLimiteSemanalDeLaMembresia() {
        yaReservado(alumno, turno(MARTES.minusDays(1), "08:00", 5), EstadoReserva.RESERVADA);
        yaReservado(alumno, turno(MIERCOLES, "08:00", 5), EstadoReserva.RESERVADA);
        yaReservado(alumno, turno(MIERCOLES.plusDays(1), "08:00", 5), EstadoReserva.RESERVADA);
        Turno turnoExtra = turno(MARTES, "19:00", 5);
        ClaseExtra claseExtra = new ClaseExtra(alumno, 90L, MARTES.minusDays(1), MARTES.plusDays(12));
        when(claseExtraRepository.buscarDisponiblesConBloqueo(alumno.getId(), MARTES))
                .thenReturn(List.of(claseExtra));

        ReservaResponse response = reservar(turnoExtra);

        assertEquals(0, response.getTurnosRestantesSemana());
        assertEquals(response.getId(), claseExtra.getReservaUsoId());
        verify(claseExtraRepository).save(claseExtra);
    }

    // --- Acceso ---

    @Test
    void alumnoInactivoNoPuedeReservar() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, false, 3);
        Turno turno = turno(MARTES, "19:00", 5);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> reservar(turno));
        assertEquals("Tu cuenta está inactiva.", ex.getMessage());
        assertTrue(reservas.isEmpty());
    }

    @Test
    void alumnoOnlineNoPuedeReservar() {
        alumno = alumno(10L, Modalidad.ONLINE, true, 3);
        Turno turno = turno(MARTES, "19:00", 5);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> reservar(turno));
        assertEquals("La reserva de turnos es solo para alumnos presenciales.", ex.getMessage());
    }

    @Test
    void alumnoSinMembresiaActivaNoPuedeReservar() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, null);
        Turno turno = turno(MARTES, "19:00", 5);

        assertThrows(AccesoDenegadoException.class, () -> reservar(turno));
        assertTrue(reservas.isEmpty());
    }

    @Test
    void emailSinAlumnoDa404() {
        Turno turno = turno(MARTES, "19:00", 5);

        assertThrows(RecursoNoEncontradoException.class,
                () -> reservaService.reservar("otro@mail.com", turno.getId(), MARTES_10_30));
    }

    // --- Turno ---

    @Test
    void turnoInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.reservar(EMAIL, 999L, MARTES_10_30));
    }

    @Test
    void turnoDePasadoManianaDa400() {
        Turno turno = turno(MARTES.plusDays(2), "08:00", 5);

        assertThrows(RegistroInvalidoException.class, () -> reservar(turno));
        assertTrue(reservas.isEmpty());
    }

    @Test
    void turnoDeAyerDa400() {
        Turno turno = turno(MARTES.minusDays(1), "19:00", 5);

        assertThrows(RegistroInvalidoException.class, () -> reservar(turno));
    }

    @Test
    void turnoDeHoyQueYaEmpezoDa400() {
        Turno turno = turno(MARTES, "10:00", 5);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class, () -> reservar(turno));
        assertEquals("Este turno ya empezó, elegí uno más tarde.", ex.getMessage());
    }

    @Test
    void turnoDeHoyQueEmpiezaJustoAhoraDa400() {
        Turno turno = turno(MARTES, "10:30", 5);

        assertThrows(RegistroInvalidoException.class, () -> reservar(turno));
    }

    @Test
    void turnoDeHoyQueEmpiezaMasTardeSePuedeReservar() {
        Turno turno = turno(MARTES, "10:31", 5);

        reservar(turno);

        assertEquals(1, reservas.size());
    }

    @Test
    void turnoDeManianaTempranoSePuedeReservarAunqueLaHoraYaPasoHoy() {
        // A las 10:30 de hoy, el turno de mañana a las 08:00 todavía no empezó
        Turno turno = turno(MIERCOLES, "08:00", 5);

        reservar(turno);

        assertEquals(1, reservas.size());
    }

    @Test
    void turnoCanceladoPorElGimnasioDa409() {
        Turno turno = turno(MARTES, "19:00", 5, EstadoTurno.CANCELADO);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Este turno fue cancelado por el gimnasio.", ex.getMessage());
        assertTrue(reservas.isEmpty());
    }

    // --- Cupo ---

    @Test
    void turnoCompletoDa409() {
        Turno turno = turno(MARTES, "19:00", 2);
        yaReservado(otroAlumno(20L), turno, EstadoReserva.RESERVADA);
        yaReservado(otroAlumno(30L), turno, EstadoReserva.RESERVADA);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Este turno está completo.", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void lasReservasCanceladasNoOcupanLugar() {
        Turno turno = turno(MARTES, "19:00", 1);
        yaReservado(otroAlumno(20L), turno, EstadoReserva.CANCELADA);
        yaReservado(otroAlumno(30L), turno, EstadoReserva.CANCELADA_POR_GIMNASIO);

        reservar(turno);

        assertEquals(3, reservas.size());
    }

    // --- Reglas del alumno ---

    @Test
    void noSePuedeReservarElMismoTurnoDosVeces() {
        Turno turno = turno(MARTES, "19:00", 5);
        yaReservado(alumno, turno, EstadoReserva.RESERVADA);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Ya reservaste este turno.", ex.getMessage());
    }

    @Test
    void noSePuedeTenerDosTurnosElMismoDia() {
        Turno yaTomado = turno(MARTES, "12:00", 5);
        Turno otro = turno(MARTES, "19:00", 5);
        yaReservado(alumno, yaTomado, EstadoReserva.RESERVADA);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(otro));
        assertEquals("Ya tenés un turno reservado este día.", ex.getMessage());
    }

    @Test
    void unTurnoCanceladoDelMismoDiaNoImpideReservarOtro() {
        Turno cancelado = turno(MARTES, "12:00", 5);
        Turno otro = turno(MARTES, "19:00", 5);
        yaReservado(alumno, cancelado, EstadoReserva.CANCELADA);

        reservar(otro);

        assertEquals(2, reservas.size());
    }

    @Test
    void sePuedeVolverAReservarUnTurnoQueElAlumnoCancelo() {
        Turno turno = turno(MARTES, "19:00", 5);
        yaReservado(alumno, turno, EstadoReserva.CANCELADA);

        reservar(turno);

        assertEquals(2, reservas.size());
    }

    @Test
    void sinTurnosRestantesEnLaSemanaDa409() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 2);
        // Semana del lunes 12/10 al domingo 18/10: ya tiene 2 reservas (lunes y viernes)
        yaReservado(alumno, turno(MARTES.minusDays(1), "19:00", 5), EstadoReserva.RESERVADA);
        yaReservado(alumno, turno(MARTES.plusDays(3), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Ya usaste todos los turnos de tu membresía en esta semana.", ex.getMessage());
    }

    @Test
    void laUltimaReservaDeLaSemanaDejaCeroTurnosRestantes() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 2);
        yaReservado(alumno, turno(MARTES.minusDays(1), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        assertEquals(0, reservar(turno).getTurnosRestantesSemana());
    }

    @Test
    void lasReservasDeOtraSemanaNoCuentanParaElTope() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 1);
        // Domingo 11/10: es de la semana anterior (la semana va de lunes a domingo)
        yaReservado(alumno, turno(MARTES.minusDays(2), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        reservar(turno);

        assertEquals(2, reservas.size());
    }

    @Test
    void lasReservasCanceladasNoCuentanParaElTopeSemanal() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 1);
        yaReservado(alumno, turno(MARTES.minusDays(1), "19:00", 5), EstadoReserva.CANCELADA);
        Turno turno = turno(MARTES, "19:00", 5);

        reservar(turno);

        assertEquals(2, reservas.size());
    }

    // --- Concurrencia ---

    @Test
    void bloqueaAlAlumnoYAlTurnoAntesDeGuardar() {
        Turno turno = turno(MARTES, "19:00", 5);

        reservar(turno);

        InOrder orden = inOrder(alumnoRepository, turnoRepository, reservaRepository);
        orden.verify(alumnoRepository).findByIdConBloqueo(alumno.getId());
        orden.verify(turnoRepository).findByIdConBloqueo(turno.getId());
        orden.verify(reservaRepository).save(any(Reserva.class));
    }

    // --- Cuota en mora (MOGNI-254) ---

    /** El alumno queda con una cuota que vence {@code dias} días después del martes (negativo = ya venció). */
    private void cuotaQueVenceEn(int dias) {
        alumno.buscarInscripcionActiva().orElseThrow().agregarCuota(MARTES.plusDays(dias).minusDays(30));
    }

    @Test
    void alumnoEnMoraNoPuedeReservar() {
        cuotaQueVenceEn(-2);
        Turno turno = turno(MARTES, "19:00", 5);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> reservar(turno));

        assertEquals("Tenés la cuota en mora: regularizá el pago para reservar turnos.", ex.getMessage());
        assertTrue(reservas.isEmpty());
    }

    @Test
    void alumnoVencidoDentroDeLas24HorasTodaviaPuedeReservar() {
        cuotaQueVenceEn(-1);
        Turno turno = turno(MARTES, "19:00", 5);

        assertEquals(EstadoReserva.RESERVADA, reservar(turno).getEstado());
    }

    @Test
    void alRenovarPuedeVolverAReservarEnElMomento() {
        cuotaQueVenceEn(-5);
        // Renovó en mora: la cuota nueva empieza hoy
        alumno.buscarInscripcionActiva().orElseThrow().agregarCuota(MARTES);
        Turno turno = turno(MARTES, "19:00", 5);

        assertEquals(EstadoReserva.RESERVADA, reservar(turno).getEstado());
    }
}
