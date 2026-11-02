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
    /** El propietario puede reactivarlo: cancelado a propósito, sin empezar y ya generado (con id). */
    private boolean puedeReactivarse;
    /** Cancelado porque su horario salió de la agenda (no se reactiva a mano). */
    private boolean canceladoPorFranja;
    /** Si está transcurriendo ahora (badge "Ahora"). */
    private boolean enCurso;
    /** HU-262: ya llegó su hora de inicio (y no está cancelado). Antes de eso no hay asistencia para mostrar. */
    private boolean comenzo;
    /** HU-262: se puede marcar o corregir la asistencia ahora (ya comenzó y todavía es su día). */
    private boolean asistenciaEditable;
    /** HU-262: reservas activas marcadas PRESENTE. */
    private int presentes;
    /** HU-262: reservas activas marcadas AUSENTE (en un día terminado incluye las que quedaron sin marcar). */
    private int ausentes;
    /** HU-262: reservas activas todavía sin marcar (siempre 0 en un día terminado). */
    private int sinMarcar;
    /** Alumnos con reserva activa, por apellido y nombre. En la vista semanal siempre vacío. */
    private List<AlumnoAgendaResponse> alumnos;
}
