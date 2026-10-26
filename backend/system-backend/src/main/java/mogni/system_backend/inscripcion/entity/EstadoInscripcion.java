package mogni.system_backend.inscripcion.entity;

public enum EstadoInscripcion {
    ACTIVA,
    CANCELADA,
    /**
     * Membresía ya pagada para el período que empieza en su fecha de inicio (renovó por adelantado con otra
     * membresía). Hasta ese día el alumno sigue con su inscripción ACTIVA; ese día ponerAlDia() la activa.
     */
    PROGRAMADA
}
