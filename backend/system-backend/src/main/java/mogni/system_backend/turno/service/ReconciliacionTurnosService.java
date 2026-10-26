package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.turno.dto.CancelacionGimnasioResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.HorarioTurno;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * MOGNI-252: mantiene los turnos ya generados de una fecha alineados con las franjas vigentes y con el cupo.
 * Solo toca turnos que todavía no empezaron. Cada fecha se reconcilia en su propia transacción (REQUIRES_NEW) para
 * que los locks duren poco y para que funcione tanto después de editar la agenda como desde una consulta readOnly.
 *
 * Orden de locks, igual que ReservaService: primero los alumnos con reserva (por id) y después el turno. Si una
 * reserva en curso igual provoca un deadlock, la base aborta una de las dos transacciones; el error se atrapa en
 * quien llama y la próxima consulta vuelve a reconciliar (todo es idempotente).
 */
@Service
public class ReconciliacionTurnosService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacionTurnosService.class);

    /** Qué hay que hacer en una fecha para que sus turnos coincidan con las franjas. */
    public record Plan(List<HorarioTurno> aCrear, List<Turno> aReactivar, List<Turno> aCancelar) {

        public boolean estaVacio() {
            return aCrear.isEmpty() && aReactivar.isEmpty() && aCancelar.isEmpty();
        }
    }

    public record Resultado(int creados, int reactivados, int cancelados, int reservasCanceladas) {

        static final Resultado SIN_CAMBIOS = new Resultado(0, 0, 0, 0);

        public boolean huboCambios() {
            return creados > 0 || reactivados > 0 || cancelados > 0;
        }
    }

    private final TurnoRepository turnoRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ReservaRepository reservaRepository;
    private final AlumnoRepository alumnoRepository;
    private final ConfiguracionGimnasioService configuracionGimnasioService;
    private final CancelacionGimnasioService cancelacionGimnasioService;
    private final EntityManager entityManager;

    // @Lazy: CancelacionGimnasioService usa GeneracionTurnosService, que a su vez usa este servicio (ciclo)
    public ReconciliacionTurnosService(TurnoRepository turnoRepository,
                                       FranjaHorariaRepository franjaHorariaRepository,
                                       ReservaRepository reservaRepository,
                                       AlumnoRepository alumnoRepository,
                                       ConfiguracionGimnasioService configuracionGimnasioService,
                                       @Lazy CancelacionGimnasioService cancelacionGimnasioService,
                                       EntityManager entityManager) {
        this.turnoRepository = turnoRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.reservaRepository = reservaRepository;
        this.alumnoRepository = alumnoRepository;
        this.configuracionGimnasioService = configuracionGimnasioService;
        this.cancelacionGimnasioService = cancelacionGimnasioService;
        this.entityManager = entityManager;
    }

    /**
     * Diferencias entre los turnos registrados de la fecha y los que salen de las franjas. Los turnos que ya
     * empezaron no entran nunca en el plan; los CANCELADO a propósito (MANUAL o sin motivo) tampoco, ni se
     * regeneran: cuentan como existentes.
     */
    public static Plan calcularPlan(LocalDate fecha, List<Turno> existentes, List<FranjaHoraria> franjas,
                                    LocalDateTime ahora) {
        Map<LocalTime, HorarioTurno> deseados = franjas.stream()
                .flatMap(franja -> franja.turnosCalculados().stream())
                .collect(Collectors.toMap(HorarioTurno::horaInicio, Function.identity(), (a, b) -> a));
        Map<LocalTime, Turno> porHora = existentes.stream()
                .collect(Collectors.toMap(Turno::getHoraInicio, Function.identity(), (a, b) -> a));

        List<HorarioTurno> aCrear = deseados.values().stream()
                .filter(horario -> !porHora.containsKey(horario.horaInicio()))
                .filter(horario -> empiezaDespues(fecha, horario.horaInicio(), ahora))
                .sorted(Comparator.comparing(HorarioTurno::horaInicio))
                .toList();

        List<Turno> aReactivar = new ArrayList<>();
        List<Turno> aCancelar = new ArrayList<>();
        for (Turno turno : existentes) {
            if (!empiezaDespues(fecha, turno.getHoraInicio(), ahora)) {
                continue;
            }
            if (deseados.containsKey(turno.getHoraInicio())) {
                if (turno.fueCanceladoPorFranja()) {
                    aReactivar.add(turno);
                }
            } else if (turno.getEstado() == EstadoTurno.DISPONIBLE) {
                aCancelar.add(turno);
            }
        }
        return new Plan(aCrear, aReactivar, aCancelar);
    }

    /**
     * Aplica el plan de la fecha. Una fecha sin turnos no se toca: la generación a demanda ya se ocupa de ella.
     * El cupo de los turnos nuevos y de los reactivados es el vigente.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resultado reconciliarFecha(LocalDate fecha, LocalDateTime ahora) {
        List<Turno> existentes = turnoRepository.findByFechaOrderByHoraInicioAsc(fecha);
        if (existentes.isEmpty()) {
            return Resultado.SIN_CAMBIOS;
        }
        List<FranjaHoraria> franjas = franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(fecha.getDayOfWeek());
        Plan plan = calcularPlan(fecha, existentes, franjas, ahora);
        if (plan.estaVacio()) {
            return Resultado.SIN_CAMBIOS;
        }

        int cupo = configuracionGimnasioService.obtenerCupoMaximo();
        // Primero lo que puede chocar con la restricción única, antes de tomar ningún lock
        int creados = crear(fecha, plan.aCrear(), cupo);
        int reactivados = reactivar(plan.aReactivar(), cupo, fecha, ahora);

        int cancelados = 0;
        int reservasCanceladas = 0;
        for (Turno turno : ordenadosPorId(plan.aCancelar())) {
            Optional<CancelacionGimnasioResponse> cancelacion = cancelarPorOrfandad(turno, ahora);
            if (cancelacion.isPresent()) {
                cancelados++;
                reservasCanceladas += cancelacion.get().getReservasCanceladas();
            }
        }

        log.info("Turnos del {} reconciliados con las franjas: {} creados, {} reactivados, {} cancelados ({} reservas)",
                fecha, creados, reactivados, cancelados, reservasCanceladas);
        return new Resultado(creados, reactivados, cancelados, reservasCanceladas);
    }

    /**
     * Cupo nuevo para los turnos de la fecha que no empezaron y no tienen reservas activas. Los que ya tienen
     * reservas conservan su cupo. La cantidad de reservas se vuelve a contar con el turno bloqueado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int aplicarCupoEnFecha(LocalDate fecha, int cupo, LocalDateTime ahora) {
        List<Turno> candidatos = turnoRepository.findByFechaOrderByHoraInicioAsc(fecha).stream()
                .filter(turno -> turno.getEstado() == EstadoTurno.DISPONIBLE)
                .filter(turno -> turno.getCupoMaximo() != cupo)
                .filter(turno -> empiezaDespues(fecha, turno.getHoraInicio(), ahora))
                .toList();

        int actualizados = 0;
        for (Turno candidato : ordenadosPorId(candidatos)) {
            Optional<Turno> bloqueado = bloquear(candidato.getId());
            if (bloqueado.isEmpty()) {
                continue;
            }
            Turno turno = bloqueado.get();
            if (turno.getEstado() != EstadoTurno.DISPONIBLE
                    || turno.getCupoMaximo() == cupo
                    || !empiezaDespues(fecha, turno.getHoraInicio(), ahora)
                    || reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA) > 0) {
                continue;
            }
            turno.setCupoMaximo(cupo);
            turnoRepository.save(turno);
            actualizados++;
        }
        if (actualizados > 0) {
            log.info("Cupo {} aplicado a {} turnos del {} sin reservas", cupo, actualizados, fecha);
        }
        return actualizados;
    }

    private int crear(LocalDate fecha, List<HorarioTurno> horarios, int cupo) {
        if (horarios.isEmpty()) {
            return 0;
        }
        List<Turno> nuevos = horarios.stream().map(horario -> new Turno(fecha, horario, cupo)).toList();
        turnoRepository.saveAllAndFlush(nuevos);
        return nuevos.size();
    }

    private int reactivar(List<Turno> turnos, int cupo, LocalDate fecha, LocalDateTime ahora) {
        int reactivados = 0;
        for (Turno candidato : ordenadosPorId(turnos)) {
            Optional<Turno> bloqueado = bloquear(candidato.getId());
            if (bloqueado.isPresent()
                    && bloqueado.get().fueCanceladoPorFranja()
                    && empiezaDespues(fecha, bloqueado.get().getHoraInicio(), ahora)) {
                bloqueado.get().reactivar(cupo);
                turnoRepository.save(bloqueado.get());
                reactivados++;
            }
        }
        return reactivados;
    }

    /**
     * Cancela un turno que ya no tiene franja, con la cancelación del gimnasio: sin reservas solo cambia el estado
     * del turno; con reservas activas cada alumno recibe una clase extra y un aviso. Vacío si otro proceso ya lo
     * canceló o si empezó mientras tanto.
     */
    private Optional<CancelacionGimnasioResponse> cancelarPorOrfandad(Turno turno, LocalDateTime ahora) {
        // Se bloquea en el mismo orden que ReservaService: primero los alumnos, ordenados por id, y después el turno
        List<Reserva> activas = reservaRepository.findWithAlumnoByTurnoIdInAndEstado(
                List.of(turno.getId()), EstadoReserva.RESERVADA);
        activas.stream()
                .map(reserva -> reserva.getAlumno().getId())
                .distinct()
                .sorted()
                .forEach(alumnoRepository::findByIdConBloqueo);

        Optional<Turno> bloqueado = bloquear(turno.getId());
        if (bloqueado.isEmpty()
                || bloqueado.get().getEstado() == EstadoTurno.CANCELADO
                || !empiezaDespues(bloqueado.get().getFecha(), bloqueado.get().getHoraInicio(), ahora)) {
            return Optional.empty();
        }
        return Optional.of(cancelacionGimnasioService.cancelar(
                bloqueado.get(), ahora, MotivoCancelacionTurno.FRANJA_ELIMINADA));
    }

    /** Turno con la fila bloqueada y el estado releído, por si cambió desde que se cargó en esta transacción. */
    private Optional<Turno> bloquear(Long turnoId) {
        Optional<Turno> turno = turnoRepository.findByIdConBloqueo(turnoId);
        turno.ifPresent(entityManager::refresh);
        return turno;
    }

    private static List<Turno> ordenadosPorId(List<Turno> turnos) {
        return turnos.stream().sorted(Comparator.comparing(Turno::getId)).toList();
    }

    private static boolean empiezaDespues(LocalDate fecha, LocalTime horaInicio, LocalDateTime ahora) {
        return fecha.atTime(horaInicio).isAfter(ahora);
    }
}
