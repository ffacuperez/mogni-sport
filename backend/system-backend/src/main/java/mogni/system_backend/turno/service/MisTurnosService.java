package mogni.system_backend.turno.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.turno.dto.MiTurnoResponse;
import mogni.system_backend.turno.dto.MisTurnosResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-256: turnos reservados del alumno logueado que todavía no pasaron. El alumno sale siempre de la sesión, así
 * que nunca puede ver los turnos de otro. Solo se listan las reservas RESERVADA: las que canceló el alumno
 * (CANCELADA) y las que canceló el gimnasio (CANCELADA_POR_GIMNASIO) no aparecen.
 */
@Service
public class MisTurnosService {

    private final AlumnoRepository alumnoRepository;
    private final ReservaRepository reservaRepository;

    public MisTurnosService(AlumnoRepository alumnoRepository, ReservaRepository reservaRepository) {
        this.alumnoRepository = alumnoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public MisTurnosResponse consultarMisTurnos(String email) {
        return consultarMisTurnos(email, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    MisTurnosResponse consultarMisTurnos(String email, LocalDateTime ahora) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        ConsultaTurnosService.validarAcceso(alumno);

        List<MiTurnoResponse> turnos = reservaRepository
                .findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(
                        alumno.getId(), EstadoReserva.RESERVADA, ahora.toLocalDate())
                .stream()
                .filter(reserva -> !yaTermino(reserva.getTurno(), ahora))
                .sorted(Comparator.comparing((Reserva r) -> r.getTurno().getFecha())
                        .thenComparing(r -> r.getTurno().getHoraInicio()))
                .map(reserva -> mapear(reserva, ahora))
                .toList();
        return new MisTurnosResponse(ahora.toLocalDate(), turnos);
    }

    /** Un turno de hoy se muestra mientras no terminó: cuando llega su hora de fin ya no. */
    private static boolean yaTermino(Turno turno, LocalDateTime ahora) {
        return turno.getFecha().atTime(turno.getHoraFin()).compareTo(ahora) <= 0;
    }

    private static MiTurnoResponse mapear(Reserva reserva, LocalDateTime ahora) {
        Turno turno = reserva.getTurno();
        return new MiTurnoResponse(
                reserva.getId(),
                turno.getId(),
                turno.getFecha(),
                turno.getFecha().getDayOfWeek(),
                FranjaHorariaService.formatearHora(turno.getHoraInicio()),
                FranjaHorariaService.formatearHora(turno.getHoraFin()),
                CancelacionService.sePuedeCancelar(turno, ahora));
    }
}
