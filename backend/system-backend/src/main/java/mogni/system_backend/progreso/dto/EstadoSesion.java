package mogni.system_backend.progreso.dto;

/**
 * REGISTRADA: el alumno la finalizó. EN_CURSO (HU-269): la empezó y le faltan ejercicios o finalizarla; solo mientras
 * el plan está activo. Sin registro (o sin terminar en un plan que ya terminó), una sesión está PENDIENTE si el plan
 * sigue activo y NO_REGISTRADA si ya finalizó.
 */
public enum EstadoSesion {
    REGISTRADA,
    EN_CURSO,
    PENDIENTE,
    NO_REGISTRADA
}
