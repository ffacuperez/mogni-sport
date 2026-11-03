package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroSerieRequest {

    @NotNull(message = "El orden de la serie es obligatorio")
    @Min(value = 1, message = "La serie debe empezar en 1")
    private Integer orden;

    @NotNull(message = "El peso es obligatorio para completar la serie")
    @DecimalMin(value = "0.0", inclusive = false, message = "El peso debe ser mayor que 0")
    private BigDecimal peso;

    @NotNull(message = "Las repeticiones son obligatorias para completar la serie")
    @Min(value = 1, message = "Las repeticiones deben ser al menos 1")
    private Integer repeticiones;

    @NotNull(message = "El RIR es obligatorio para completar la serie")
    @Min(value = 0, message = "El RIR no puede ser negativo")
    @Max(value = 10, message = "El RIR no puede ser más de 10")
    private Integer rir;

    private String nota;
}
