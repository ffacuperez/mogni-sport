package mogni.system_backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.pago.entity.MedioDePago;

/** Pago registrado con la cuota que creó: período, precio, recargo y total. */
@Getter
@AllArgsConstructor
public class PagoResponse {
    private Long id;
    private Long cuotaId;
    private Long alumnoId;
    private String membresiaNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private BigDecimal precio;
    private BigDecimal recargo;
    /** Precio + recargo. */
    private BigDecimal monto;
    private MedioDePago medioDePago;
    private LocalDate fechaPago;
}
