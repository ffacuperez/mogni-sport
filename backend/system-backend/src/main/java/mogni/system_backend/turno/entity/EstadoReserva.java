package mogni.system_backend.turno.entity;

/** HU-254. Solo las RESERVADA ocupan lugar en el turno y cuentan para el contador semanal. */
public enum EstadoReserva {
    RESERVADA,
    /** La canceló el alumno (HU-255). */
    CANCELADA,
    /** El gimnasio canceló el turno: el alumno recibe una clase extra. */
    CANCELADA_POR_GIMNASIO,
    /** La canceló el sistema porque el alumno entró en mora o fue dado de baja. Sin clase extra. */
    CANCELADA_POR_SISTEMA
}
