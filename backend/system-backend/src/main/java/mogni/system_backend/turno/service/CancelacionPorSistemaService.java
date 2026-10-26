package mogni.system_backend.turno.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * El sistema cancela las reservas de un alumno que entró en mora o fue dado de baja (rediseño de cuotas, secciones 10
 * y 11). Quedan CANCELADA_POR_SISTEMA, así dejan de ocupar lugar; no dan clase extra y no se aplica la regla de las 2
 * horas (no las cancela el alumno). Se usa dentro de la transacción de quien la llama.
 */
@Service
public class CancelacionPorSistemaService {

    private final ReservaRepository reservaRepository;
    private final ClaseExtraRepository claseExtraRepository;

    public CancelacionPorSistemaService(ReservaRepository reservaRepository,
                                        ClaseExtraRepository claseExtraRepository) {
        this.reservaRepository = reservaRepository;
        this.claseExtraRepository = claseExtraRepository;
    }

    /**
     * Cancela las reservas del alumno en turnos que todavía no empezaron (solo existen de hoy y mañana); las de turnos
     * que ya empezaron o pasaron no se tocan, porque el alumno pudo haber ido. Si una reserva se hizo con una clase
     * extra, el crédito se le devuelve (igual que cuando cancela el alumno). Devuelve cuántas canceló.
     */
    public int cancelarReservasFuturas(Long alumnoId, LocalDateTime ahora) {
        List<Reserva> futuras = reservaRepository.findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(
                        alumnoId, EstadoReserva.RESERVADA, ahora.toLocalDate()).stream()
                .filter(reserva -> reserva.getTurno().getFecha().atTime(reserva.getTurno().getHoraInicio()).isAfter(ahora))
                .toList();
        for (Reserva reserva : futuras) {
            reserva.setEstado(EstadoReserva.CANCELADA_POR_SISTEMA);
            reservaRepository.save(reserva);
            claseExtraRepository.findByReservaUsoId(reserva.getId()).ifPresent(claseExtra -> {
                claseExtra.setReservaUsoId(null);
                claseExtraRepository.save(claseExtra);
            });
        }
        return futuras.size();
    }
}
