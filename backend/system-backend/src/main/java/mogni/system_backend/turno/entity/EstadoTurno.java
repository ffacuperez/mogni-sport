package mogni.system_backend.turno.entity;

/** "Completo" no es un estado: es un turno DISPONIBLE sin lugares libres. CANCELADO lo va a usar la HU-258. */
public enum EstadoTurno {
    DISPONIBLE,
    CANCELADO
}
