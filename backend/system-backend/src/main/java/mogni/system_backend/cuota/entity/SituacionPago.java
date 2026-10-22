package mogni.system_backend.cuota.entity;

import java.time.LocalDate;

/**
 * Situación de pago del alumno (sección 4 del rediseño de cuotas). No se guarda: se calcula comparando el
 * vencimiento de su última cuota con la fecha de hoy, como la edad con la fecha de nacimiento.
 */
public enum SituacionPago {
    /** Faltan más de 5 días para el vencimiento. */
    AL_DIA,
    /** Faltan 5 días o menos, incluido el día del vencimiento. */
    PROXIMA_A_VENCER,
    /** El día siguiente al vencimiento: todavía puede reservar y renovar sin recargo. */
    VENCIDA,
    /** Desde dos días después del vencimiento: no puede reservar y al renovar paga recargo. */
    EN_MORA;

    public static SituacionPago de(LocalDate vencimiento, LocalDate hoy) {
        long dias = ReglasCuota.diasHastaVencimiento(vencimiento, hoy);
        if (dias > ReglasCuota.DIAS_AVISO_VENCIMIENTO) {
            return AL_DIA;
        }
        if (dias >= 0) {
            return PROXIMA_A_VENCER;
        }
        return dias == -1 ? VENCIDA : EN_MORA;
    }

    /** Situación según la última cuota del alumno; null si no tiene cuotas (solo pasa con datos viejos). */
    public static SituacionPago de(Cuota ultimaCuota, LocalDate hoy) {
        return ultimaCuota == null ? null : de(ultimaCuota.getFechaVencimiento(), hoy);
    }
}
