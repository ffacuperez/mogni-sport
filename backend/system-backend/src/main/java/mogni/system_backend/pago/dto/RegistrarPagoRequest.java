package mogni.system_backend.pago.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.pago.entity.MedioDePago;

/** El monto no lo envía el cliente: siempre es el monto esperado de la cuota (ver PagoService). */
@Getter
@Setter
public class RegistrarPagoRequest {

    @NotNull(message = "Debe seleccionarse la cuota a pagar")
    private Long cuotaId;

    @NotNull(message = "Debe seleccionarse el medio de pago")
    private MedioDePago medioDePago;
}
