package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * Generación a demanda de los turnos de una fecha (HU-259). No hay tarea programada: se generan cuando alguien
 * consulta. Lo usa la consulta del alumno y lo va a usar la HU-258 (cancelar turnos con anticipación).
 */
@Service
public class GeneracionTurnosService {

    private static final Logger log = LoggerFactory.getLogger(GeneracionTurnosService.class);

    private final TurnoRepository turnoRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ConfiguracionGimnasioService configuracionGimnasioService;
    private final InsercionTurnosService insercionTurnosService;
    private final ReconciliacionTurnosService reconciliacionTurnosService;
    private final EntityManager entityManager;

    public GeneracionTurnosService(TurnoRepository turnoRepository,
                                   FranjaHorariaRepository franjaHorariaRepository,
                                   ConfiguracionGimnasioService configuracionGimnasioService,
                                   InsercionTurnosService insercionTurnosService,
                                   ReconciliacionTurnosService reconciliacionTurnosService,
                                   EntityManager entityManager) {
        this.turnoRepository = turnoRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.configuracionGimnasioService = configuracionGimnasioService;
        this.insercionTurnosService = insercionTurnosService;
        this.reconciliacionTurnosService = reconciliacionTurnosService;
        this.entityManager = entityManager;
    }

    /**
     * Turnos de la fecha, ordenados por hora de inicio. Si la fecha no tiene ninguno, se generan todos los del día
     * (incluso los de horas que ya pasaron) a partir de las franjas de ese día de la semana, con el cupo vigente.
     * Un día sin franjas no genera nada. Si la fecha ya tiene turnos (en cualquier estado, incluso cancelado) se
     * devuelven, y si es de hoy en adelante y no coinciden con las franjas vigentes se reconcilian primero
     * (ver {@link ReconciliacionTurnosService}).
     */
    public List<Turno> obtenerOGenerarTurnos(LocalDate fecha) {
        return obtenerOGenerarTurnos(fecha, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    public List<Turno> obtenerOGenerarTurnos(LocalDate fecha, LocalDateTime ahora) {
        List<Turno> existentes = turnoRepository.findByFechaOrderByHoraInicioAsc(fecha);
        if (!existentes.isEmpty()) {
            return reconciliarSiHaceFalta(fecha, existentes, ahora);
        }

        List<Turno> nuevos = calcularTurnos(fecha);
        if (nuevos.isEmpty()) {
            return List.of();
        }

        try {
            return insercionTurnosService.insertar(nuevos).stream()
                    .sorted(Comparator.comparing(Turno::getHoraInicio))
                    .toList();
        } catch (DataIntegrityViolationException e) {
            // Otra consulta generó la misma fecha al mismo tiempo: se usan los turnos que creó ella
            log.info("Los turnos del {} ya los generó otra consulta; se vuelven a leer", fecha);
            return turnoRepository.findByFechaOrderByHoraInicioAsc(fecha);
        }
    }

    /**
     * Red de seguridad: si la reconciliación al editar la agenda falló o no corrió, acá se detecta la diferencia entre
     * los turnos de la fecha y las franjas vigentes y se corrige. Si todo coincide (lo normal) no abre ninguna
     * transacción. Un error no rompe la consulta: se devuelven los turnos que había y se reintenta la próxima vez.
     */
    private List<Turno> reconciliarSiHaceFalta(LocalDate fecha, List<Turno> existentes, LocalDateTime ahora) {
        if (fecha.isBefore(ahora.toLocalDate())) {
            return existentes;
        }
        List<FranjaHoraria> franjas = franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(fecha.getDayOfWeek());
        if (ReconciliacionTurnosService.calcularPlan(fecha, existentes, franjas, ahora).estaVacio()) {
            return existentes;
        }
        try {
            reconciliacionTurnosService.reconciliarFecha(fecha, ahora);
        } catch (RuntimeException e) {
            log.warn("No se pudieron reconciliar los turnos del {} con las franjas; se devuelven los existentes", fecha, e);
            return existentes;
        }
        // La reconciliación corrió en otra transacción: se sueltan estas copias para releer el estado confirmado
        existentes.forEach(entityManager::detach);
        return turnoRepository.findByFechaOrderByHoraInicioAsc(fecha);
    }

    private List<Turno> calcularTurnos(LocalDate fecha) {
        List<FranjaHoraria> franjas = franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(fecha.getDayOfWeek());
        if (franjas.isEmpty()) {
            return List.of();
        }
        int cupo = configuracionGimnasioService.obtenerCupoMaximo();
        return franjas.stream()
                .flatMap(franja -> franja.turnosCalculados().stream())
                .map(horario -> new Turno(fecha, horario, cupo))
                .toList();
    }
}
