package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.turno.entity.EstadoReserva;

/** HU-255: la reserva ya cancelada. */
@Getter
@AllArgsConstructor
public class CancelacionResponse {
    private Long reservaId;
    private Long turnoId;
    private LocalDate fecha;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    private EstadoReserva estado;
}
