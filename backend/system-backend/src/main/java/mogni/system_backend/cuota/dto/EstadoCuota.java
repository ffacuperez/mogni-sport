package mogni.system_backend.cuota.dto;

import java.time.LocalDate;

import mogni.system_backend.cuota.entity.SituacionPago;

/**
 * MOGNI-281: la situación de pago de un alumno según su última cuota pagada (la misma que usan el listado de cuotas,
 * la mora para reservar y los avisos). diasParaVencer es negativo una vez vencida (ayer = -1).
 */
public record EstadoCuota(SituacionPago situacion, LocalDate fechaVencimiento, long diasParaVencer) {
}
