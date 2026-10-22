package mogni.system_backend.pago.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.pago.entity.MedioDePago;

/**
 * Renovación (MOGNI-45): se paga el mes siguiente del alumno, no una cuota elegida. El monto, el recargo, la fecha y
 * el período los calcula el backend (ver PagoService).
 */
@Getter
@Setter
public class RegistrarPagoRequest {

    @NotNull(message = "Debe seleccionarse el alumno")
    private Long alumnoId;

    @NotNull(message = "Debe seleccionarse el medio de pago")
    private MedioDePago medioDePago;
}
