package mogni.system_backend.cuota.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.EstadoCuota;

@Getter
@AllArgsConstructor
public class CuotaResponse {
    private Long id;
    private Long alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private String alumnoDni;
    private String membresiaNombre;
    private String periodo;
    private BigDecimal montoEsperado;
    private LocalDate fechaVencimiento;
    private EstadoCuota estado;
}
