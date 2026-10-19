package mogni.system_backend.turno.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.notificacion.entity.AvisoTurnoReactivado;
import mogni.system_backend.notificacion.repository.AvisoTurnoReactivadoRepository;
import mogni.system_backend.turno.dto.ReversionCancelacionResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * El propietario se arrepiente de haber cancelado un turno. El turno vuelve a DISPONIBLE (con el cupo vigente) y los
 * alumnos que tenían una reserva cancelada reciben un aviso. Las reservas NO se restauran: conservan su clase extra
 * y tienen que reservar de nuevo. Los turnos cancelados por cambio de franjas no se pueden reactivar a mano: los
 * maneja ReconciliacionTurnosService.
 *
 * Locks en el mismo orden que ReservaService (alumnos por id y después el turno), porque los avisos tienen una FK
 * al alumno y una reserva en curso de ese alumno podría trabarse con esta transacción.
 */
@Service
public class ReversionCancelacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");

    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;
    private final AlumnoRepository alumnoRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ConfiguracionGimnasioService configuracionGimnasioService;
    private final AvisoTurnoReactivadoRepository avisoRepository;
    private final EntityManager entityManager;

    public ReversionCancelacionService(TurnoRepository turnoRepository,
                                       ReservaRepository reservaRepository,
                                       AlumnoRepository alumnoRepository,
                                       FranjaHorariaRepository franjaHorariaRepository,
                                       ConfiguracionGimnasioService configuracionGimnasioService,
                                       AvisoTurnoReactivadoRepository avisoRepository,
                                       EntityManager entityManager) {
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
        this.alumnoRepository = alumnoRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.configuracionGimnasioService = configuracionGimnasioService;
        this.avisoRepository = avisoRepository;
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

        List<Reserva> afectadas = reservaRepository.findWithAlumnoByTurnoIdInAndEstado(
                List.of(turnoId), EstadoReserva.CANCELADA_POR_GIMNASIO);
        afectadas.stream()
                .map(reserva -> reserva.getAlumno().getId())
                .distinct()
                .sorted()
                .forEach(alumnoRepository::findByIdConBloqueo);

        Turno turno = turnoRepository.findByIdConBloqueo(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el turno que querés reactivar."));
        // Se relee el estado: otra reactivación o la reconciliación pudo cambiarlo antes de conseguir el lock
        entityManager.refresh(turno);
        validar(turno, ahora);

        turno.reactivar(configuracionGimnasioService.obtenerCupoMaximo());
        turnoRepository.save(turno);

        String mensaje = "El turno del " + turno.getFecha().format(FECHA) + " a las "
                + FranjaHorariaService.formatearHora(turno.getHoraInicio())
                + " volvió a estar disponible. Si querés, podés reservarlo de nuevo.";
        for (Reserva reserva : afectadas) {
            avisoRepository.save(new AvisoTurnoReactivado(
                    reserva.getAlumno(), reserva.getId(), turno.getId(), ahora.toLocalDate(), turno.getFecha(),
                    "Turno disponible nuevamente", mensaje));
        }
        return new ReversionCancelacionResponse(turno.getId(), afectadas.size());
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
