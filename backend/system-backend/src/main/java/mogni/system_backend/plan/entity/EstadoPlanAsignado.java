package mogni.system_backend.plan.entity;

/** Un alumno tiene como mucho un plan ACTIVO; para asignarle otro hay que FINALIZAR el actual. */
public enum EstadoPlanAsignado {
    ACTIVO,
    FINALIZADO
}
