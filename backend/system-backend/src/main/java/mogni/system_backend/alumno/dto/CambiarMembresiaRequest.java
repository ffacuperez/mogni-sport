package mogni.system_backend.alumno.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.pago.entity.MedioDePago;

/**
 * Cambio de membresía (reglas M1 a M9). El medio de pago es obligatorio solo si la membresía nueva es más cara (se
 * cobra la diferencia); si es igual o más barata no se cobra nada y se usa el medio del pago anterior.
 */
@Getter
@Setter
public class CambiarMembresiaRequest {

    @NotNull(message = "Debe seleccionarse la nueva membresía")
    private Long membresiaId;

    private MedioDePago medioDePago;
}
