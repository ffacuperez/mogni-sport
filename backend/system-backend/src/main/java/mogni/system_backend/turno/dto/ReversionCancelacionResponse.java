package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resultado de reactivar un turno que el propietario había cancelado. */
@Getter
@AllArgsConstructor
public class ReversionCancelacionResponse {
    private Long turnoId;
    /** Alumnos que tenían una reserva cancelada en el turno y recibieron el aviso. */
    private int alumnosAvisados;
}
