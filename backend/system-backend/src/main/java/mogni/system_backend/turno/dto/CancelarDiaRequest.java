package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelarDiaRequest {
    @NotNull
    private LocalDate fecha;
}
