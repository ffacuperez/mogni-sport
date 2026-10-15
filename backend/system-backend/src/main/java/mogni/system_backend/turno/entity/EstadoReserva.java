package mogni.system_backend.turno.entity;

/** HU-254. Solo las RESERVADA ocupan lugar en el turno y cuentan para el contador semanal. */
public enum EstadoReserva {
    RESERVADA,
    CANCELADA,
    CANCELADA_POR_GIMNASIO
}
