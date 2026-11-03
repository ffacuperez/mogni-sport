package mogni.system_backend.progreso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroSesionRequest {

    @NotNull(message = "La semana es obligatoria")
    @Min(value = 1, message = "La semana debe ser mayor o igual a 1")
    private Integer semana;

    @NotNull(message = "La sesión es obligatoria")
    @Min(value = 1, message = "La sesión debe ser mayor o igual a 1")
    private Integer numeroSesion;
}
