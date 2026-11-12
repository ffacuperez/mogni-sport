package mogni.system_backend.turno.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.MotivoNoReserva;
import mogni.system_backend.turno.dto.ReservaResponse;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.repository.ClaseExtraRepository;

/**
 * HU-254: reservar un turno. Las reglas se validan acá aunque la pantalla ya las muestre (el frontend solo dibuja).
 * Para que sea seguro ante concurrencia se bloquean, en este orden, la fila del alumno (sus reglas por día y por
 * semana) y la fila del turno (el cupo). Siempre el mismo orden, así dos reservas nunca se traban entre sí.
 */
@Service
public class ReservaService {

    /** Día de gracia: puede reservar los turnos de hoy, pero no los de mañana, cuando ya va a estar en mora. */
    static final String MENSAJE_CUOTA_VENCIDA =
            "Tu cuota venció y hoy es el último día para pagarla: para reservar turnos de mañana, regularizá el pago.";

    /** MOGNI-254: anticipación mínima para reservar un turno. */
    static final Duration ANTICIPACION_MINIMA = Duration.ofHours(1);

    private final AlumnoRepository alumnoRepository;
    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;
    private final ClaseExtraRepository claseExtraRepository;

    public ReservaService(AlumnoRepository alumnoRepository,
                          TurnoRepository turnoRepository,
                          ReservaRepository reservaRepository,
                          ClaseExtraRepository claseExtraRepository) {
        this.alumnoRepository = alumnoRepository;
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
        this.claseExtraRepository = claseExtraRepository;
    }

    @Transactional
    public ReservaResponse reservar(String email, Long turnoId) {
        return reservar(email, turnoId, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    ReservaResponse reservar(String email, Long turnoId, LocalDateTime ahora) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        ConsultaTurnosService.validarAcceso(alumno);
        if (ReglasReserva.estaEnMora(alumno, ahora.toLocalDate())) {
            throw new AccesoDenegadoException("Tenés la cuota en mora: regularizá el pago para reservar turnos.");
        }

        alumnoRepository.findByIdConBloqueo(alumno.getId());
        Turno turno = turnoRepository.findByIdConBloqueo(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el turno que querés reservar."));

        validarTurno(turno, ahora);
        if (ReglasReserva.requierePagoPara(alumno, ahora.toLocalDate(), turno.getFecha())) {
            throw new AccesoDenegadoException(MENSAJE_CUOTA_VENCIDA);
        }
        ClaseExtra claseExtra = validarAlumno(alumno, turno);
        validarCupo(turno);

        Reserva reserva = reservaRepository.save(new Reserva(null, alumno, turno, ahora, EstadoReserva.RESERVADA));
        if (claseExtra != null) {
            claseExtra.setReservaUsoId(reserva.getId());
            claseExtraRepository.save(claseExtra);
        }
        long reservasEnLaSemana = ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), turno.getFecha()).size();
        int restantes = Math.max(0, ReglasReserva.frecuenciaSemanal(alumno) - (int) reservasEnLaSemana);
        return new ReservaResponse(
                reserva.getId(),
                turno.getId(),
                turno.getFecha(),
                FranjaHorariaService.formatearHora(turno.getHoraInicio()),
                FranjaHorariaService.formatearHora(turno.getHoraFin()),
                reserva.getEstado(),
                restantes);
    }

    /** Fecha (hoy o mañana), que el gimnasio no lo haya cancelado, que no haya empezado y que falte al menos 1 hora. */
    private void validarTurno(Turno turno, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        if (!turno.getFecha().equals(hoy) && !turno.getFecha().equals(hoy.plusDays(1))) {
            throw new RegistroInvalidoException("Solo podés reservar turnos de hoy y mañana.");
        }
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ConflictoException("Este turno fue cancelado por el gimnasio.");
        }
        if (turno.getFecha().equals(hoy) && !turno.getHoraInicio().isAfter(ahora.toLocalTime())) {
            throw new RegistroInvalidoException("Este turno ya empezó, elegí uno más tarde.");
        }
        if (faltaMenosDeUnaHora(turno, ahora)) {
            throw new RegistroInvalidoException(
                    "No podés reservar este turno porque falta menos de 1 hora para su inicio.");
        }
    }

    /**
     * MOGNI-254: se reserva hasta 1 hora antes del inicio (con exactamente 60 minutos todavía se puede). Compara fecha
     * y hora completas: a las 23:45, el turno de mañana a las 00:30 también queda afuera.
     */
    static boolean faltaMenosDeUnaHora(Turno turno, LocalDateTime ahora) {
        LocalDateTime limite = LocalDateTime.of(turno.getFecha(), turno.getHoraInicio()).minus(ANTICIPACION_MINIMA);
        return ahora.isAfter(limite);
    }

    /**
     * Mismo turno y las reglas del alumno de ReglasReserva (un turno por día, membresía activa y tope semanal). Con la
     * semana completa usa una clase extra, bloqueada: la devuelve para marcarla como usada; si no hace falta, null.
     */
    private ClaseExtra validarAlumno(Alumno alumno, Turno turno) {
        List<Reserva> deLaSemana = ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), turno.getFecha());
        if (deLaSemana.stream().anyMatch(r -> ReglasReserva.estaActiva(r) && r.getTurno().getId().equals(turno.getId()))) {
            throw new ConflictoException("Ya reservaste este turno.");
        }

        // Solo se consulta (y se bloquea) si la semana ya está completa
        List<ClaseExtra> clasesExtra = new ArrayList<>();
        MotivoNoReserva motivo = ReglasReserva.motivoParaElDia(alumno, turno.getFecha(), deLaSemana, () -> {
            clasesExtra.addAll(claseExtraRepository.buscarDisponiblesConBloqueo(alumno.getId(), turno.getFecha()));
            return !clasesExtra.isEmpty();
        }).orElse(null);
        if (motivo != null) {
            throw switch (motivo) {
                case SIN_DIAS_DISPONIBLES -> new ConflictoException("Ya tenés un turno reservado este día.");
                case SIN_MEMBRESIA_ACTIVA -> new AccesoDenegadoException("No tenés una membresía activa para reservar turnos.");
                case CUPO_SEMANAL_AGOTADO -> new ConflictoException("Ya usaste todos los turnos de tu membresía en esta semana.");
                case EN_MORA -> new AccesoDenegadoException("Tenés la cuota en mora: regularizá el pago para reservar turnos.");
                case CUOTA_VENCIDA -> new AccesoDenegadoException(MENSAJE_CUOTA_VENCIDA);
            };
        }
        return clasesExtra.isEmpty() ? null : clasesExtra.get(0);
    }

    /** Las reservas canceladas no ocupan lugar. Se cuenta con el turno ya bloqueado. */
    private void validarCupo(Turno turno) {
        long ocupados = reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA);
        if (ocupados >= turno.getCupoMaximo()) {
            throw new ConflictoException("Este turno está completo.");
        }
    }
}
