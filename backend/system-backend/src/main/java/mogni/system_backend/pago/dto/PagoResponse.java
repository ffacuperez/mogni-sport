package mogni.system_backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.pago.entity.MedioDePago;

@Getter
@AllArgsConstructor
public class PagoResponse {
    private Long id;
    private Long cuotaId;
    private String periodo;
    private BigDecimal monto;
    private MedioDePago medioDePago;
    private LocalDate fechaPago;
    private EstadoCuota estadoCuota;
}
