package mogni.system_backend.turno.service;

/** Cambió el cupo máximo del gimnasio: lo toman los turnos futuros que todavía no tienen reservas. */
public record CupoModificadoEvent(int cupoMaximo) {
}
