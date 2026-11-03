package mogni.system_backend.plan.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/** Datos de un plan armado a mano: las mismas reglas que una plantilla (al menos una sesión con un ejercicio). */
@Getter
@Setter
@NoArgsConstructor
public class PlanRequest {

    // El @Pattern rechaza nombres que se ven vacíos pero que @NotBlank deja pasar, como los espacios duros
    @NotBlank(message = "El nombre del plan es obligatorio")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El nombre del plan es obligatorio")
    @Size(max = 100, message = "El nombre del plan no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El nivel es obligatorio")
    private NivelPlantilla nivel;

    @NotNull(message = "El objetivo es obligatorio")
    private ObjetivoPlantilla objetivo;

    @NotEmpty(message = "El plan tiene que tener al menos una sesión")
    private List<@Valid @NotNull(message = "Cada sesión tiene que tener al menos un ejercicio") SesionRequest> sesiones;

    /**
     * HU-266: vigencia del plan en semanas; la fecha fin se calcula. Al modificar, que no sea menor a la semana en
     * curso lo valida el servicio, que sabe qué día es hoy.
     */
    @NotNull(message = "La cantidad de semanas es obligatoria")
    @Min(value = 1, message = "El plan tiene que durar al menos una semana")
    @Max(value = PlanAsignado.MAXIMO_SEMANAS, message = "El plan no puede durar más de 52 semanas")
    private Integer cantidadSemanas;
}
