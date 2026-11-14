package mogni.system_backend.progreso.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SemanaPlanResponse {
    private int numero;
    private LocalDate desde;
    private LocalDate hasta;
    /** La nota que el profe le puso a la semana ("Semana de descarga"); null si no tiene. */
    private String nota;
    private List<SesionSemanaResponse> sesiones;
}
