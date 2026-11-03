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
    /**
     * Si el alumno lo puede elegir: DISPONIBLE, sin otra reserva ese día, con turnos restantes en la semana y con al
     * menos 1 hora de anticipación.
     */
    private boolean seleccionable;
    /** MOGNI-254: falta menos de 1 hora para que empiece; se muestra, pero ya no se puede reservar. */
    private boolean faltaMenosDeUnaHora;
}
