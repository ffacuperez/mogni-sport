package mogni.system_backend.pago.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.pago.entity.MedioDePago;

@Getter
@Setter
public class RegistrarPagoRequest {

    @NotNull(message = "Debe seleccionarse la cuota a pagar")
    private Long cuotaId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "Debe seleccionarse el medio de pago")
    private MedioDePago medioDePago;
}
