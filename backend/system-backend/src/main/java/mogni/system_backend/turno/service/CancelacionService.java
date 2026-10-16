package mogni.system_backend.turno.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.turno.dto.CancelacionResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-255: el alumno cancela una reserva suya. La reserva no se borra: pasa a CANCELADA, así deja de ocupar lugar
 * en el turno (queda libre para otro alumno) y deja de contar para el límite semanal, sin tocar esa lógica.
 *
 * <p>Pendiente: devolver la clase extra si la reserva se hizo con una. Todavía no existe el concepto de clase extra
 * en el código (ni cómo se otorga ni cómo se marca una reserva que la usó).
 */
@Service
public class CancelacionService {

    /** Solo se puede cancelar hasta estas horas antes de la hora de inicio del turno. */
    public static final int HORAS_ANTICIPACION = 2;

    private final AlumnoRepository alumnoRepository;
    private final ReservaRepository reservaRepository;

    public CancelacionService(AlumnoRepository alumnoRepository, ReservaRepository reservaRepository) {
        this.alumnoRepository = alumnoRepository;
        this.reservaRepository = reservaRepository;
    }

    /** true mientras falten 2 horas o más para el inicio (justo 2 horas antes todavía se puede). */
    public static boolean sePuedeCancelar(Turno turno, LocalDateTime ahora) {
        LocalDateTime limite = turno.getFecha().atTime(turno.getHoraInicio()).minusHours(HORAS_ANTICIPACION);
        return !ahora.isAfter(limite);
    }

    @Transactional
    public CancelacionResponse cancelar(String email, Long reservaId) {
        return cancelar(email, reservaId, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    CancelacionResponse cancelar(String email, Long reservaId, LocalDateTime ahora) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        ConsultaTurnosService.validarAcceso(alumno);

        Reserva reserva = reservaRepository.findByIdConBloqueo(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva que querés cancelar."));
        if (!reserva.getAlumno().getId().equals(alumno.getId())) {
            throw new AccesoDenegadoException("Solo podés cancelar tus propios turnos.");
        }
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new ConflictoException("Esta reserva ya fue cancelada.");
        }

        Turno turno = reserva.getTurno();
        if (!ahora.isBefore(turno.getFecha().atTime(turno.getHoraInicio()))) {
            throw new ConflictoException("Este turno ya empezó o ya pasó, no se puede cancelar.");
        }
        if (!sePuedeCancelar(turno, ahora)) {
            throw new ConflictoException(
                    "Solo podés cancelar hasta " + HORAS_ANTICIPACION + " horas antes del turno.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        return new CancelacionResponse(
                reserva.getId(),
                turno.getId(),
                turno.getFecha(),
                FranjaHorariaService.formatearHora(turno.getHoraInicio()),
                FranjaHorariaService.formatearHora(turno.getHoraFin()),
                reserva.getEstado());
    }
}
