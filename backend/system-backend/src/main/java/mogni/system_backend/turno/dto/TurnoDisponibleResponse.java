package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TurnoDisponibleResponse {
    private Long id;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    private int lugaresLibres;
    private EstadoTurnoAlumno estado;
    /** Si el alumno lo puede elegir: DISPONIBLE, sin otra reserva ese día y con turnos restantes en la semana. */
    private boolean seleccionable;
}
