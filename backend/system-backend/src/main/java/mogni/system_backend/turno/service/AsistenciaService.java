package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.AsistenciaResponse;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-262: el entrenador (o el propietario) registra desde la agenda si cada alumno con reserva estuvo presente o
 * ausente. Reglas:
 * <ul>
 *   <li>solo reservas RESERVADA de turnos no cancelados que ya comenzaron;</li>
 *   <li>la marca se puede corregir hasta el final del día del turno;</li>
 *   <li>la reserva no cambia de estado: AUSENTE sigue contando para el límite semanal y no da clase extra;</li>
 *   <li>al terminar el día, las reservas sin marcar pasan a AUSENTE ({@link #cerrarDiasAnteriores()}).</li>
 * </ul>
 */
@Service
public class AsistenciaService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);

    private final ReservaRepository reservaRepository;

    public AsistenciaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public AsistenciaResponse registrar(Long reservaId, Asistencia asistencia) {
        return registrar(reservaId, asistencia, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    AsistenciaResponse registrar(Long reservaId, Asistencia asistencia, LocalDateTime ahora) {
        if (asistencia == null) {
            throw new RegistroInvalidoException("Indicá si el alumno estuvo presente o ausente.");
        }
        // Con bloqueo: dos marcas a la vez sobre la misma reserva (doble clic, dos dispositivos) quedan en orden
        Reserva reserva = reservaRepository.findByIdConBloqueo(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva."));
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new ConflictoException("Esta reserva fue cancelada: no se puede registrar su asistencia.");
        }
        Turno turno = reserva.getTurno();
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ConflictoException("Este turno fue cancelado: no se puede registrar asistencia.");
        }
        if (!comenzo(turno, ahora)) {
            throw new ConflictoException("El turno todavía no comenzó. Vas a poder registrar la asistencia desde las "
                    + FranjaHorariaService.formatearHora(turno.getHoraInicio()) + ".");
        }
        if (ahora.toLocalDate().isAfter(turno.getFecha())) {
            throw new ConflictoException(
                    "La asistencia solo se puede registrar o corregir hasta el final del día del turno.");
        }

        reserva.setAsistencia(asistencia);
        reservaRepository.save(reserva);

        Conteo conteo = Conteo.de(
                reservaRepository.findByTurnoIdInAndEstado(List.of(turno.getId()), EstadoReserva.RESERVADA),
                ahora.toLocalDate());
        return new AsistenciaResponse(reserva.getId(), turno.getId(), asistencia,
                conteo.presentes(), conteo.ausentes(), conteo.sinMarcar());
    }

    /**
     * Al terminar el día, las reservas que quedaron sin marcar pasan a AUSENTE. Corre a las 00:05 y, como el back puede
     * estar apagado a esa hora (un reinicio o un deploy), también antes de consultar la agenda (igual que la puesta al día de
     * cuotas). Es idempotente: solo toca reservas sin marcar de días anteriores a hoy.
     */
    @Transactional
    public int cerrarDiasAnteriores() {
        return cerrarDiasAnteriores(ZonaHoraria.hoy());
    }

    /** Cron diario (hora de Argentina), apenas termina el día. */
    @Scheduled(cron = "0 5 0 * * *", zone = ZonaHoraria.ZONA)
    @Transactional
    public void cierreDiario() {
        cerrarDiasAnteriores(ZonaHoraria.hoy());
    }

    /** Igual que el anterior, con "hoy" como parámetro para poder probarlo con cualquier fecha. */
    int cerrarDiasAnteriores(LocalDate hoy) {
        int cerradas = reservaRepository.registrarAsistenciaSinMarcarAntesDe(
                hoy, EstadoReserva.RESERVADA, Asistencia.AUSENTE);
        if (cerradas > 0) {
            log.info("Cierre de asistencia: {} reservas sin marcar pasaron a AUSENTE", cerradas);
        }
        return cerradas;
    }

    /** El turno ya empezó: desde su hora de inicio (inclusive). */
    public static boolean comenzo(Turno turno, LocalDateTime ahora) {
        return !ahora.isBefore(turno.getFecha().atTime(turno.getHoraInicio()));
    }

    /** Se puede marcar o corregir: turno generado, no cancelado, ya comenzado y todavía es su día. */
    public static boolean sePuedeRegistrar(Turno turno, LocalDateTime ahora) {
        return turno.getId() != null
                && turno.getEstado() != EstadoTurno.CANCELADO
                && comenzo(turno, ahora)
                && !ahora.toLocalDate().isAfter(turno.getFecha());
    }

    /**
     * La asistencia que se muestra: la registrada o, si el día del turno ya terminó y no se marcó, AUSENTE (por si
     * el cierre todavía no corrió). Null = sin marcar.
     */
    public static Asistencia asistenciaEfectiva(Reserva reserva, LocalDate hoy) {
        if (reserva.getAsistencia() != null) {
            return reserva.getAsistencia();
        }
        return reserva.getTurno().getFecha().isBefore(hoy) ? Asistencia.AUSENTE : null;
    }

    /** Presentes, ausentes y sin marcar de un grupo de reservas RESERVADA (las de un turno). */
    public record Conteo(int presentes, int ausentes, int sinMarcar) {

        public static Conteo de(Collection<Reserva> reservas, LocalDate hoy) {
            int presentes = 0;
            int ausentes = 0;
            for (Reserva reserva : reservas) {
                Asistencia asistencia = asistenciaEfectiva(reserva, hoy);
                if (asistencia == Asistencia.PRESENTE) {
                    presentes++;
                } else if (asistencia == Asistencia.AUSENTE) {
                    ausentes++;
                }
            }
            return new Conteo(presentes, ausentes, reservas.size() - presentes - ausentes);
        }
    }
}
