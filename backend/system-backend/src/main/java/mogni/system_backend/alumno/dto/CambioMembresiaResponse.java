package mogni.system_backend.alumno.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Vista previa del cambio de membresía (reglas M1 a M9), para el aviso de confirmación del front. Si no se permite,
 * motivo dice por qué. Los montos vienen en null si el alumno no tiene cuota vigente (solo con datos viejos).
 */
@Getter
@AllArgsConstructor
public class CambioMembresiaResponse {
    private boolean permitido;
    private String motivo;
    private String membresiaActual;
    private String membresiaNueva;
    /** El cambio aplica desde hoy. */
    private LocalDate desde;
    /** Vencimiento de la cuota vigente, que mantiene sus fechas. */
    private LocalDate hasta;
    /** Precio de la cuota vigente, sin recargo. */
    private BigDecimal precioPagado;
    /** Precio actual de la membresía nueva. */
    private BigDecimal precioNuevo;
    /** Precio con el que queda la cuota: el mayor de los dos (sin reembolso). */
    private BigDecimal precioFinal;
    private BigDecimal diferenciaACobrar;
    /** true si la nueva es más barata: no se devuelve la diferencia. */
    private boolean sinReembolso;
}
