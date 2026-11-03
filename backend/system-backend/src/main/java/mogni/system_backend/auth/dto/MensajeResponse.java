package mogni.system_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Respuesta con un único mensaje para mostrar al usuario. */
@Getter
@AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
}
