package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: alumno con reserva activa en un turno de la agenda. */
@Getter
@AllArgsConstructor
public class AlumnoAgendaResponse {
    private Long reservaId;
    private Long alumnoId;
    private String nombre;
    private String apellido;
    /** Null si no tiene inscripción activa. */
    private String membresiaNombre;
    /** Null si no tiene cuota de una inscripción activa. */
    private CuotaAgendaResponse cuota;
}
