package mogni.system_backend.ejercicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EjercicioRequest {

    @NotBlank(message = "El nombre del ejercicio es obligatorio")
    @Size(max = 100, message = "El nombre del ejercicio puede tener hasta 100 caracteres")
    private String nombre;

    @NotBlank(message = "El grupo muscular es obligatorio")
    @Size(max = 100, message = "El grupo muscular puede tener hasta 100 caracteres")
    private String grupoMuscular;

    @Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres")
    private String descripcion;

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo;
}