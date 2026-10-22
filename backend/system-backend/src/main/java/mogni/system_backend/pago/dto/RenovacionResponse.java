package mogni.system_backend.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.SituacionPago;

/**
 * Vista previa de la renovación para el modal de confirmación: el cálculo vive solo en el backend y el front lo
 * muestra. Si no se puede renovar, motivo dice por qué y el período puede venir vacío.
 */
@Getter
@AllArgsConstructor
public class RenovacionResponse {
    private Long alumnoId;
    private String nombre;
    private String apellido;
    private String dni;
    private boolean puedeRenovar;
    private String motivo;
    /** Situación actual (null si no tiene cuotas, solo con datos viejos). */
    private SituacionPago situacion;
    private String membresiaNombre;
    /** Período que va a cubrir la cuota nueva. */
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private BigDecimal precio;
    private BigDecimal recargo;
    private BigDecimal total;
}
