package mogni.system_backend.progreso.dto;

/** Sin registro, una sesión está PENDIENTE si el plan sigue activo y NO_REGISTRADA si ya finalizó. */
public enum EstadoSesion {
    REGISTRADA,
    PENDIENTE,
    NO_REGISTRADA
}
