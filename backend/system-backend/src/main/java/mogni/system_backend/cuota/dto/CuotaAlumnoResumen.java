package mogni.system_backend.cuota.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.SituacionPago;

/**
 * Situación de cuota de un alumno activo, para el listado y la ficha. Si no tiene cuotas (solo con datos viejos),
 * el vencimiento, la situación y los días en mora vienen en null.
 */
@Getter
@AllArgsConstructor
public class CuotaAlumnoResumen {
    private LocalDate fechaVencimiento;
    /** Precio actual de la membresía que va a renovar (la programada si tiene, si no la activa). */
    private BigDecimal precioMembresia;
    private SituacionPago situacion;
    /** Solo si está EN_MORA: 0 el día en que entró en mora. */
    private Long diasEnMora;
    /** Si se le puede registrar la renovación hoy (ventana de 10 días, vencida o en mora). */
    private boolean puedeRenovar;
    /** Desde qué día se puede renovar (10 días antes del vencimiento); null si no tiene cuotas. */
    private LocalDate renovableDesde;
    /**
     * Inicio de la vigencia que se le muestra al alumno (hasta fechaVencimiento): el del período que cubre hoy o, si
     * ya venció, el del último período pago. Null si no tiene cuotas.
     */
    private LocalDate vigenteDesde;
}
