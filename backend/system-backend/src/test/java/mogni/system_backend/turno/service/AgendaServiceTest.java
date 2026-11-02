package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.turno.dto.AgendaDiaResponse;
import mogni.system_backend.turno.dto.AgendaSemanaResponse;
import mogni.system_backend.turno.dto.AlumnoAgendaResponse;
import mogni.system_backend.turno.dto.DiaAgendaResponse;
import mogni.system_backend.turno.dto.EstadoDiaAgenda;
import mogni.system_backend.turno.dto.EstadoTurnoAgenda;
import mogni.system_backend.turno.dto.TurnoAgendaResponse;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * MOGNI-257. Los repositorios se simulan con listas en memoria (turnos por fecha, franjas, reservas y cuotas), así
 * cada prueba arma el escenario y las consultas filtran igual que la base. "Ahora" es el jueves 15/10/2026 10:30.
 */
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final LocalDate JUEVES = LocalDate.of(2026, 10, 15);
    private static final LocalDate VIERNES = JUEVES.plusDays(1);
    private static final LocalDate SABADO = JUEVES.plusDays(2);
    private static final LocalDate DOMINGO = JUEVES.plusDays(3);
    private static final LocalDate LUNES_SIGUIENTE = JUEVES.plusDays(4);
    /** Hoy es el jueves y mañana el viernes: los alumnos solo pueden reservar esos dos días. */
    private static final LocalDateTime AHORA = JUEVES.atTime(10, 30);

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private FranjaHorariaRepository franjaHorariaRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private GeneracionTurnosService generacionTurnosService;

    @Mock
    private ConfiguracionGimnasioService configuracionGimnasioService;

    @InjectMocks
    private AgendaService agendaService;

    /** Turnos registrados en la base, por fecha (también los que "genera" GeneracionTurnosService). */
    private final Map<LocalDate, List<Turno>> turnosPorFecha = new HashMap<>();
    private final List<FranjaHoraria> franjas = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final List<Cuota> cuotas = new ArrayList<>();
    private int cupoVigente = 10;
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        lenient().when(turnoRepository.findByFechaOrderByHoraInicioAsc(any(LocalDate.class)))
                .thenAnswer(inv -> turnosDe(inv.getArgument(0)));
        lenient().when(generacionTurnosService.obtenerOGenerarTurnos(any(LocalDate.class)))
                .thenAnswer(inv -> turnosDe(inv.getArgument(0)));
        lenient().when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(any(DayOfWeek.class)))
                .thenAnswer(inv -> franjas.stream()
                        .filter(f -> f.getDiaSemana() == inv.getArgument(0))
                        .sorted(Comparator.comparing(FranjaHoraria::getHoraInicio))
                        .toList());
        lenient().when(configuracionGimnasioService.obtenerCupoMaximo()).thenAnswer(inv -> cupoVigente);
        lenient().when(reservaRepository.findWithAlumnoByTurnoIdInAndEstado(anyCollection(), any(EstadoReserva.class)))
                .thenAnswer(inv -> reservasDe(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(reservaRepository.findByTurnoIdInAndEstado(anyCollection(), any(EstadoReserva.class)))
                .thenAnswer(inv -> reservasDe(inv.getArgument(0), inv.getArgument(1)));
        // Simula la consulta: la última cuota (vencimiento más reciente) de cada alumno con inscripción activa
        lenient().when(cuotaRepository.buscarUltimasCuotas())
                .thenAnswer(inv -> cuotas.stream()
                        .filter(c -> c.getInscripcion().getEstado() == EstadoInscripcion.ACTIVA)
                        .collect(java.util.stream.Collectors.toMap(c -> c.getInscripcion().getAlumno().getId(), c -> c,
                                (a, b) -> a.getFechaVencimiento().isAfter(b.getFechaVencimiento()) ? a : b)));
    }

    // --- Armado del escenario ---

    private List<Turno> turnosDe(LocalDate fecha) {
        return turnosPorFecha.getOrDefault(fecha, List.of()).stream()
                .sorted(Comparator.comparing(Turno::getHoraInicio))
                .toList();
    }

    private List<Reserva> reservasDe(Collection<Long> turnoIds, EstadoReserva estado) {
        return reservas.stream()
                .filter(r -> turnoIds.contains(r.getTurno().getId()) && r.getEstado() == estado)
                .toList();
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

    /** Alumno con inscripción activa en la membresía indicada (sin cuotas). */
    private Alumno alumno(String nombre, String apellido, String membresia) {
        Alumno alumno = new Alumno();
        alumno.setId(proximoId++);
        alumno.setNombre(nombre);
        alumno.setApellido(apellido);
        alumno.agregarInscripcion(new Inscripcion(proximoId++, JUEVES.minusMonths(1), null, EstadoInscripcion.ACTIVA,
                alumno, new Membresia(proximoId++, membresia, 30000.0, 3, null)));
        return alumno;
    }

    private Cuota cuota(Alumno alumno, LocalDate vencimiento) {
        Cuota cuota = new Cuota();
        cuota.setId(proximoId++);
        cuota.setInscripcion(alumno.buscarInscripcionActiva().orElseThrow());
        cuota.setFechaVencimiento(vencimiento);
        cuotas.add(cuota);
        return cuota;
    }

    private Reserva reservar(Alumno alumno, Turno turno, EstadoReserva estado) {
        Reserva reserva = new Reserva(proximoId++, alumno, turno, JUEVES.atStartOfDay(), estado);
        reservas.add(reserva);
        return reserva;
    }

    /** Reserva activa de un alumno nuevo (para llenar turnos). */
    private void reservarVarios(Turno turno, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            reservar(alumno("Alumno", "N" + proximoId, "Plan"), turno, EstadoReserva.RESERVADA);
        }
    }

    private static TurnoAgendaResponse buscar(List<TurnoAgendaResponse> turnos, String horaInicio) {
        return turnos.stream()
                .filter(t -> t.getHoraInicio().equals(horaInicio))
                .findFirst()
                .orElseThrow();
    }

    private static DiaAgendaResponse buscarDia(AgendaSemanaResponse semana, LocalDate fecha) {
        return semana.getDias().stream()
                .filter(d -> d.getFecha().equals(fecha))
                .findFirst()
                .orElseThrow();
    }

    // --- Pruebas de aceptación ---

    // Prueba 1: consultar la agenda de una fecha con turnos reservados → devuelve los turnos con sus alumnos
    @Test
    void consultar_agenda_de_una_fecha_con_reservas_devuelve_los_turnos_con_sus_alumnos() {
        Turno diez = turno(JUEVES, "10:00", 10);
        Turno once = turno(JUEVES, "11:00", 10);
        Alumno sofia = alumno("Sofía", "Ruiz", "Musculación 3x");
        Alumno martin = alumno("Martín", "Fernández", "HIIT");
        Alumno valentina = alumno("Valentina", "López", "Musculación 3x");
        cuota(martin, LocalDate.of(2026, 10, 18));
        cuota(sofia, LocalDate.of(2026, 11, 2));
        reservar(sofia, once, EstadoReserva.RESERVADA);
        Reserva reservaMartin = reservar(martin, once, EstadoReserva.RESERVADA);
        reservar(valentina, diez, EstadoReserva.RESERVADA);

        AgendaDiaResponse agenda = agendaService.consultarDia(JUEVES, AHORA);

        assertEquals(JUEVES, agenda.getFecha());
        assertEquals(List.of("10:00", "11:00"), agenda.getTurnos().stream().map(TurnoAgendaResponse::getHoraInicio).toList());
        assertEquals(List.of("López"), buscar(agenda.getTurnos(), "10:00").getAlumnos().stream()
                .map(AlumnoAgendaResponse::getApellido).toList());

        // Ordenados por apellido
        List<AlumnoAgendaResponse> alumnosDeLasOnce = buscar(agenda.getTurnos(), "11:00").getAlumnos();
        assertEquals(List.of("Fernández", "Ruiz"), alumnosDeLasOnce.stream().map(AlumnoAgendaResponse::getApellido).toList());
        AlumnoAgendaResponse primero = alumnosDeLasOnce.get(0);
        assertEquals(reservaMartin.getId(), primero.getReservaId());
        assertEquals(martin.getId(), primero.getAlumnoId());
        assertEquals("Martín", primero.getNombre());
        assertEquals("HIIT", primero.getMembresiaNombre());
        assertEquals(SituacionPago.PROXIMA_A_VENCER, primero.getCuota().getSituacion());
        assertEquals(LocalDate.of(2026, 10, 18), primero.getCuota().getFechaVencimiento());
    }

    // Prueba 2: el cupo ocupado coincide con la cantidad de alumnos con reserva activa
    @Test
    void el_cupo_ocupado_coincide_con_las_reservas_activas() {
        // El cupo sale del turno (no hay un número fijo)
        Turno turno = turno(JUEVES, "18:00", 12);
        reservarVarios(turno, 5);

        AgendaDiaResponse agenda = agendaService.consultarDia(JUEVES, AHORA);

        TurnoAgendaResponse respuesta = buscar(agenda.getTurnos(), "18:00");
        assertEquals(12, respuesta.getCupoMaximo());
        assertEquals(5, respuesta.getOcupados());
        assertEquals(respuesta.getAlumnos().size(), respuesta.getOcupados());
        assertEquals(7, respuesta.getLibres());
        assertEquals(12, agenda.getCupoPorTurno());
        assertEquals(5, agenda.getResumen().getReservas());
        assertEquals(7, agenda.getResumen().getLugaresLibres());
    }

    // Prueba 3: una reserva cancelada no se cuenta como ocupada
    @Test
    void una_reserva_cancelada_no_cuenta_como_ocupada() {
        Turno turno = turno(JUEVES, "18:00", 10);
        reservar(alumno("Sofía", "Ruiz", "Plan"), turno, EstadoReserva.RESERVADA);
        reservar(alumno("Iván", "Maldonado", "Plan"), turno, EstadoReserva.RESERVADA);
        reservar(alumno("Federico", "Montez", "Plan"), turno, EstadoReserva.CANCELADA);
        reservar(alumno("Facundo", "Murua", "Plan"), turno, EstadoReserva.CANCELADA_POR_GIMNASIO);

        AgendaDiaResponse agenda = agendaService.consultarDia(JUEVES, AHORA);
        TurnoAgendaResponse respuesta = buscar(agenda.getTurnos(), "18:00");

        assertEquals(2, respuesta.getOcupados());
        assertEquals(8, respuesta.getLibres());
        assertEquals(List.of("Maldonado", "Ruiz"), respuesta.getAlumnos().stream().map(AlumnoAgendaResponse::getApellido).toList());
        assertEquals(2, agenda.getResumen().getReservas());

        // En la vista semanal tampoco cuentan
        TurnoAgendaResponse enLaSemana = buscar(buscarDia(agendaService.consultarSemana(JUEVES, AHORA), JUEVES).getTurnos(), "18:00");
        assertEquals(2, enLaSemana.getOcupados());
        assertEquals(8, enLaSemana.getLibres());
    }

    // Prueba 4: consultar una fecha sin reservas → devuelve los turnos con 0 ocupados y listado vacío
    @Test
    void una_fecha_sin_reservas_devuelve_los_turnos_con_0_ocupados_y_listado_vacio() {
        turno(VIERNES, "08:00", 10);
        turno(VIERNES, "09:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(VIERNES, AHORA);

        assertEquals(2, agenda.getTurnos().size());
        for (TurnoAgendaResponse turno : agenda.getTurnos()) {
            assertEquals(0, turno.getOcupados());
            assertEquals(10, turno.getLibres());
            assertTrue(turno.getAlumnos().isEmpty());
            assertEquals(EstadoTurnoAgenda.CON_LUGARES, turno.getEstado());
        }
        assertEquals(2, agenda.getResumen().getCantidadTurnos());
        assertEquals(0, agenda.getResumen().getReservas());
        assertEquals(0, agenda.getResumen().getTurnosCompletos());
        assertEquals(20, agenda.getResumen().getLugaresLibres());
        // Sin reservas no hace falta buscar cuotas
        verifyNoInteractions(cuotaRepository);
    }

    // --- Origen de los turnos y transacciones ---

    @Test
    void hoy_obtiene_o_genera_sus_turnos_y_muestra_los_que_devuelve() {
        turno(JUEVES, "19:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(null, AHORA);

        verify(generacionTurnosService).obtenerOGenerarTurnos(JUEVES);
        assertEquals(JUEVES, agenda.getHoy());
        assertEquals(JUEVES, agenda.getFecha());
        assertEquals(EstadoDiaAgenda.RESERVAS_ABIERTAS, agenda.getEstadoDia());
        assertEquals(List.of("19:00"), agenda.getTurnos().stream().map(TurnoAgendaResponse::getHoraInicio).toList());
    }

    @Test
    void maniana_tiene_reservas_abiertas_y_genera_sus_turnos() {
        Turno turno = turno(VIERNES, "08:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(VIERNES, AHORA);

        verify(generacionTurnosService).obtenerOGenerarTurnos(VIERNES);
        verify(turnoRepository, never()).findByFechaOrderByHoraInicioAsc(any());
        assertEquals(EstadoDiaAgenda.RESERVAS_ABIERTAS, agenda.getEstadoDia());
        assertNull(agenda.getFechaHabilitacion());
        assertNotNull(agenda.getResumen());
        assertEquals(turno.getId(), agenda.getTurnos().get(0).getTurnoId());
        assertEquals(EstadoTurnoAgenda.CON_LUGARES, agenda.getTurnos().get(0).getEstado());
    }

    @Test
    void las_fechas_que_no_son_hoy_ni_maniana_nunca_generan_ni_guardan_turnos() {
        franjas.add(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        turno(LUNES, "08:00", 10);

        agendaService.consultarDia(LUNES, AHORA);
        agendaService.consultarDia(SABADO, AHORA);
        agendaService.consultarDia(LUNES_SIGUIENTE, AHORA);

        verify(generacionTurnosService, never()).obtenerOGenerarTurnos(any());
        verify(turnoRepository, never()).save(any());
        verify(turnoRepository, never()).saveAll(anyList());
        verify(turnoRepository, never()).saveAllAndFlush(anyList());
    }

    /**
     * La consulta es readOnly (igual que la del alumno) y los turnos de hoy y mañana igual quedan guardados:
     * los inserta InsercionTurnosService en una transacción propia (REQUIRES_NEW), que no es de solo lectura.
     */
    @Test
    void la_consulta_es_de_solo_lectura_y_la_insercion_de_turnos_usa_su_propia_transaccion() throws Exception {
        Transactional dia = AgendaService.class.getMethod("consultarDia", LocalDate.class).getAnnotation(Transactional.class);
        Transactional semana = AgendaService.class.getMethod("consultarSemana", LocalDate.class).getAnnotation(Transactional.class);
        Transactional insercion = InsercionTurnosService.class.getMethod("insertar", List.class).getAnnotation(Transactional.class);

        assertTrue(dia.readOnly());
        assertTrue(semana.readOnly());
        assertEquals(Propagation.REQUIRES_NEW, insercion.propagation());
        assertFalse(insercion.readOnly());
    }

    // --- Estados del día ---

    @Test
    void un_dia_bloqueado_se_calcula_desde_las_franjas_con_el_cupo_vigente_sin_guardarse() {
        cupoVigente = 15;
        franjas.add(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(17, 0)));
        franjas.add(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));

        AgendaDiaResponse agenda = agendaService.consultarDia(LUNES_SIGUIENTE, AHORA);

        assertEquals(EstadoDiaAgenda.BLOQUEADO, agenda.getEstadoDia());
        // Se habilita el día anterior (domingo 18/10)
        assertEquals(DOMINGO, agenda.getFechaHabilitacion());
        assertNull(agenda.getResumen());
        assertEquals(15, agenda.getCupoPorTurno());
        assertEquals(List.of("08:00", "09:00", "16:00"), agenda.getTurnos().stream().map(TurnoAgendaResponse::getHoraInicio).toList());
        for (TurnoAgendaResponse turno : agenda.getTurnos()) {
            assertNull(turno.getTurnoId());
            assertEquals(15, turno.getCupoMaximo());
            assertEquals(EstadoTurnoAgenda.BLOQUEADO, turno.getEstado());
            assertEquals(0, turno.getOcupados());
            assertTrue(turno.getAlumnos().isEmpty());
        }
        verify(reservaRepository, never()).findWithAlumnoByTurnoIdInAndEstado(anyCollection(), any());
    }

    @Test
    void un_dia_bloqueado_con_turnos_registrados_usa_esos_turnos() {
        // Por ejemplo, un turno cancelado de antemano (HU-258)
        franjas.add(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        Turno cancelado = turno(LUNES_SIGUIENTE, "08:00", 10, EstadoTurno.CANCELADO);
        turno(LUNES_SIGUIENTE, "09:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(LUNES_SIGUIENTE, AHORA);

        assertEquals(2, agenda.getTurnos().size());
        assertEquals(cancelado.getId(), buscar(agenda.getTurnos(), "08:00").getTurnoId());
        assertEquals(EstadoTurnoAgenda.CANCELADO, buscar(agenda.getTurnos(), "08:00").getEstado());
        assertEquals(EstadoTurnoAgenda.BLOQUEADO, buscar(agenda.getTurnos(), "09:00").getEstado());
        verify(franjaHorariaRepository, never()).findByDiaSemanaOrderByHoraInicioAsc(any());
    }

    @Test
    void un_dia_sin_franjas_queda_sin_horarios() {
        AgendaDiaResponse bloqueado = agendaService.consultarDia(SABADO, AHORA);
        AgendaDiaResponse maniana = agendaService.consultarDia(VIERNES, AHORA);

        for (AgendaDiaResponse agenda : List.of(bloqueado, maniana)) {
            assertEquals(EstadoDiaAgenda.SIN_HORARIOS, agenda.getEstadoDia());
            assertTrue(agenda.getTurnos().isEmpty());
            assertNull(agenda.getResumen());
            assertNull(agenda.getCupoPorTurno());
            assertNull(agenda.getFechaHabilitacion());
        }
    }

    @Test
    void un_dia_pasado_muestra_sus_turnos_y_alumnos_como_pasados() {
        Turno turno = turno(LUNES, "08:00", 10);
        reservar(alumno("Sofía", "Ruiz", "Plan"), turno, EstadoReserva.RESERVADA);
        reservarVarios(turno, 6);

        AgendaDiaResponse agenda = agendaService.consultarDia(LUNES, AHORA);

        assertEquals(EstadoDiaAgenda.PASADO, agenda.getEstadoDia());
        TurnoAgendaResponse respuesta = buscar(agenda.getTurnos(), "08:00");
        assertEquals(EstadoTurnoAgenda.PASADO, respuesta.getEstado());
        assertEquals(7, respuesta.getOcupados());
        assertEquals(7, respuesta.getAlumnos().size());
        assertEquals(7, agenda.getResumen().getReservas());
    }

    @Test
    void un_dia_pasado_sin_turnos_registrados_queda_sin_horarios_sin_mirar_las_franjas() {
        franjas.add(new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        AgendaDiaResponse agenda = agendaService.consultarDia(LUNES, AHORA);

        assertEquals(EstadoDiaAgenda.SIN_HORARIOS, agenda.getEstadoDia());
        assertTrue(agenda.getTurnos().isEmpty());
        verify(franjaHorariaRepository, never()).findByDiaSemanaOrderByHoraInicioAsc(any());
    }

    // --- Estados de los turnos ---

    @Test
    void casi_lleno_depende_del_cupo_del_turno() {
        assertEquals(8, AgendaService.umbralCasiLleno(10));
        assertEquals(16, AgendaService.umbralCasiLleno(20));
        assertEquals(4, AgendaService.umbralCasiLleno(5));
        assertEquals(10, AgendaService.umbralCasiLleno(12));

        reservarVarios(turno(VIERNES, "08:00", 10), 7);
        reservarVarios(turno(VIERNES, "09:00", 10), 8);
        reservarVarios(turno(VIERNES, "10:00", 10), 9);
        reservarVarios(turno(VIERNES, "11:00", 10), 10);
        reservarVarios(turno(VIERNES, "16:00", 20), 15);
        reservarVarios(turno(VIERNES, "17:00", 20), 16);

        AgendaDiaResponse agenda = agendaService.consultarDia(VIERNES, AHORA);

        assertEquals(EstadoTurnoAgenda.CON_LUGARES, buscar(agenda.getTurnos(), "08:00").getEstado());
        assertEquals(EstadoTurnoAgenda.CASI_LLENO, buscar(agenda.getTurnos(), "09:00").getEstado());
        assertEquals(EstadoTurnoAgenda.CASI_LLENO, buscar(agenda.getTurnos(), "10:00").getEstado());
        assertEquals(EstadoTurnoAgenda.COMPLETO, buscar(agenda.getTurnos(), "11:00").getEstado());
        assertEquals(EstadoTurnoAgenda.CON_LUGARES, buscar(agenda.getTurnos(), "16:00").getEstado());
        assertEquals(EstadoTurnoAgenda.CASI_LLENO, buscar(agenda.getTurnos(), "17:00").getEstado());
        assertEquals(1, agenda.getResumen().getTurnosCompletos());
    }

    @Test
    void hoy_marca_el_turno_en_curso_y_los_que_ya_terminaron() {
        turno(JUEVES, "09:00", 10);
        turno(JUEVES, "10:00", 10);
        turno(JUEVES, "11:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(JUEVES, AHORA);

        TurnoAgendaResponse nueve = buscar(agenda.getTurnos(), "09:00");
        TurnoAgendaResponse diez = buscar(agenda.getTurnos(), "10:00");
        TurnoAgendaResponse once = buscar(agenda.getTurnos(), "11:00");
        assertEquals(EstadoTurnoAgenda.PASADO, nueve.getEstado());
        assertFalse(nueve.isEnCurso());
        assertTrue(diez.isEnCurso());
        assertEquals(EstadoTurnoAgenda.CON_LUGARES, diez.getEstado());
        assertFalse(once.isEnCurso());
    }

    @Test
    void un_turno_cancelado_no_cuenta_en_el_resumen() {
        turno(VIERNES, "08:00", 10, EstadoTurno.CANCELADO);
        turno(VIERNES, "09:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(VIERNES, AHORA);

        assertEquals(EstadoTurnoAgenda.CANCELADO, buscar(agenda.getTurnos(), "08:00").getEstado());
        assertEquals(1, agenda.getResumen().getCantidadTurnos());
        assertEquals(10, agenda.getResumen().getLugaresLibres());
    }

    @Test
    void un_turno_cancelado_a_proposito_que_no_empezo_se_puede_reactivar() {
        turno(VIERNES, "08:00", 10, EstadoTurno.CANCELADO);
        turno(VIERNES, "09:00", 10);

        AgendaDiaResponse agenda = agendaService.consultarDia(VIERNES, AHORA);

        assertTrue(buscar(agenda.getTurnos(), "08:00").isPuedeReactivarse());
        assertFalse(buscar(agenda.getTurnos(), "08:00").isCanceladoPorFranja());
        // Un turno que no está cancelado no se reactiva
        assertFalse(buscar(agenda.getTurnos(), "09:00").isPuedeReactivarse());
    }

    @Test
    void un_turno_cancelado_por_cambio_de_franja_no_se_reactiva_a_mano() {
        LocalTime inicio = LocalTime.parse("08:00");
        Turno huerfano = new Turno(proximoId++, VIERNES, inicio, inicio.plusHours(1), 10, EstadoTurno.CANCELADO,
                MotivoCancelacionTurno.FRANJA_ELIMINADA);
        turnosPorFecha.computeIfAbsent(VIERNES, f -> new ArrayList<>()).add(huerfano);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(VIERNES, AHORA).getTurnos(), "08:00");

        assertFalse(respuesta.isPuedeReactivarse());
        assertTrue(respuesta.isCanceladoPorFranja());
    }

    @Test
    void un_turno_cancelado_que_ya_empezo_no_se_puede_reactivar() {
        turno(JUEVES, "09:00", 10, EstadoTurno.CANCELADO);

        AgendaDiaResponse agenda = agendaService.consultarDia(JUEVES, AHORA);

        assertFalse(buscar(agenda.getTurnos(), "09:00").isPuedeReactivarse());
    }

    // --- Cuota del alumno ---

    @Test
    void el_alumno_muestra_la_situacion_de_su_ultima_cuota() {
        Turno turno = turno(JUEVES, "18:00", 10);
        Alumno conCuotas = alumno("Martín", "Fernández", "HIIT");
        cuota(conCuotas, LocalDate.of(2026, 9, 18));
        cuota(conCuotas, LocalDate.of(2026, 10, 10));
        Alumno sinCuotas = alumno("Sofía", "Ruiz", "Full 5x");
        reservar(conCuotas, turno, EstadoReserva.RESERVADA);
        reservar(sinCuotas, turno, EstadoReserva.RESERVADA);

        List<AlumnoAgendaResponse> alumnos = buscar(agendaService.consultarDia(JUEVES, AHORA).getTurnos(), "18:00").getAlumnos();

        // La situación se calcula con la última cuota (venció el 10/10: en mora)
        assertEquals(SituacionPago.EN_MORA, alumnos.get(0).getCuota().getSituacion());
        assertEquals(LocalDate.of(2026, 10, 10), alumnos.get(0).getCuota().getFechaVencimiento());
        // Sin cuotas: la membresía sale de la inscripción activa
        assertNull(alumnos.get(1).getCuota());
        assertEquals("Full 5x", alumnos.get(1).getMembresiaNombre());
    }

    // --- Vista semanal ---

    @Test
    void la_semana_va_de_lunes_a_domingo_con_el_estado_de_cada_dia_y_sin_alumnos() {
        turno(LUNES, "08:00", 10);
        Turno hoy = turno(JUEVES, "18:00", 10);
        turno(VIERNES, "08:00", 10);
        franjas.add(new FranjaHoraria(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        reservarVarios(hoy, 9);

        AgendaSemanaResponse semana = agendaService.consultarSemana(JUEVES, AHORA);

        assertEquals(JUEVES, semana.getHoy());
        assertEquals(LUNES, semana.getDesde());
        assertEquals(DOMINGO, semana.getHasta());
        assertEquals(7, semana.getDias().size());
        assertEquals(LUNES, semana.getDias().get(0).getFecha());
        assertEquals(DOMINGO, semana.getDias().get(6).getFecha());

        assertEquals(EstadoDiaAgenda.PASADO, buscarDia(semana, LUNES).getEstadoDia());
        assertEquals(EstadoDiaAgenda.SIN_HORARIOS, buscarDia(semana, LUNES.plusDays(1)).getEstadoDia());
        assertEquals(EstadoDiaAgenda.RESERVAS_ABIERTAS, buscarDia(semana, JUEVES).getEstadoDia());
        assertEquals(EstadoDiaAgenda.RESERVAS_ABIERTAS, buscarDia(semana, VIERNES).getEstadoDia());
        DiaAgendaResponse sabado = buscarDia(semana, SABADO);
        assertEquals(EstadoDiaAgenda.BLOQUEADO, sabado.getEstadoDia());
        assertEquals(VIERNES, sabado.getFechaHabilitacion());
        assertEquals(2, sabado.getTurnos().size());
        assertEquals(EstadoDiaAgenda.SIN_HORARIOS, buscarDia(semana, DOMINGO).getEstadoDia());

        TurnoAgendaResponse turnoDeHoy = buscar(buscarDia(semana, JUEVES).getTurnos(), "18:00");
        assertEquals(9, turnoDeHoy.getOcupados());
        assertEquals(EstadoTurnoAgenda.CASI_LLENO, turnoDeHoy.getEstado());
        assertTrue(turnoDeHoy.getAlumnos().isEmpty());

        // Solo hoy y mañana pasan por la generación de turnos
        verify(generacionTurnosService).obtenerOGenerarTurnos(JUEVES);
        verify(generacionTurnosService).obtenerOGenerarTurnos(VIERNES);
        verify(generacionTurnosService, never()).obtenerOGenerarTurnos(LUNES);
        verify(generacionTurnosService, never()).obtenerOGenerarTurnos(SABADO);
    }

    @Test
    void la_semana_sin_fecha_es_la_actual() {
        AgendaSemanaResponse semana = agendaService.consultarSemana(null, AHORA);

        assertEquals(LUNES, semana.getDesde());
        assertEquals(DOMINGO, semana.getHasta());
    }

    // --- HU-262: asistencia en la agenda ---

    private Reserva reservarConAsistencia(Turno turno, Asistencia asistencia) {
        Reserva reserva = reservar(alumno("Alumno", "N" + proximoId, "Plan"), turno, EstadoReserva.RESERVADA);
        reserva.setAsistencia(asistencia);
        return reserva;
    }

    // HU-262 prueba 7: la agenda muestra la cantidad correcta de presentes y ausentes del turno
    @Test
    void un_turno_que_ya_comenzo_muestra_presentes_ausentes_y_sin_marcar() {
        // A las 10:30 el turno de 10 está en curso
        Turno diez = turno(JUEVES, "10:00", 8);
        reservarConAsistencia(diez, Asistencia.PRESENTE);
        reservarConAsistencia(diez, Asistencia.PRESENTE);
        reservarConAsistencia(diez, Asistencia.AUSENTE);
        reservarConAsistencia(diez, null);
        // Una cancelada no ocupa lugar ni cuenta para la asistencia
        reservar(alumno("Iván", "Maldonado", "Plan"), diez, EstadoReserva.CANCELADA);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(JUEVES, AHORA).getTurnos(), "10:00");

        assertTrue(respuesta.isComenzo());
        assertTrue(respuesta.isAsistenciaEditable());
        assertEquals(2, respuesta.getPresentes());
        assertEquals(1, respuesta.getAusentes());
        assertEquals(1, respuesta.getSinMarcar());
        assertEquals(4, respuesta.getOcupados());
        assertEquals(1, respuesta.getAlumnos().stream().filter(a -> a.getAsistencia() == null).count());
    }

    @Test
    void un_turno_de_hoy_que_ya_termino_se_puede_seguir_corrigiendo() {
        Turno nueve = turno(JUEVES, "09:00", 8);
        reservarConAsistencia(nueve, Asistencia.AUSENTE);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(JUEVES, AHORA).getTurnos(), "09:00");

        assertEquals(EstadoTurnoAgenda.PASADO, respuesta.getEstado());
        assertTrue(respuesta.isAsistenciaEditable());
        assertEquals(1, respuesta.getAusentes());
    }

    @Test
    void un_turno_que_todavia_no_comenzo_no_tiene_asistencia() {
        Turno once = turno(JUEVES, "11:00", 8);
        reservarConAsistencia(once, null);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(JUEVES, AHORA).getTurnos(), "11:00");

        assertFalse(respuesta.isComenzo());
        assertFalse(respuesta.isAsistenciaEditable());
        assertEquals(0, respuesta.getPresentes());
        assertEquals(0, respuesta.getAusentes());
        assertEquals(0, respuesta.getSinMarcar());
        assertNull(respuesta.getAlumnos().get(0).getAsistencia());
    }

    // HU-262 prueba 6: en un día terminado las reservas sin marcar se muestran como «Ausente» y ya no se editan
    @Test
    void en_un_dia_terminado_las_sin_marcar_cuentan_como_ausentes() {
        LocalDate miercoles = JUEVES.minusDays(1);
        Turno ayer = turno(miercoles, "18:00", 8);
        reservarConAsistencia(ayer, Asistencia.PRESENTE);
        reservarConAsistencia(ayer, null);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(miercoles, AHORA).getTurnos(), "18:00");

        assertTrue(respuesta.isComenzo());
        assertFalse(respuesta.isAsistenciaEditable());
        assertEquals(1, respuesta.getPresentes());
        assertEquals(1, respuesta.getAusentes());
        assertEquals(0, respuesta.getSinMarcar());
        assertTrue(respuesta.getAlumnos().stream().noneMatch(a -> a.getAsistencia() == null));
    }

    @Test
    void un_turno_cancelado_no_tiene_asistencia() {
        Turno nueve = turno(JUEVES, "09:00", 8, EstadoTurno.CANCELADO);

        TurnoAgendaResponse respuesta = buscar(agendaService.consultarDia(JUEVES, AHORA).getTurnos(), "09:00");

        assertFalse(respuesta.isComenzo());
        assertFalse(respuesta.isAsistenciaEditable());
    }

    @Test
    void la_vista_semana_tambien_informa_presentes_y_ausentes() {
        Turno diez = turno(JUEVES, "10:00", 8);
        reservarConAsistencia(diez, Asistencia.PRESENTE);
        reservarConAsistencia(diez, Asistencia.AUSENTE);

        AgendaSemanaResponse semana = agendaService.consultarSemana(JUEVES, AHORA);

        TurnoAgendaResponse respuesta = buscar(buscarDia(semana, JUEVES).getTurnos(), "10:00");
        assertEquals(1, respuesta.getPresentes());
        assertEquals(1, respuesta.getAusentes());
        assertEquals(2, respuesta.getOcupados());
    }
}
