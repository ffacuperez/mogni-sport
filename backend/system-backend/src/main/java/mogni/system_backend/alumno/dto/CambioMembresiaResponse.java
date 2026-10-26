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
    /** Vencimiento del período vigente: la cuota del cambio termina el mismo día. */
    private LocalDate hasta;
    /** Lo pagado por el período vigente, sin recargo. */
    private BigDecimal precioPagado;
    /** Precio actual de la membresía nueva. */
    private BigDecimal precioNuevo;
    /** Lo que se cobra hoy: la diferencia si la nueva es más cara, 0 si no. */
    private BigDecimal diferenciaACobrar;
    /** true si la nueva es más barata: no se devuelve la diferencia. */
    private boolean sinReembolso;
    /** true si hay diferencia a cobrar: hay que elegir el medio de pago. */
    private boolean requiereMedioDePago;
    /** Días que le quedan a la cuota actual (0 = vence hoy); null si no tiene cuota vigente. */
    private Long diasRestantes;
}
