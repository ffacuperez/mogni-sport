package mogni.system_backend.cuota.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.SituacionPago;

/**
 * Fila de la pantalla de Cuotas (MOGNI-46): un alumno activo con su última cuota. Si no tiene cuotas (solo con datos
 * viejos), los datos de la cuota y la situación vienen en null.
 */
@Getter
@AllArgsConstructor
public class CuotaResponse {
    private Long alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private String alumnoDni;
    private Long cuotaId;
    private String membresiaNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private BigDecimal precio;
    private SituacionPago situacion;
    /** Solo si está EN_MORA: 0 el día en que entró en mora. */
    private Long diasEnMora;
    /** Si se le puede registrar la renovación hoy (ventana de 10 días, vencida o en mora). */
    private boolean puedeRenovar;
    /** Desde qué día se puede renovar (10 días antes del vencimiento); null si no tiene cuotas. */
    private LocalDate renovableDesde;
}
