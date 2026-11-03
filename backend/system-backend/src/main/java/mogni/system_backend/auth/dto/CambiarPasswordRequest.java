package mogni.system_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 8, message = "La contraseña nueva debe tener al menos 8 caracteres")
    private String passwordNueva;

    /**
     * MOGNI-263: confirmación de la contraseña nueva. Opcional para no romper clientes viejos: si viene, tiene que
     * coincidir con la nueva (el frontend la manda siempre).
     */
    private String passwordConfirmacion;
}
