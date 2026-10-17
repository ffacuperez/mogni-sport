package mogni.system_backend.turno.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: un día de la vista Semana (sin alumnos, solo la ocupación de cada turno). */
@Getter
@AllArgsConstructor
public class DiaAgendaResponse {
    private LocalDate fecha;
    private EstadoDiaAgenda estadoDia;
    /** Solo si está BLOQUEADO. */
    private LocalDate fechaHabilitacion;
    private List<TurnoAgendaResponse> turnos;
}
