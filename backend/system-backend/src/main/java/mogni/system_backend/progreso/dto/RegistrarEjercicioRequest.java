package mogni.system_backend.progreso.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** HU-269: todas las series de un ejercicio y una nota opcional. Que sean las que pide el plan lo valida el servicio. */
@Getter
@Setter
@NoArgsConstructor
public class RegistrarEjercicioRequest {

    @NotEmpty(message = "Cargá las series del ejercicio")
    private List<@NotNull(message = "Cargá las series del ejercicio") @Valid SerieRequest> series;

    @Size(max = 500, message = "La nota puede tener hasta 500 caracteres")
    private String notas;
}
