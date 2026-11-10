package mogni.system_backend.cuota;

import java.time.LocalDate;

import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.inscripcion.entity.Inscripcion;

/**
 * Ayuda para las pruebas: la situación de pago depende solo del vencimiento, así que las pruebas lo fijan exacto. Como
 * las cuotas son mensuales y los meses tienen distinta cantidad de días, calcular el inicio para que caiga en un
 * vencimiento dado no siempre es posible (al 30/03 no se llega sumando un mes a ningún día de febrero).
 */
public final class CuotasDePrueba {

    private CuotasDePrueba() {
    }

    /** Cuota mensual de la inscripción (inicio un mes antes) que vence exactamente ese día. */
    public static Cuota queVence(Inscripcion inscripcion, LocalDate vencimiento) {
        Cuota cuota = inscripcion.agregarCuota(vencimiento.minusMonths(1));
        cuota.setFechaVencimiento(vencimiento);
        return cuota;
    }
}
