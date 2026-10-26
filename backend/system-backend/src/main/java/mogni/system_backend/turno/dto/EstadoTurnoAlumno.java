package mogni.system_backend.turno.dto;

/**
 * Cómo ve el alumno un turno (HU-259). No es el estado guardado del turno: se calcula en cada consulta,
 * con esta prioridad: CANCELADO → RESERVADO → COMPLETO → DISPONIBLE.
 */
public enum EstadoTurnoAlumno {
    DISPONIBLE,
    COMPLETO,
    RESERVADO,
    CANCELADO
}
