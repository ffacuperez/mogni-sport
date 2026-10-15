package mogni.system_backend.turno.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** HU-259: todo lo que necesita la pantalla de turnos del alumno. El backend decide, el frontend solo dibuja. */
@Getter
@AllArgsConstructor
public class TurnosDisponiblesResponse {
    /** Hoy en Argentina. */
    private LocalDate hoy;
    private LocalDate fechaSeleccionada;
    /** Hoy y los 4 días siguientes. */
    private List<DiaTurnosResponse> dias;
    /** De la semana (lunes a domingo) de fechaSeleccionada. */
    private int turnosRestantesSemana;
    private boolean tieneReservaEnElDia;
    /** Turnos de fechaSeleccionada ordenados por hora de inicio (si es hoy, sin los que ya empezaron). */
    private List<TurnoDisponibleResponse> turnos;
}
