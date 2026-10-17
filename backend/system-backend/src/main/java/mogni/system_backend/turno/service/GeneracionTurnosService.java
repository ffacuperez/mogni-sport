package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

    public GeneracionTurnosService(TurnoRepository turnoRepository,
                                   FranjaHorariaRepository franjaHorariaRepository,
                                   ConfiguracionGimnasioService configuracionGimnasioService,
                                   InsercionTurnosService insercionTurnosService) {
        this.turnoRepository = turnoRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.configuracionGimnasioService = configuracionGimnasioService;
        this.insercionTurnosService = insercionTurnosService;
    }

    /**
     * Turnos de la fecha, ordenados por hora de inicio. Si la fecha ya tiene al menos un turno (en cualquier
     * estado, incluso cancelado) se devuelven tal cual: no se crea ni se modifica nada. Si no tiene ninguno,
     * se generan todos los del día (incluso los de horas que ya pasaron) a partir de las franjas de ese día
     * de la semana, con el cupo vigente. Un día sin franjas no genera nada.
     */
    public List<Turno> obtenerOGenerarTurnos(LocalDate fecha) {
        List<Turno> existentes = turnoRepository.findByFechaOrderByHoraInicioAsc(fecha);
        if (!existentes.isEmpty()) {
            return existentes;
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
