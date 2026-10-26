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
    /** Membresía con la que renueva: la actual o la elegida. */
    private String membresiaNombre;
    /** Período que va a cubrir la cuota nueva. */
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    /** Precio actual de la membresía con la que renueva; el recargo se calcula sobre él. */
    private BigDecimal precio;
    private BigDecimal recargo;
    private BigDecimal total;
    /** Membresía que usa hoy (la de su inscripción activa). */
    private String membresiaActual;
    /** true si renueva con una membresía distinta a la actual. */
    private boolean cambioDeMembresia;
    /** Solo con cambio: desde qué día rige la membresía nueva (hoy si está vencida o en mora). */
    private LocalDate rigeDesde;
    /** Solo con cambio: true si rige en el futuro (queda PROGRAMADA hasta ese día). */
    private boolean programada;
}
