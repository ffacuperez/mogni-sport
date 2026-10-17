package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Turno calculado de una franja, con las horas en formato "HH:mm". */
@Getter
@AllArgsConstructor
public class HorarioTurnoResponse {
    private String horaInicio;
    private String horaFin;
}
