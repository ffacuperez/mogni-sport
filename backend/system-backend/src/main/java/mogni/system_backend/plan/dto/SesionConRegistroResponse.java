package mogni.system_backend.plan.dto;

import java.time.LocalDate;

/**
 * Una sesión de la semana en curso que el alumno ya empezó o registró. En el editor queda en solo lectura y no se
 * puede quitar.
 *
 * @param completada true si ya la registró; false si la empezó y todavía no la terminó
 * @param fecha      el día en que la empezó
 */
public record SesionConRegistroResponse(int semana, int numeroSesion, boolean completada, LocalDate fecha) {
}
