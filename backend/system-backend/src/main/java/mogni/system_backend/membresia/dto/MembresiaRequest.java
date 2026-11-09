package mogni.system_backend.membresia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import mogni.system_backend.membresia.entity.Membresia;
import lombok.Setter;

@Getter
@Setter
public class MembresiaRequest {

    @NotBlank(message = "El nombre de la membresía es obligatorio")
    @Size(max = 100, message = "El nombre de la membresía puede tener hasta 100 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private Double precioActual;

    @NotNull(message = "Los días por semana son obligatorios")
    @Min(value = Membresia.DIAS_POR_SEMANA_MIN, message = "Los días por semana deben ser al menos 2")
    @Max(value = Membresia.DIAS_POR_SEMANA_MAX, message = "Los días por semana no pueden ser más de 6")
    private Integer frecuenciaSemanal;

    @Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres")
    private String descripcion;
}
