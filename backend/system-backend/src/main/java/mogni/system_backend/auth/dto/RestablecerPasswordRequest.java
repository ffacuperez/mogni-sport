package mogni.system_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recuperar contraseña, paso 2: el token del link del mail y la contraseña nueva. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerPasswordRequest {

    @NotBlank(message = "El link para restablecer la contraseña no es válido")
    private String token;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 8, message = "La contraseña nueva debe tener al menos 8 caracteres")
    private String passwordNueva;

    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    private String passwordConfirmacion;
}
