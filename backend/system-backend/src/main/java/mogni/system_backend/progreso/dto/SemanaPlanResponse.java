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
    private List<SesionSemanaResponse> sesiones;
}
