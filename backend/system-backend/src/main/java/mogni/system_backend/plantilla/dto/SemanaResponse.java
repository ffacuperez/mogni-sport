package mogni.system_backend.plantilla.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Una semana de una plantilla o de un plan asignado, con sus sesiones. */
@Getter
@AllArgsConstructor
public class SemanaResponse {
    private Long id;
    private int numero;
    /** null si la semana no tiene nota. */
    private String nota;
    private List<SesionPlantillaResponse> sesiones;
}
