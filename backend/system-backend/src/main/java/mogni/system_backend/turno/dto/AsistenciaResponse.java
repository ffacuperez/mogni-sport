package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.turno.entity.Asistencia;

/** HU-262: asistencia que quedó registrada, con el conteo actualizado del turno (para la lista de la agenda). */
@Getter
@AllArgsConstructor
public class AsistenciaResponse {
    private Long reservaId;
    private Long turnoId;
    private Asistencia asistencia;
    private int presentes;
    private int ausentes;
    private int sinMarcar;
}
