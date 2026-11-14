package mogni.system_backend.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** HU-266: el plan dura las semanas que tiene la plantilla; la fecha fin se calcula. */
@Getter
@Setter
@NoArgsConstructor
public class AsignarPlanDesdePlantillaRequest {

    @NotNull(message = "Elegí el alumno al que le vas a asignar el plan")
    private Long alumnoId;

    @NotNull(message = "Elegí la plantilla que le vas a asignar")
    private Long plantillaId;
}
