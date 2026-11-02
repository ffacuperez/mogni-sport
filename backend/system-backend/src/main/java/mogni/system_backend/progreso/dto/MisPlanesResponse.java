package mogni.system_backend.progreso.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MisPlanesResponse {
    /** Null si el alumno no tiene plan activo. */
    private PlanResumenResponse activo;
    /** Los planes finalizados, del más reciente al más antiguo. */
    private List<PlanResumenResponse> anteriores;
}
