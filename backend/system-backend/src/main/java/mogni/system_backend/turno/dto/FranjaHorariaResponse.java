package mogni.system_backend.turno.dto;

import java.time.DayOfWeek;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FranjaHorariaResponse {
    private Long id;
    private DayOfWeek diaSemana;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    private int cantidadTurnos;
    /** Turnos de 1 hora en los que se divide la franja (calculados, no generados). */
    private List<HorarioTurnoResponse> turnos;
}
