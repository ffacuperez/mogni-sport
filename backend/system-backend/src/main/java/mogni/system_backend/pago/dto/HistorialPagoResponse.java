package mogni.system_backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.pago.entity.MedioDePago;

@Getter
@AllArgsConstructor
public class HistorialPagoResponse {
    private Long pagoId;
    private BigDecimal monto;
    private MedioDePago medioDePago;
    private LocalDate fechaPago;
    private String periodo;
}
