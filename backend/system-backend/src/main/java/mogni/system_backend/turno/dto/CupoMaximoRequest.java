package mogni.system_backend.turno.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CupoMaximoRequest {

    @NotNull(message = "El cupo máximo es obligatorio")
    @Min(value = 1, message = "El cupo máximo tiene que ser un número entero mayor a 0.")
    private Integer cupoMaximo;
}
