package mogni.system_backend.turno.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** MOGNI-257: vista Semana de la agenda, de lunes a domingo. */
@Getter
@AllArgsConstructor
public class AgendaSemanaResponse {
    /** Hoy en Argentina. */
    private LocalDate hoy;
    /** Lunes. */
    private LocalDate desde;
    /** Domingo. */
    private LocalDate hasta;
    /** Los 7 días, de lunes a domingo. */
    private List<DiaAgendaResponse> dias;
}
