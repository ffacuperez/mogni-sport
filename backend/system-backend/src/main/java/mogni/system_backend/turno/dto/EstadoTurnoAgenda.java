package mogni.system_backend.turno.dto;

/**
 * MOGNI-257: cómo ve el personal un turno en la agenda. No es el estado guardado del turno: se calcula en cada
 * consulta, con esta prioridad: CANCELADO → PASADO → BLOQUEADO → COMPLETO → CASI_LLENO → CON_LUGARES.
 */
public enum EstadoTurnoAgenda {
    CON_LUGARES,
    CASI_LLENO,
    COMPLETO,
    CANCELADO,
    PASADO,
    BLOQUEADO
}
