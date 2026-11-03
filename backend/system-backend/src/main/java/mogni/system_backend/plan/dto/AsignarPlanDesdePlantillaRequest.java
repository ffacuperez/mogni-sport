package mogni.system_backend.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plan.entity.PlanAsignado;

@Getter
@Setter
@NoArgsConstructor
public class AsignarPlanDesdePlantillaRequest {

    @NotNull(message = "Elegí el alumno al que le vas a asignar el plan")
    private Long alumnoId;

    @NotNull(message = "Elegí la plantilla que le vas a asignar")
    private Long plantillaId;

    /** HU-266: la plantilla no tiene vigencia; las semanas se eligen al asignarla y la fecha fin se calcula. */
    @NotNull(message = "La cantidad de semanas es obligatoria")
    @Min(value = 1, message = "El plan tiene que durar al menos una semana")
    @Max(value = PlanAsignado.MAXIMO_SEMANAS, message = "El plan no puede durar más de 52 semanas")
    private Integer cantidadSemanas;
}
