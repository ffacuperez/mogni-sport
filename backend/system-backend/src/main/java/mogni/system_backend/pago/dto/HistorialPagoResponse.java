package mogni.system_backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.pago.entity.MedioDePago;

/** HU-48: un pago con el período que cubrió, su membresía y el desglose del recargo. */
@Getter
@AllArgsConstructor
public class HistorialPagoResponse {
    private Long pagoId;
    private LocalDate fechaPago;
    private MedioDePago medioDePago;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private String membresiaNombre;
    private BigDecimal precio;
    private BigDecimal recargo;
    /** Total cobrado: precio + recargo. */
    private BigDecimal monto;
}
