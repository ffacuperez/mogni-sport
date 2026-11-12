package mogni.system_backend.turno.dto;

/** MOGNI-281: por qué el alumno no puede reservar ningún turno de hoy ni de mañana. */
public enum MotivoNoReserva {
    /** La cuota está en mora: no puede reservar hasta regularizar el pago. */
    EN_MORA,
    /**
     * La cuota venció ayer: hoy es su día de gracia y puede reservar los turnos de hoy, pero mañana ya va a estar en
     * mora, así que para reservar los de mañana tiene que regularizar el pago.
     */
    CUOTA_VENCIDA,
    /** No tiene una inscripción activa, así que su membresía no le da turnos. */
    SIN_MEMBRESIA_ACTIVA,
    /** Usó todos los turnos de la semana y no le queda ninguna clase extra. */
    CUPO_SEMANAL_AGOTADO,
    /** Ya tiene un turno hoy y otro mañana, los únicos días que se pueden reservar. */
    SIN_DIAS_DISPONIBLES
}
