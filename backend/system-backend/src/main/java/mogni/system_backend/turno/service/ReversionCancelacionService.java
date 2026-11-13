package mogni.system_backend.turno.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.turno.dto.ReversionCancelacionResponse;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * El propietario se arrepiente de haber cancelado un turno. El turno vuelve a DISPONIBLE (con el cupo vigente). Las
 * reservas NO se restauran ni se avisa a los alumnos: ya recibieron el aviso de la cancelación, conservan su clase a
 * recuperar y, si quieren, lo reservan de nuevo. Los turnos cancelados por cambio de franjas no se pueden reactivar a
 * mano: los maneja ReconciliacionTurnosService.
 */
@Service
public class ReversionCancelacionService {

    private final TurnoRepository turnoRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ConfiguracionGimnasioService configuracionGimnasioService;
    private final EntityManager entityManager;

    public ReversionCancelacionService(TurnoRepository turnoRepository,
                                       FranjaHorariaRepository franjaHorariaRepository,
                                       ConfiguracionGimnasioService configuracionGimnasioService,
                                       EntityManager entityManager) {
        this.turnoRepository = turnoRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.configuracionGimnasioService = configuracionGimnasioService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ReversionCancelacionResponse revertir(Long turnoId) {
        return revertir(turnoId, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    ReversionCancelacionResponse revertir(Long turnoId, LocalDateTime ahora) {
        if (!turnoRepository.existsById(turnoId)) {
            throw new RecursoNoEncontradoException("No existe el turno que querés reactivar.");
        }

        Turno turno = turnoRepository.findByIdConBloqueo(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el turno que querés reactivar."));
        // Se relee el estado: otra reactivación o la reconciliación pudo cambiarlo antes de conseguir el lock
        entityManager.refresh(turno);
        validar(turno, ahora);

        turno.reactivar(configuracionGimnasioService.obtenerCupoMaximo());
        turnoRepository.save(turno);
        return new ReversionCancelacionResponse(turno.getId());
    }

    private void validar(Turno turno, LocalDateTime ahora) {
        if (turno.getEstado() != EstadoTurno.CANCELADO) {
            throw new ConflictoException("Este turno no está cancelado.");
        }
        if (turno.fueCanceladoPorFranja()) {
            throw new ConflictoException("Este turno se canceló porque su horario ya no está en la agenda. "
                    + "Si volvés a agregar la franja, se reactiva solo.");
        }
        if (!turno.getFecha().atTime(turno.getHoraInicio()).isAfter(ahora)) {
            throw new ConflictoException("Este turno ya empezó o ya pasó y no se puede reactivar.");
        }
        // Sin franja que lo respalde, la reconciliación lo volvería a cancelar enseguida
        List<FranjaHoraria> franjas = franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(
                turno.getFecha().getDayOfWeek());
        boolean conFranja = franjas.stream().flatMap(franja -> franja.turnosCalculados().stream())
                .anyMatch(horario -> horario.horaInicio().equals(turno.getHoraInicio()));
        if (!conFranja) {
            throw new ConflictoException("No hay una franja horaria que incluya este turno. "
                    + "Agregala en Horarios antes de reactivarlo.");
        }
    }
}
