package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistroSerieResponse {
    private int orden;
    private BigDecimal peso;
    private Integer repeticiones;
    private Integer rir;
    private String nota;
    private BigDecimal rmEstimado;
    private boolean completo;
}
