package mogni.system_backend.progreso.dto;

import mogni.system_backend.plan.entity.MetodoPlan;

/** MOGNI-281: el plan vigente en el inicio del alumno. metodo y mesociclo son null en los planes que no los tienen. */
public record PlanInicioResponse(Long id, String nombre, MetodoPlan metodo, String mesociclo, int semanaActual,
                                 int cantidadSemanas) {
}
