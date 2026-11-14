package mogni.system_backend.progreso.dto;


/** MOGNI-281: el plan vigente en el inicio del alumno. */
public record PlanInicioResponse(Long id, String nombre, int semanaActual, int cantidadSemanas) {
}
