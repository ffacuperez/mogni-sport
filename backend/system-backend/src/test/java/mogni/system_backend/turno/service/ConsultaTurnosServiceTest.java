package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.turno.dto.EstadoTurnoAlumno;
import mogni.system_backend.turno.dto.TurnoDisponibleResponse;
import mogni.system_backend.turno.dto.TurnosDisponiblesResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-259. Los repositorios se simulan con listas en memoria (turnos por fecha y reservas), así cada prueba
 * arma el escenario y las consultas filtran igual que la base.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaTurnosServiceTest {

    private static final String EMAIL = "lucia@mail.com";
    /** Martes 13/10/2026 a las 10:30. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);
    private static final LocalDate MIERCOLES = MARTES.plusDays(1);
    private static final LocalDateTime MARTES_10_30 = MARTES.atTime(10, 30);

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private GeneracionTurnosService generacionTurnosService;

    @InjectMocks
    private ConsultaTurnosService consultaTurnosService;

    private Alumno alumno;
    private Alumno otroAlumno;
    private final Map<LocalDate, List<Turno>> turnosPorFecha = new HashMap<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 3);
        otroAlumno = alumno(20L, Modalidad.PRESENCIAL, true, 5);

        lenient().when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenAnswer(inv -> Optional.of(alumno));
        lenient().when(generacionTurnosService.obtenerOGenerarTurnos(any(LocalDate.class)))
                .thenAnswer(inv -> turnosPorFecha.getOrDefault(inv.<LocalDate>getArgument(0), List.of()));
        lenient().when(reservaRepository.findByTurnoIdInAndEstado(anyCollection(), any(EstadoReserva.class)))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(0);
                    EstadoReserva estado = inv.getArgument(1);
                    return reservas.stream()
                            .filter(r -> ids.contains(r.getTurno().getId()) && r.getEstado() == estado)
                            .toList();
                });
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
    }

    // --- Armado del escenario ---

    private static Alumno alumno(Long id, Modalidad modalidad, boolean activa, Integer frecuenciaSemanal) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setModalidad(modalidad);
        a.setCuenta(new Cuenta(id, "a" + id + "@mail.com", "hash", Rol.ALUMNO, false, activa));
        if (frecuenciaSemanal != null) {
            Membresia membresia = new Membresia(1L, "Plan", 30000.0, frecuenciaSemanal, null);
            a.agregarInscripcion(new Inscripcion(1L, MARTES.minusMonths(1), null, EstadoInscripcion.ACTIVA, a, membresia));
        }
        return a;
    }

    private Turno turno(LocalDate fecha, String inicio, int cupo) {
        return turno(fecha, inicio, cupo, EstadoTurno.DISPONIBLE);
    }

    private Turno turno(LocalDate fecha, String inicio, int cupo, EstadoTurno estado) {
        LocalTime horaInicio = LocalTime.parse(inicio);
        Turno turno = new Turno(proximoId++, fecha, horaInicio, horaInicio.plusHours(1), cupo, estado);
        turnosPorFecha.computeIfAbsent(fecha, f -> new ArrayList<>()).add(turno);
        return turno;
    }

    private void reservar(Alumno quien, Turno turno, EstadoReserva estado) {
        reservas.add(new Reserva(proximoId++, quien, turno, MARTES.atStartOfDay(), estado));
    }

    private TurnosDisponiblesResponse consultar(LocalDate fecha, LocalDateTime ahora) {
        return consultaTurnosService.consultarTurnosDisponibles(EMAIL, fecha, ahora);
    }

    private static TurnoDisponibleResponse buscar(TurnosDisponiblesResponse response, String horaInicio) {
        return response.getTurnos().stream()
                .filter(t -> t.getHoraInicio().equals(horaInicio))
                .findFirst()
                .orElseThrow();
    }

    // --- Acceso ---

    // Pruebas 2 y 3
    @Test
    void alumnoInactivoNoPuedeConsultar() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, false, 3);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> consultar(null, MARTES_10_30));
        assertEquals("Tu cuenta está inactiva.", ex.getMessage());
    }

    @Test
    void alumnoOnlineNoPuedeConsultar() {
        alumno = alumno(10L, Modalidad.ONLINE, true, 3);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> consultar(null, MARTES_10_30));
        assertEquals("La reserva de turnos es solo para alumnos presenciales.", ex.getMessage());
    }

    @Test
    void fechaQueNoEsHoyNiManianaDa400() {
        assertThrows(RegistroInvalidoException.class, () -> consultar(MARTES.plusDays(2), MARTES_10_30));
        assertThrows(RegistroInvalidoException.class, () -> consultar(MARTES.minusDays(1), MARTES_10_30));
    }

    // --- Días y generación ---

    // Pruebas 1, 4 y 5
    @Test
    void sinFechaEsHoyYMuestraCincoDiasConHoyYManianaHabilitados() {
        TurnosDisponiblesResponse response = consultar(null, MARTES_10_30);

        assertEquals(MARTES, response.getHoy());
        assertEquals(MARTES, response.getFechaSeleccionada());
        assertEquals(5, response.getDias().size());
        assertEquals(MARTES, response.getDias().get(0).getFecha());
        assertEquals(MARTES.plusDays(4), response.getDias().get(4).getFecha());
        assertEquals(List.of(true, true, false, false, false),
                response.getDias().stream().map(d -> d.isHabilitado()).toList());
    }

    // Criterio 16: siempre se aseguran hoy y mañana, se consulte el día que se consulte
    @Test
    void siempreGeneraHoyYManiana() {
        consultar(MIERCOLES, MARTES_10_30);

        verify(generacionTurnosService).obtenerOGenerarTurnos(MARTES);
        verify(generacionTurnosService).obtenerOGenerarTurnos(MIERCOLES);
    }

    // Pruebas 15 y 22
    @Test
    void sinTurnosDevuelveListaVacia() {
        assertTrue(consultar(MIERCOLES, MARTES_10_30).getTurnos().isEmpty());
    }

    // --- Lista de turnos ---

    // Pruebas 6 y 9: hoy no se muestran los que ya empezaron (incluido el que empieza justo ahora)
    @Test
    void hoyOcultaLosTurnosQueYaEmpezaron() {
        turno(MARTES, "08:00", 5);
        turno(MARTES, "10:00", 5);
        turno(MARTES, "10:30", 5);
        turno(MARTES, "18:00", 5);
        turno(MARTES, "11:00", 5);

        TurnosDisponiblesResponse response = consultar(MARTES, MARTES_10_30);

        assertEquals(List.of("11:00", "18:00"), response.getTurnos().stream().map(TurnoDisponibleResponse::getHoraInicio).toList());
    }

    @Test
    void manianaMuestraTodosLosTurnosOrdenados() {
        turno(MIERCOLES, "08:00", 5);
        turno(MIERCOLES, "09:00", 5);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertEquals(List.of("08:00", "09:00"), response.getTurnos().stream().map(TurnoDisponibleResponse::getHoraInicio).toList());
        assertEquals("09:00", response.getTurnos().get(0).getHoraFin());
    }

    // Pruebas 7, 8 y 10: lugares libres = cupo del turno − reservas activas; las canceladas no ocupan lugar
    @Test
    void lugaresLibresNoCuentanLasReservasCanceladas() {
        Turno turno = turno(MIERCOLES, "08:00", 5);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);
        reservar(otroAlumno, turno, EstadoReserva.CANCELADA);
        reservar(otroAlumno, turno, EstadoReserva.CANCELADA_POR_GIMNASIO);

        TurnoDisponibleResponse respuesta = consultar(MIERCOLES, MARTES_10_30).getTurnos().get(0);

        assertEquals(3, respuesta.getLugaresLibres());
        assertEquals(EstadoTurnoAlumno.DISPONIBLE, respuesta.getEstado());
        assertTrue(respuesta.isSeleccionable());
    }

    @Test
    void lugaresLibresUsanElCupoDelTurnoYNuncaSonNegativos() {
        Turno turno = turno(MIERCOLES, "08:00", 1);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);

        assertEquals(0, consultar(MIERCOLES, MARTES_10_30).getTurnos().get(0).getLugaresLibres());
    }

    // Prueba 11
    @Test
    void turnoSinLugaresEsCompletoYNoSeleccionable() {
        Turno turno = turno(MIERCOLES, "08:00", 2);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);
        reservar(otroAlumno, turno, EstadoReserva.RESERVADA);

        TurnoDisponibleResponse respuesta = consultar(MIERCOLES, MARTES_10_30).getTurnos().get(0);

        assertEquals(EstadoTurnoAlumno.COMPLETO, respuesta.getEstado());
        assertFalse(respuesta.isSeleccionable());
    }

    // Pruebas 14 y 23: un turno cancelado no se puede elegir, los demás del día siguen disponibles
    @Test
    void turnoCanceladoNoSeleccionableYLosDemasSiguenDisponibles() {
        turno(MIERCOLES, "08:00", 5, EstadoTurno.CANCELADO);
        turno(MIERCOLES, "09:00", 5);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertEquals(EstadoTurnoAlumno.CANCELADO, buscar(response, "08:00").getEstado());
        assertFalse(buscar(response, "08:00").isSeleccionable());
        assertEquals(EstadoTurnoAlumno.DISPONIBLE, buscar(response, "09:00").getEstado());
        assertTrue(buscar(response, "09:00").isSeleccionable());
    }

    @Test
    void canceladoTienePrioridadSobreReservadoYCompleto() {
        Turno turno = turno(MIERCOLES, "08:00", 1, EstadoTurno.CANCELADO);
        reservar(alumno, turno, EstadoReserva.RESERVADA);

        assertEquals(EstadoTurnoAlumno.CANCELADO, consultar(MIERCOLES, MARTES_10_30).getTurnos().get(0).getEstado());
    }

    // Pruebas 12 y 13: el turno reservado se marca (aunque esté lleno) y bloquea los demás del día
    @Test
    void conUnTurnoReservadoEnElDiaLosDemasNoSeSeleccionan() {
        Turno reservado = turno(MIERCOLES, "19:00", 1);
        turno(MIERCOLES, "08:00", 5);
        reservar(alumno, reservado, EstadoReserva.RESERVADA);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertTrue(response.isTieneReservaEnElDia());
        assertEquals(EstadoTurnoAlumno.RESERVADO, buscar(response, "19:00").getEstado());
        assertFalse(buscar(response, "19:00").isSeleccionable());
        assertEquals(EstadoTurnoAlumno.DISPONIBLE, buscar(response, "08:00").getEstado());
        assertFalse(buscar(response, "08:00").isSeleccionable());
    }

    @Test
    void unaReservaCanceladaNoCuentaComoReservaDelDia() {
        Turno turno = turno(MIERCOLES, "08:00", 5);
        reservar(alumno, turno, EstadoReserva.CANCELADA);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertFalse(response.isTieneReservaEnElDia());
        assertEquals(EstadoTurnoAlumno.DISPONIBLE, response.getTurnos().get(0).getEstado());
        assertTrue(response.getTurnos().get(0).isSeleccionable());
    }

    @Test
    void laReservaDeOtroDiaNoBloqueaElDiaSeleccionado() {
        Turno hoy = turno(MARTES, "08:00", 5);
        turno(MIERCOLES, "08:00", 5);
        reservar(alumno, hoy, EstadoReserva.RESERVADA);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertFalse(response.isTieneReservaEnElDia());
        assertTrue(response.getTurnos().get(0).isSeleccionable());
    }

    // --- Contador semanal ---

    // Pruebas 16 y 17
    @Test
    void contadorRestaSoloLasReservasActivasDeLaSemana() {
        Turno lunes = turno(MARTES.minusDays(1), "08:00", 5);
        Turno hoy = turno(MARTES, "08:00", 5);
        Turno semanaAnterior = turno(MARTES.minusDays(7), "08:00", 5);
        reservar(alumno, lunes, EstadoReserva.RESERVADA);
        reservar(alumno, hoy, EstadoReserva.CANCELADA);
        reservar(alumno, semanaAnterior, EstadoReserva.RESERVADA);

        assertEquals(2, consultar(MIERCOLES, MARTES_10_30).getTurnosRestantesSemana());
    }

    // Prueba 19
    @Test
    void conElContadorEnCeroNoSePuedeElegirNingunTurno() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 1);
        Turno lunes = turno(MARTES.minusDays(1), "08:00", 5);
        reservar(alumno, lunes, EstadoReserva.RESERVADA);
        turno(MIERCOLES, "08:00", 5);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertEquals(0, response.getTurnosRestantesSemana());
        assertEquals(EstadoTurnoAlumno.DISPONIBLE, response.getTurnos().get(0).getEstado());
        assertFalse(response.getTurnos().get(0).isSeleccionable());
    }

    // Prueba 18: un domingo, el lunes ya es otra semana
    @Test
    void elDomingoElContadorDelLunesEsDeLaSemanaSiguiente() {
        LocalDate domingo = LocalDate.of(2026, 10, 18);
        LocalDate lunes = domingo.plusDays(1);
        LocalDateTime domingoALas9 = domingo.atTime(9, 0);
        reservar(alumno, turno(domingo.minusDays(5), "08:00", 5), EstadoReserva.RESERVADA);
        reservar(alumno, turno(domingo.minusDays(3), "08:00", 5), EstadoReserva.RESERVADA);
        reservar(alumno, turno(domingo.minusDays(1), "08:00", 5), EstadoReserva.RESERVADA);

        assertEquals(0, consultar(domingo, domingoALas9).getTurnosRestantesSemana());
        assertEquals(3, consultar(lunes, domingoALas9).getTurnosRestantesSemana());
    }

    @Test
    void sinInscripcionActivaNoLeQuedanTurnos() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, null);
        turno(MIERCOLES, "08:00", 5);

        TurnosDisponiblesResponse response = consultar(MIERCOLES, MARTES_10_30);

        assertEquals(0, response.getTurnosRestantesSemana());
        assertFalse(response.getTurnos().get(0).isSeleccionable());
    }

    @Test
    void elContadorNuncaEsNegativo() {
        alumno = alumno(10L, Modalidad.PRESENCIAL, true, 1);
        reservar(alumno, turno(MARTES.minusDays(1), "08:00", 5), EstadoReserva.RESERVADA);
        reservar(alumno, turno(MARTES, "08:00", 5), EstadoReserva.RESERVADA);

        assertEquals(0, consultar(MIERCOLES, MARTES_10_30).getTurnosRestantesSemana());
        verify(reservaRepository).findByAlumnoIdAndEstadoAndTurnoFechaBetween(
                eq(10L), eq(EstadoReserva.RESERVADA), eq(LocalDate.of(2026, 10, 12)), eq(LocalDate.of(2026, 10, 18)));
    }
}
