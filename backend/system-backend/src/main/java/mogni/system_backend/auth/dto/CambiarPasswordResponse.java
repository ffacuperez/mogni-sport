package mogni.system_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CambiarPasswordResponse {
    private String mensaje;
    /** Token nuevo, ya sin la marca de cambio de contraseña pendiente. */
    private String token;
}
