package mogni.system_backend.ejercicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EjercicioRequest {

    @NotBlank(message = "El nombre del ejercicio es obligatorio")
    private String nombre;

    @NotBlank(message = "El grupo muscular es obligatorio")
    private String grupoMuscular;

    private String descripcion;

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo;
}