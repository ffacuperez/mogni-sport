package mogni.system_backend.plantilla.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /** Objetivo fijo, o el mínimo del rango si viene repeticionesMax (8-10 → 8 y 10). Solo enteros. */
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    @NotNull(message = "Las repeticiones objetivo son obligatorias")
    @Min(value = 1, message = "Las repeticiones objetivo tienen que ser al menos 1")
    @Max(value = 100, message = "Las repeticiones objetivo no pueden ser más de 100")
    private Integer repeticiones;

    /** Opcional: máximo del rango de repeticiones. Sin él, las repeticiones son un objetivo fijo. */
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    @Max(value = 100, message = "Las repeticiones objetivo no pueden ser más de 100")
    private Integer repeticionesMax;

    /** Opcional, en minutos (1.5 = un minuto y medio). */
    @DecimalMin(value = "0.0", inclusive = false, message = "El descanso sugerido tiene que ser mayor a 0")
    @DecimalMax(value = "10", message = "El descanso sugerido no puede superar los 10 minutos")
    @Digits(integer = 2, fraction = 2, message = "El descanso sugerido admite hasta 2 decimales")
    private BigDecimal descansoMinutos;

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

    @JsonIgnore
    @AssertTrue(message = "En el rango de repeticiones, el mínimo no puede ser mayor al máximo")
    public boolean isRangoRepeticionesValido() {
        return repeticiones == null || repeticionesMax == null || repeticiones <= repeticionesMax;
    }
}
