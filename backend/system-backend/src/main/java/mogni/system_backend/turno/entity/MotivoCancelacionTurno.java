package mogni.system_backend.turno.entity;

/**
 * Por qué un turno está CANCELADO. Un CANCELADO sin motivo (turnos anteriores a este campo) se trata como MANUAL.
 */
public enum MotivoCancelacionTurno {
    /** El propietario lo canceló a propósito (un turno o un día entero). Nunca se regenera solo. */
    MANUAL,
    /** La franja horaria que lo respaldaba se eliminó o se movió. Se reactiva si la franja vuelve. */
    FRANJA_ELIMINADA
}
