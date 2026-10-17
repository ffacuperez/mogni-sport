package mogni.system_backend.turno.dto;

/**
 * MOGNI-257: cómo ve el personal un día en la agenda. Se calcula en cada consulta, con esta prioridad:
 * SIN_HORARIOS (no tiene turnos) → PASADO → BLOQUEADO (después de mañana) → RESERVAS_ABIERTAS (hoy y mañana).
 */
public enum EstadoDiaAgenda {
    RESERVAS_ABIERTAS,
    BLOQUEADO,
    PASADO,
    SIN_HORARIOS
}
