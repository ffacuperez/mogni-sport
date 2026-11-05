package mogni.system_backend.progreso.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** HU-269: qué sesión del plan, y de qué semana, va a hacer el alumno. */
@Getter
@Setter
@NoArgsConstructor
public class IniciarSesionRequest {

    @NotNull(message = "La semana es obligatoria")
    private Integer semana;

    @NotNull(message = "La sesión es obligatoria")
    private Integer numeroSesion;
}
