package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.turno.entity.EstadoReserva;

/** HU-254: la reserva recién creada y cuántos turnos le quedan al alumno en la semana. */
@Getter
@AllArgsConstructor
public class ReservaResponse {
    private Long id;
    private Long turnoId;
    private LocalDate fecha;
    /** "HH:mm" */
    private String horaInicio;
    /** "HH:mm" */
    private String horaFin;
    private EstadoReserva estado;
    private int turnosRestantesSemana;
}
