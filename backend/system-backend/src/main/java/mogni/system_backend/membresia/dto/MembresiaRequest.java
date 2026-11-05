package mogni.system_backend.membresia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembresiaRequest {

    @NotBlank(message = "El nombre de la membresía es obligatorio")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private Double precioActual;

    @NotNull(message = "Los días por semana son obligatorios")
    @Min(value = 3, message = "Los días por semana deben ser al menos 3")
    @Max(value = 5, message = "Los días por semana no pueden ser más de 5")
    private Integer frecuenciaSemanal;

    private String descripcion;
}
