package mogni.system_backend.plantilla.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@NoArgsConstructor
public class EjercicioSesionRequest {

    @NotNull(message = "Cada ejercicio de la sesión tiene que ser uno del catálogo")
    private Long ejercicioId;

    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    @NotNull(message = "Las series son obligatorias")
    @Min(value = 1, message = "Las series tienen que ser al menos 1")
    @Max(value = 20, message = "Las series no pueden ser más de 20")
    private Integer series;

    /** Un número entero: un rango como "8-10" o un decimal como 8.5 no se aceptan. */
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    @NotNull(message = "Las repeticiones objetivo son obligatorias")
    @Min(value = 1, message = "Las repeticiones objetivo tienen que ser al menos 1")
    @Max(value = 100, message = "Las repeticiones objetivo no pueden ser más de 100")
    private Integer repeticiones;

    /** Opcional, en kg. */
    @DecimalMin(value = "0.0", inclusive = false, message = "El peso sugerido tiene que ser mayor a 0")
    @DecimalMax(value = "999.99", message = "El peso sugerido no puede superar los 999,99 kg")
    @Digits(integer = 3, fraction = 2, message = "El peso sugerido admite hasta 2 decimales")
    private BigDecimal pesoSugerido;

    /** Opcional: repeticiones en reserva al terminar cada serie. */
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    @Min(value = 0, message = "El RIR objetivo no puede ser negativo")
    @Max(value = 10, message = "El RIR objetivo no puede ser más de 10")
    private Integer rirObjetivo;
}
