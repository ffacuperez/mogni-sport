package mogni.system_backend.turno.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.turno.dto.AgendaDiaResponse;
import mogni.system_backend.turno.dto.AgendaSemanaResponse;
import mogni.system_backend.turno.dto.AlumnoAgendaResponse;
import mogni.system_backend.turno.dto.CuotaAgendaResponse;
import mogni.system_backend.turno.dto.DiaAgendaResponse;
import mogni.system_backend.turno.dto.EstadoDiaAgenda;
import mogni.system_backend.turno.dto.EstadoTurnoAgenda;
import mogni.system_backend.turno.dto.ResumenAgendaResponse;
import mogni.system_backend.turno.dto.TurnoAgendaResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * MOGNI-257: agenda de turnos para el personal (propietario y entrenador). Muestra, por fecha y turno, los
 * alumnos con reserva activa y los lugares ocupados y libres. Solo lee: lo único que se puede guardar son los
 * turnos de hoy y mañana, que se generan igual que cuando los consulta un alumno (HU-259).
 */
@Service
public class AgendaService {

    /** Un turno está "casi lleno" desde este porcentaje de su cupo (cupo 10 → 8 y 9 ocupados). */
    static final int PORCENTAJE_CASI_LLENO = 80;

    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final CuotaRepository cuotaRepository;
    private final GeneracionTurnosService generacionTurnosService;
    private final ConfiguracionGimnasioService configuracionGimnasioService;

    public AgendaService(TurnoRepository turnoRepository,
                         ReservaRepository reservaRepository,
                         FranjaHorariaRepository franjaHorariaRepository,
                         CuotaRepository cuotaRepository,
                         GeneracionTurnosService generacionTurnosService,
                         ConfiguracionGimnasioService configuracionGimnasioService) {
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.cuotaRepository = cuotaRepository;
        this.generacionTurnosService = generacionTurnosService;
        this.configuracionGimnasioService = configuracionGimnasioService;
    }

    /**
     * Vista Día: turnos de la fecha con sus alumnos. fecha null = hoy. Es readOnly como la consulta del alumno:
     * si hay que generar los turnos de hoy o mañana, los guarda InsercionTurnosService en su propia transacción.
     */
    @Transactional(readOnly = true)
    public AgendaDiaResponse consultarDia(LocalDate fecha) {
        return consultarDia(fecha, ZonaHoraria.ahora());
    }

    /** Vista Semana: de lunes a domingo de la semana de la fecha (null = esta semana), sin alumnos. */
    @Transactional(readOnly = true)
    public AgendaSemanaResponse consultarSemana(LocalDate fecha) {
        return consultarSemana(fecha, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    AgendaDiaResponse consultarDia(LocalDate fecha, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDate dia = fecha != null ? fecha : hoy;
        List<Turno> turnos = turnosDe(dia, hoy);
        EstadoDiaAgenda estadoDia = estadoDia(dia, hoy, turnos);

        // Los turnos calculados (días bloqueados) no tienen id ni reservas
        List<Long> ids = turnos.stream().map(Turno::getId).filter(Objects::nonNull).toList();
        Map<Long, List<Reserva>> reservasPorTurno = ids.isEmpty() ? Map.of()
                : reservaRepository.findWithAlumnoByTurnoIdInAndEstado(ids, EstadoReserva.RESERVADA).stream()
                        .collect(Collectors.groupingBy(reserva -> reserva.getTurno().getId()));
        Map<Long, Cuota> cuotaPorAlumno = reservasPorTurno.isEmpty() ? Map.of() : cuotaActualPorAlumno();

        List<TurnoAgendaResponse> turnosResponse = turnos.stream()
                .map(turno -> {
                    List<Reserva> reservas = turno.getId() == null ? List.of()
                            : reservasPorTurno.getOrDefault(turno.getId(), List.of());
                    List<AlumnoAgendaResponse> alumnos = reservas.stream()
                            .map(reserva -> mapearAlumno(reserva, cuotaPorAlumno.get(reserva.getAlumno().getId())))
                            .sorted(Comparator.comparing(AlumnoAgendaResponse::getApellido, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                    .thenComparing(AlumnoAgendaResponse::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                            .toList();
                    return mapearTurno(turno, reservas.size(), alumnos, estadoDia, ahora);
                })
                .toList();

        boolean conResumen = estadoDia == EstadoDiaAgenda.RESERVAS_ABIERTAS || estadoDia == EstadoDiaAgenda.PASADO;
        return new AgendaDiaResponse(
                hoy,
                dia,
                estadoDia,
                fechaHabilitacion(dia, estadoDia),
                turnos.isEmpty() ? null : turnos.get(0).getCupoMaximo(),
                conResumen ? resumir(turnosResponse) : null,
                turnosResponse);
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    AgendaSemanaResponse consultarSemana(LocalDate fecha, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDate lunes = (fecha != null ? fecha : hoy).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> fechas = IntStream.range(0, 7).mapToObj(lunes::plusDays).toList();
        Map<LocalDate, List<Turno>> turnosPorFecha = fechas.stream()
                .collect(Collectors.toMap(dia -> dia, dia -> turnosDe(dia, hoy)));

        // Una sola consulta de reservas para toda la semana
        List<Long> ids = turnosPorFecha.values().stream().flatMap(List::stream)
                .map(Turno::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> ocupadosPorTurno = ids.isEmpty() ? Map.of()
                : reservaRepository.findByTurnoIdInAndEstado(ids, EstadoReserva.RESERVADA).stream()
                        .collect(Collectors.groupingBy(reserva -> reserva.getTurno().getId(), Collectors.counting()));

        List<DiaAgendaResponse> dias = fechas.stream()
                .map(dia -> {
                    List<Turno> turnos = turnosPorFecha.get(dia);
                    EstadoDiaAgenda estadoDia = estadoDia(dia, hoy, turnos);
                    List<TurnoAgendaResponse> turnosResponse = turnos.stream()
                            .map(turno -> mapearTurno(turno,
                                    turno.getId() == null ? 0 : ocupadosPorTurno.getOrDefault(turno.getId(), 0L).intValue(),
                                    List.of(), estadoDia, ahora))
                            .toList();
                    return new DiaAgendaResponse(dia, estadoDia, fechaHabilitacion(dia, estadoDia), turnosResponse);
                })
                .toList();
        return new AgendaSemanaResponse(hoy, lunes, lunes.plusDays(6), dias);
    }

    /**
     * Turnos de la fecha, ordenados por hora de inicio:
     * - hoy y mañana (reservas abiertas): se obtienen o generan igual que para el alumno, así el personal ve
     *   exactamente los mismos turnos y cupos;
     * - días pasados: solo los turnos registrados (si nunca se generaron, el día no tiene turnos);
     * - después de mañana (bloqueado): los registrados (p. ej. cancelados de antemano) o, si no hay, los que
     *   saldrían de las franjas con el cupo vigente. Estos se calculan sin guardarse (quedan sin id).
     */
    private List<Turno> turnosDe(LocalDate dia, LocalDate hoy) {
        if (!dia.isBefore(hoy) && !dia.isAfter(hoy.plusDays(1))) {
            return generacionTurnosService.obtenerOGenerarTurnos(dia);
        }
        List<Turno> registrados = turnoRepository.findByFechaOrderByHoraInicioAsc(dia);
        if (!registrados.isEmpty() || dia.isBefore(hoy)) {
            return registrados;
        }
        List<FranjaHoraria> franjas = franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(dia.getDayOfWeek());
        if (franjas.isEmpty()) {
            return List.of();
        }
        int cupo = configuracionGimnasioService.obtenerCupoMaximo();
        return franjas.stream()
                .flatMap(franja -> franja.turnosCalculados().stream())
                .map(horario -> new Turno(dia, horario, cupo))
                .sorted(Comparator.comparing(Turno::getHoraInicio))
                .toList();
    }

    static EstadoDiaAgenda estadoDia(LocalDate dia, LocalDate hoy, List<Turno> turnos) {
        if (turnos.isEmpty()) {
            return EstadoDiaAgenda.SIN_HORARIOS;
        }
        if (dia.isBefore(hoy)) {
            return EstadoDiaAgenda.PASADO;
        }
        if (dia.isAfter(hoy.plusDays(1))) {
            return EstadoDiaAgenda.BLOQUEADO;
        }
        return EstadoDiaAgenda.RESERVAS_ABIERTAS;
    }

    /** Los alumnos pueden reservar hoy y mañana: un día bloqueado se habilita el día anterior. */
    private static LocalDate fechaHabilitacion(LocalDate dia, EstadoDiaAgenda estadoDia) {
        return estadoDia == EstadoDiaAgenda.BLOQUEADO ? dia.minusDays(1) : null;
    }

    /** Ocupados desde los que un turno está casi lleno: el porcentaje del cupo, redondeado hacia arriba. */
    static int umbralCasiLleno(int cupoMaximo) {
        return (cupoMaximo * PORCENTAJE_CASI_LLENO + 99) / 100;
    }

    private static TurnoAgendaResponse mapearTurno(Turno turno, int ocupados, List<AlumnoAgendaResponse> alumnos,
                                                   EstadoDiaAgenda estadoDia, LocalDateTime ahora) {
        int libres = Math.max(0, turno.getCupoMaximo() - ocupados);
        boolean cancelado = turno.getEstado() == EstadoTurno.CANCELADO;
        LocalTime horaActual = ahora.toLocalTime();
        boolean esHoy = turno.getFecha().equals(ahora.toLocalDate());
        boolean enCurso = !cancelado && esHoy
                && !turno.getHoraInicio().isAfter(horaActual) && turno.getHoraFin().isAfter(horaActual);

        EstadoTurnoAgenda estado;
        if (cancelado) {
            estado = EstadoTurnoAgenda.CANCELADO;
        } else if (!turno.getFecha().atTime(turno.getHoraFin()).isAfter(ahora)) {
            estado = EstadoTurnoAgenda.PASADO;
        } else if (estadoDia == EstadoDiaAgenda.BLOQUEADO) {
            estado = EstadoTurnoAgenda.BLOQUEADO;
        } else if (libres == 0) {
            estado = EstadoTurnoAgenda.COMPLETO;
        } else if (ocupados >= umbralCasiLleno(turno.getCupoMaximo())) {
            estado = EstadoTurnoAgenda.CASI_LLENO;
        } else {
            estado = EstadoTurnoAgenda.CON_LUGARES;
        }

        return new TurnoAgendaResponse(
                turno.getId(),
                FranjaHorariaService.formatearHora(turno.getHoraInicio()),
                FranjaHorariaService.formatearHora(turno.getHoraFin()),
                turno.getCupoMaximo(),
                ocupados,
                libres,
                estado,
                enCurso,
                alumnos);
    }

    /** Tarjetas del día. Los turnos cancelados no cuentan. */
    private static ResumenAgendaResponse resumir(List<TurnoAgendaResponse> turnos) {
        List<TurnoAgendaResponse> vigentes = turnos.stream()
                .filter(turno -> turno.getEstado() != EstadoTurnoAgenda.CANCELADO)
                .toList();
        return new ResumenAgendaResponse(
                vigentes.size(),
                vigentes.stream().mapToInt(TurnoAgendaResponse::getOcupados).sum(),
                (int) vigentes.stream().filter(turno -> turno.getLibres() == 0).count(),
                vigentes.stream().mapToInt(TurnoAgendaResponse::getLibres).sum());
    }

    /**
     * Cuota más reciente (por vencimiento, como en AlumnoService) de cada alumno con inscripción activa. Se usa la
     * misma consulta que la pantalla de Cuotas: trae inscripción y membresía en una sola consulta.
     */
    private Map<Long, Cuota> cuotaActualPorAlumno() {
        BinaryOperator<Cuota> laMasReciente = (a, b) -> a.getFechaVencimiento().isBefore(b.getFechaVencimiento()) ? b : a;
        return cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, null).stream()
                .collect(Collectors.toMap(cuota -> cuota.getInscripcion().getAlumno().getId(), cuota -> cuota, laMasReciente));
    }

    private static AlumnoAgendaResponse mapearAlumno(Reserva reserva, Cuota cuota) {
        Alumno alumno = reserva.getAlumno();
        String membresia = cuota != null
                ? cuota.getInscripcion().getMembresia().getNombre()
                // Inscripción activa sin cuotas (datos viejos): la membresía se busca en el alumno
                : alumno.buscarInscripcionActiva().map(inscripcion -> inscripcion.getMembresia().getNombre()).orElse(null);
        return new AlumnoAgendaResponse(
                reserva.getId(),
                alumno.getId(),
                alumno.getNombre(),
                alumno.getApellido(),
                membresia,
                cuota != null ? new CuotaAgendaResponse(cuota.getEstado(), cuota.getFechaVencimiento()) : null);
    }
}
