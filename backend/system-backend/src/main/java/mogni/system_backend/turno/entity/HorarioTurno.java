package mogni.system_backend.turno.entity;

import java.time.LocalTime;

/** Inicio y fin de un turno de 1 hora calculado a partir de una franja (no se guarda en la base). */
public record HorarioTurno(LocalTime horaInicio, LocalTime horaFin) {
}
