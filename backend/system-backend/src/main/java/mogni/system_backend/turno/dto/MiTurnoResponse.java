package mogni.system_backend.turno.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** HU-256: un turno que el alumno tiene reservado. */
@Getter
@AllArgsConstructor
public class MiTurnoResponse {
    private Long reservaId;
    private Long turnoId;
    private LocalDate fecha;
    private DayOfWeek diaSemana;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    /** Si todavía se puede cancelar: hasta 2 horas antes de la hora de inicio (HU-255). */
    private boolean puedeCancelar;
}
