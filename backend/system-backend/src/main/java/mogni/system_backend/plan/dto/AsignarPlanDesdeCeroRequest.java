package mogni.system_backend.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plan armado desde cero, directamente para el alumno: no queda guardado como plantilla. */
@Getter
@Setter
@NoArgsConstructor
public class AsignarPlanDesdeCeroRequest extends PlanRequest {

    @NotNull(message = "Elegí el alumno al que le vas a asignar el plan")
    private Long alumnoId;
}
