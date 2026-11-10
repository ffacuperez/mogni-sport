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

import mogni.system_backend.cuota.CuotasDePrueba;
import mogni.system_backend.alumno.entity.Alumno;
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
        alumno = alumno(10L, true, 3);

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

    private static Alumno alumno(Long id, boolean activa, Integer frecuenciaSemanal) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, activa, null));
        if (frecuenciaSemanal != null) {
            Membresia membresia = new Membresia(1L, "Plan", 30000.0, frecuenciaSemanal, null);
            a.agregarInscripcion(new Inscripcion(1L, MARTES.minusMonths(1), null, EstadoInscripcion.ACTIVA, a, membresia));
        }
        return a;
    }

    private static Alumno otroAlumno(Long id) {
        return alumno(id, true, 5);
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
        alumno = alumno(10L, false, 3);
        Turno turno = turno(MARTES, "19:00", 5);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> reservar(turno));
        assertEquals("Tu cuenta está inactiva.", ex.getMessage());
        assertTrue(reservas.isEmpty());
    }

    @Test
    void alumnoSinMembresiaActivaNoPuedeReservar() {
        alumno = alumno(10L, true, null);
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
        // MOGNI-254: antes era el de las 10:31; ahora tiene que faltar al menos 1 hora
        Turno turno = turno(MARTES, "12:00", 5);

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
        alumno = alumno(10L, true, 2);
        // Semana del lunes 12/10 al domingo 18/10: ya tiene 2 reservas (lunes y viernes)
        yaReservado(alumno, turno(MARTES.minusDays(1), "19:00", 5), EstadoReserva.RESERVADA);
        yaReservado(alumno, turno(MARTES.plusDays(3), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Ya usaste todos los turnos de tu membresía en esta semana.", ex.getMessage());
    }

    @Test
    void laUltimaReservaDeLaSemanaDejaCeroTurnosRestantes() {
        alumno = alumno(10L, true, 2);
        yaReservado(alumno, turno(MARTES.minusDays(1), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        assertEquals(0, reservar(turno).getTurnosRestantesSemana());
    }

    @Test
    void conSeisDiasLaSextaReservaDeLaSemanaEntraYDejaCeroRestantes() {
        alumno = alumno(10L, true, 6);
        // Semana del lunes 12/10 al domingo 18/10: ya tiene 5 reservas (todos los días menos martes y domingo)
        for (int offset : new int[] {-1, 1, 2, 3, 4}) {
            yaReservado(alumno, turno(MARTES.plusDays(offset), "19:00", 5), EstadoReserva.RESERVADA);
        }
        Turno turno = turno(MARTES, "19:00", 5);

        assertEquals(0, reservar(turno).getTurnosRestantesSemana());
        assertEquals(6, reservas.size());
    }

    @Test
    void conSeisDiasYLaSemanaCompletaLaSeptimaDa409() {
        alumno = alumno(10L, true, 6);
        // Ya tiene 6 reservas: todos los días de la semana menos el martes
        for (int offset : new int[] {-1, 1, 2, 3, 4, 5}) {
            yaReservado(alumno, turno(MARTES.plusDays(offset), "19:00", 5), EstadoReserva.RESERVADA);
        }
        Turno turno = turno(MARTES, "19:00", 5);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Ya usaste todos los turnos de tu membresía en esta semana.", ex.getMessage());
    }

    @Test
    void conDosDiasLaPrimeraReservaDejaUnTurnoRestante() {
        alumno = alumno(10L, true, 2);

        assertEquals(1, reservar(turno(MARTES, "19:00", 5)).getTurnosRestantesSemana());
    }

    @Test
    void lasReservasDeOtraSemanaNoCuentanParaElTope() {
        alumno = alumno(10L, true, 1);
        // Domingo 11/10: es de la semana anterior (la semana va de lunes a domingo)
        yaReservado(alumno, turno(MARTES.minusDays(2), "19:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "19:00", 5);

        reservar(turno);

        assertEquals(2, reservas.size());
    }

    @Test
    void lasReservasCanceladasNoCuentanParaElTopeSemanal() {
        alumno = alumno(10L, true, 1);
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
        CuotasDePrueba.queVence(alumno.buscarInscripcionActiva().orElseThrow(), MARTES.plusDays(dias));
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

    // --- Anticipación mínima de 1 hora (MOGNI-254) ---

    private static final String MENSAJE_ANTICIPACION =
            "No podés reservar este turno porque falta menos de 1 hora para su inicio.";
    /** Martes a las 23:45, para probar contra los turnos de la madrugada del miércoles. */
    private static final LocalDateTime MARTES_23_45 = MARTES.atTime(23, 45);

    private ReservaResponse reservarA(Turno turno, LocalDateTime ahora) {
        return reservaService.reservar(EMAIL, turno.getId(), ahora);
    }

    private void rechazaPorAnticipacion(Turno turno, LocalDateTime ahora) {
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class, () -> reservarA(turno, ahora));
        assertEquals(MENSAJE_ANTICIPACION, ex.getMessage());
        assertTrue(reservas.isEmpty());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void conMasDeUnaHoraDeAnticipacionSePuedeReservar() {
        // 10:30 → 11:31: faltan 61 minutos
        Turno turno = turno(MARTES, "11:31", 5);

        assertEquals(EstadoReserva.RESERVADA, reservarA(turno, MARTES_10_30).getEstado());
        assertEquals(1, reservas.size());
    }

    @Test
    void conExactamenteUnaHoraDeAnticipacionSePuedeReservar() {
        Turno turno = turno(MARTES, "11:30", 5);

        assertEquals(EstadoReserva.RESERVADA, reservarA(turno, MARTES_10_30).getEstado());
    }

    @Test
    void conUnSegundoMenosDeUnaHoraNoSePuedeReservar() {
        // 10:30:01 → 11:30: faltan 59 minutos 59 segundos
        Turno turno = turno(MARTES, "11:30", 5);

        rechazaPorAnticipacion(turno, MARTES_10_30.plusSeconds(1));
    }

    @Test
    void conTreintaMinutosDeAnticipacionNoSePuedeReservar() {
        Turno turno = turno(MARTES, "11:00", 5);

        rechazaPorAnticipacion(turno, MARTES_10_30);
    }

    @Test
    void unTurnoQueYaEmpezoSigueDandoSuMensajePropio() {
        // El chequeo de "ya empezó" va antes que el de anticipación, así el alumno ve el motivo más claro
        Turno turno = turno(MARTES, "10:00", 5);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> reservarA(turno, MARTES_10_30));
        assertEquals("Este turno ya empezó, elegí uno más tarde.", ex.getMessage());
        assertTrue(reservas.isEmpty());
    }

    @Test
    void aLas23_45ElTurnoDeManianaALas00_30NoSePuedeReservar() {
        // Se compara fecha y hora completas: solo por la hora (00:30 < 23:45) no alcanzaría
        Turno turno = turno(MIERCOLES, "00:30", 5);

        rechazaPorAnticipacion(turno, MARTES_23_45);
    }

    @Test
    void aLas23_45ElTurnoDeManianaALas09_00SePuedeReservar() {
        Turno turno = turno(MIERCOLES, "09:00", 5);

        assertEquals(EstadoReserva.RESERVADA, reservarA(turno, MARTES_23_45).getEstado());
    }

    @Test
    void elRechazoPorAnticipacionNoUsaLaClaseExtraNiOcupaLugar() {
        // Límite semanal agotado y una clase extra vigente: igual se rechaza antes de tocarla
        alumno = alumno(10L, true, 1);
        yaReservado(alumno, turno(MARTES.minusDays(1), "08:00", 5), EstadoReserva.RESERVADA);
        Turno turno = turno(MARTES, "11:00", 5);
        ClaseExtra claseExtra = new ClaseExtra(alumno, 90L, MARTES.minusDays(1), MARTES.plusDays(12));
        lenient().when(claseExtraRepository.buscarDisponiblesConBloqueo(alumno.getId(), MARTES))
                .thenReturn(List.of(claseExtra));

        assertThrows(RegistroInvalidoException.class, () -> reservarA(turno, MARTES_10_30));

        assertEquals(1, reservas.size());
        assertEquals(0, reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA));
        assertEquals(null, claseExtra.getReservaUsoId());
        verify(claseExtraRepository, never()).buscarDisponiblesConBloqueo(anyLong(), any());
        verify(claseExtraRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    // --- Regresión de las reglas existentes que no tenían test propio ---

    /** Simula la consulta real: solo clases extra sin usar y vigentes en la fecha del turno. */
    private void conClasesExtra(ClaseExtra... clases) {
        lenient().when(claseExtraRepository.buscarDisponiblesConBloqueo(anyLong(), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate fecha = inv.getArgument(1);
                    return List.of(clases).stream()
                            .filter(c -> c.getReservaUsoId() == null)
                            .filter(c -> !c.getVigenteDesde().isAfter(fecha) && !c.getVigenteHasta().isBefore(fecha))
                            .toList();
                });
    }

    @Test
    void conLaClaseExtraVencidaYElLimiteAgotadoNoSePuedeReservar() {
        alumno = alumno(10L, true, 1);
        yaReservado(alumno, turno(MARTES.minusDays(1), "08:00", 5), EstadoReserva.RESERVADA);
        // Venció el domingo: no sirve para el martes
        ClaseExtra vencida = new ClaseExtra(alumno, 90L, MARTES.minusDays(10), MARTES.minusDays(2));
        conClasesExtra(vencida);
        Turno turno = turno(MARTES, "19:00", 5);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservar(turno));
        assertEquals("Ya usaste todos los turnos de tu membresía en esta semana.", ex.getMessage());
        assertEquals(null, vencida.getReservaUsoId());
        verify(claseExtraRepository, never()).save(any());
    }

    @Test
    void conLimiteDisponibleNoSeConsumeLaClaseExtra() {
        ClaseExtra claseExtra = new ClaseExtra(alumno, 90L, MARTES.minusDays(1), MARTES.plusDays(12));
        conClasesExtra(claseExtra);
        Turno turno = turno(MARTES, "19:00", 5);

        reservar(turno);

        assertEquals(null, claseExtra.getReservaUsoId());
        verify(claseExtraRepository, never()).buscarDisponiblesConBloqueo(anyLong(), any());
        verify(claseExtraRepository, never()).save(any());
    }

    @Test
    void despuesDeReservarQuedaReservadaDelAlumnoYDelTurnoYHayUnLugarMenos() {
        Turno turno = turno(MARTES, "19:00", 5);
        yaReservado(otroAlumno(20L), turno, EstadoReserva.RESERVADA);
        long ocupadosAntes = reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA);

        ReservaResponse response = reservar(turno);

        Reserva guardada = reservas.stream().filter(r -> r.getId().equals(response.getId())).findFirst().orElseThrow();
        assertEquals(EstadoReserva.RESERVADA, guardada.getEstado());
        assertEquals(alumno, guardada.getAlumno());
        assertEquals(turno, guardada.getTurno());
        long ocupadosDespues = reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA);
        assertEquals(ocupadosAntes + 1, ocupadosDespues);
        assertEquals(3, turno.getCupoMaximo() - ocupadosDespues);
    }
}
