package mogni.system_backend.turno.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: un turno de la agenda con su ocupación. */
@Getter
@AllArgsConstructor
public class TurnoAgendaResponse {
    /** Null si el turno todavía no está generado (días bloqueados: se calcula desde las franjas). */
    private Long turnoId;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    private int cupoMaximo;
    /** Reservas RESERVADA (las canceladas no ocupan lugar). */
    private int ocupados;
    private int libres;
    private EstadoTurnoAgenda estado;
    /** Si está transcurriendo ahora (badge "Ahora"). */
    private boolean enCurso;
    /** Alumnos con reserva activa, por apellido y nombre. En la vista semanal siempre vacío. */
    private List<AlumnoAgendaResponse> alumnos;
}
