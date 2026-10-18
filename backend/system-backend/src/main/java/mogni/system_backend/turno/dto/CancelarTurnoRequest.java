package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelarTurnoRequest {
    @NotNull
    private LocalDate fecha;

    @NotBlank
    @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "La hora debe tener el formato HH:mm.")
    private String horaInicio;
}
