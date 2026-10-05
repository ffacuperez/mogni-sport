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
}
