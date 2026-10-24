package mogni.system_backend.cuota.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reglas de cobro del gimnasio en un solo lugar (ver documentacion/Cuotas-Rediseno-Estados-y-Preguntas.md).
 * Los valores pueden cambiar si el propietario lo decide: se cambian acá y en ningún otro lado.
 */
public final class ReglasCuota {

    /** Días que cubre cada cuota (regla C2). */
    public static final int DIAS_CUOTA = 30;

    /** Desde cuántos días antes del vencimiento el alumno está "Próxima a vencer" (y recibe el aviso). */
    public static final int DIAS_AVISO_VENCIMIENTO = 5;

    /** Desde cuántos días antes del vencimiento se puede renovar (Q2). */
    public static final int DIAS_VENTANA_RENOVACION = 10;

    /** Recargo por renovar en mora, en porcentaje del precio vigente (HU-80). */
    public static final int PORCENTAJE_RECARGO = 10;

    private static final BigDecimal DECENA = BigDecimal.TEN;

    private ReglasCuota() {
    }

    /** Días desde hoy hasta el vencimiento: negativo si ya pasó (ayer = -1). */
    public static long diasHastaVencimiento(LocalDate vencimiento, LocalDate hoy) {
        return ChronoUnit.DAYS.between(hoy, vencimiento);
    }

    /** Días en mora contando el día en que entró en mora como día 0. Solo tiene sentido si está EN_MORA. */
    public static long diasEnMora(LocalDate vencimiento, LocalDate hoy) {
        return -diasHastaVencimiento(vencimiento, hoy) - 2;
    }

    /** Se puede renovar desde 10 días antes del vencimiento, y en cualquier momento después (vencida o en mora). */
    public static boolean puedeRenovar(LocalDate vencimiento, LocalDate hoy) {
        return diasHastaVencimiento(vencimiento, hoy) <= DIAS_VENTANA_RENOVACION;
    }

    /**
     * Inicio de la cuota nueva al renovar: si no está en mora, sigue la cadena (empieza en el vencimiento anterior);
     * si está en mora, empieza hoy (los días en mora no se cobran).
     */
    public static LocalDate inicioRenovacion(LocalDate vencimientoAnterior, LocalDate hoy) {
        return SituacionPago.de(vencimientoAnterior, hoy) == SituacionPago.EN_MORA ? hoy : vencimientoAnterior;
    }

    /** Total con recargo: precio + 10%, redondeado a la decena siempre hacia arriba ($15.420 → $16.970). */
    public static BigDecimal totalConRecargo(BigDecimal precio) {
        BigDecimal conRecargo = precio.multiply(BigDecimal.valueOf(100 + PORCENTAJE_RECARGO))
                .divide(BigDecimal.valueOf(100));
        return conRecargo.divide(DECENA, 0, RoundingMode.CEILING).multiply(DECENA).setScale(2);
    }

    /** Recargo registrado: la diferencia entre el total redondeado y el precio. */
    public static BigDecimal recargo(BigDecimal precio) {
        return totalConRecargo(precio).subtract(precio).setScale(2);
    }

    /**
     * Diferencia a cobrar al cambiar de membresía (regla M5): si la nueva es más cara, la diferencia completa entre su
     * precio actual y lo ya pagado por el período, sin recargo y sin prorrateo; si es igual o más barata, 0 (sin
     * reembolso).
     */
    public static BigDecimal diferenciaCambio(BigDecimal pagadoDelPeriodo, BigDecimal precioNueva) {
        return precioNueva.subtract(pagadoDelPeriodo).max(BigDecimal.ZERO).setScale(2);
    }
}
