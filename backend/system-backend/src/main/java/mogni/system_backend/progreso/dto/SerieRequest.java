package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** HU-269: una serie realizada. Peso, repeticiones y RIR son obligatorios; el peso puede ser 0 (peso del cuerpo). */
@Getter
@Setter
@NoArgsConstructor
public class SerieRequest {

    @NotNull(message = "Falta el número de serie")
    private Integer orden;

    @NotNull(message = "Cargá el peso de todas las series")
    @DecimalMin(value = "0", message = "El peso no puede ser negativo")
    @DecimalMax(value = "1000", message = "El peso no puede superar los 1000 kg")
    private BigDecimal peso;

    @NotNull(message = "Cargá las repeticiones de todas las series")
    @Min(value = 1, message = "Las repeticiones tienen que ser al menos 1")
    @Max(value = 100, message = "Las repeticiones no pueden superar 100")
    private Integer repeticiones;

    @NotNull(message = "Cargá el RIR de todas las series")
    @Min(value = 0, message = "El RIR no puede ser negativo")
    @Max(value = 10, message = "El RIR no puede superar 10")
    private Integer rir;
}
