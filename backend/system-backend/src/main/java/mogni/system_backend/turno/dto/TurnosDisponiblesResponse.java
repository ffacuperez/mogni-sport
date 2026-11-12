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
    /** Turnos por semana que permite la membresía activa del alumno (0 si no tiene). */
    private int turnosPorSemana;
    private boolean tieneReservaEnElDia;
    /** Turnos de fechaSeleccionada ordenados por hora de inicio (si es hoy, sin los que ya empezaron). */
    private List<TurnoDisponibleResponse> turnos;
    /**
     * MOGNI-254/259: el alumno está en mora. Puede ver la sección pero no elegir turnos (ninguno viene seleccionable)
     * hasta que regularice el pago.
     */
    private boolean bloqueadoPorMora;
    /**
     * MOGNI-281: le queda una clase extra para fechaSeleccionada. Con la semana completa igual puede reservar ese día
     * (los turnos vienen seleccionables), como lo permite la reserva.
     */
    private boolean tieneClaseExtra;
    /**
     * Día de gracia: la cuota venció ayer y fechaSeleccionada es mañana, cuando ya va a estar en mora. Ve los turnos
     * pero no puede elegir ninguno hasta que regularice el pago. Los de hoy los puede reservar.
     */
    private boolean bloqueadoPorCuotaVencida;
}
