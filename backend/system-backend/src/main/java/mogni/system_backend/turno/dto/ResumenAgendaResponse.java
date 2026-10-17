package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: tarjetas de resumen de la vista Día. Solo cuentan los turnos no cancelados. */
@Getter
@AllArgsConstructor
public class ResumenAgendaResponse {
    private int cantidadTurnos;
    private int reservas;
    private int turnosCompletos;
    private int lugaresLibres;
}
