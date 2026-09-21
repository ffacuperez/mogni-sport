package mogni.system_backend.membresia.dto;

import java.math.BigDecimal;

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

    public static final int DURACION_POR_DEFECTO = 30;

    @NotBlank(message = "El nombre de la membresía es obligatorio")
    private String nombre;

    @NotNull(message = "Los días por semana son obligatorios")
    @Min(value = 1, message = "Los días por semana deben ser al menos 1")
    @Max(value = 7, message = "Los días por semana no pueden ser más de 7")
    private Integer diasPorSemana;

    private String descripcion;

    /** Opcional: si no se envía, se usa la vigencia actual (o {@value #DURACION_POR_DEFECTO} días en un alta). */
    @Min(value = 1, message = "La duración debe ser de al menos 1 día")
    private Integer duracionDias;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
}
