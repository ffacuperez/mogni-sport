package mogni.system_backend.turno.entity;

/** "Completo" no es un estado: es un turno DISPONIBLE sin lugares libres. */
public enum EstadoTurno {
    DISPONIBLE,
    CANCELADO
}
