package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resultado de reactivar un turno que el propietario había cancelado. */
@Getter
@AllArgsConstructor
public class ReversionCancelacionResponse {
    private Long turnoId;
}
