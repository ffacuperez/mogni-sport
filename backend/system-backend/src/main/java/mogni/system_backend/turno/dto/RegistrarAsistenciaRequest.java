package mogni.system_backend.turno.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.turno.entity.Asistencia;

/** HU-262: { "asistencia": "PRESENTE" | "AUSENTE" }. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarAsistenciaRequest {
    @NotNull(message = "Indicá si el alumno estuvo presente o ausente.")
    private Asistencia asistencia;
}
