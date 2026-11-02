package mogni.system_backend.turno.entity;

/**
 * HU-262: marca del entrenador sobre una reserva RESERVADA de un turno que ya comenzó. Mientras no se marca, la
 * asistencia de la reserva es null ("sin marcar"); al terminar el día las que siguen sin marcar pasan a AUSENTE.
 * La reserva sigue RESERVADA en los dos casos: cuenta para el límite semanal y nunca genera clase extra.
 */
public enum Asistencia {
    PRESENTE,
    AUSENTE
}
