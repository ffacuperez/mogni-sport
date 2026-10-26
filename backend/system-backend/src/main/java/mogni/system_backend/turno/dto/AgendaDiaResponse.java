package mogni.system_backend.turno.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: todo lo que necesita la vista Día de la agenda. El backend decide, el frontend solo dibuja. */
@Getter
@AllArgsConstructor
public class AgendaDiaResponse {
    /** Hoy en Argentina. */
    private LocalDate hoy;
    private LocalDate fecha;
    private EstadoDiaAgenda estadoDia;
    /** Día en que los alumnos pueden empezar a reservar (el anterior). Solo si está BLOQUEADO. */
    private LocalDate fechaHabilitacion;
    /** Cupo de los turnos del día (null si no tiene turnos). */
    private Integer cupoPorTurno;
    /** Null si el día está BLOQUEADO o SIN_HORARIOS. */
    private ResumenAgendaResponse resumen;
    /** Ordenados por hora de inicio. */
    private List<TurnoAgendaResponse> turnos;
}
