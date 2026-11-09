package mogni.system_backend.cuota.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.dto.CuotaAlumnoResumen;
import mogni.system_backend.cuota.dto.EstadoCuota;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

public class CuotaService {

    /**
     * MOGNI-281: la situación de pago de un alumno. Sale de la última cuota pagada (activa + programada): quien ya pagó
     * el período siguiente con otra membresía no aparece por vencer. Vacío si no tiene cuotas (solo datos viejos):
     * igual que en el listado, no se lo trata como al día.
     */
    public static Optional<EstadoCuota> estadoCuotaDe(Alumno alumno, LocalDate hoy) {
        return alumno.buscarUltimaCuotaPagada().map(cuota -> new EstadoCuota(
                SituacionPago.de(cuota, hoy),
                cuota.getFechaVencimiento(),
                ReglasCuota.diasHastaVencimiento(cuota.getFechaVencimiento(), hoy)));
    }

    /**
     * Situación de cuota de un alumno a partir de su última cuota pagada (activa + programada); cuota en null si no
     * tiene (solo datos viejos).
     */
    public static CuotaAlumnoResumen resumenDe(Alumno alumno, Cuota cuota, LocalDate hoy) {
        Membresia membresia = alumno.buscarInscripcionActiva().map(Inscripcion::getMembresia).orElse(null);
        // El precio es lo que va a pagar en la próxima renovación: el de la programada si ya pagó el cambio
        Membresia aRenovar = alumno.buscarInscripcionProgramada().map(Inscripcion::getMembresia).orElse(membresia);
        SituacionPago situacion = SituacionPago.de(cuota, hoy);
        // Sin cuotas (datos viejos) pero con inscripción activa: puede renovar, el período empieza hoy
        boolean puedeRenovar = cuota != null
                ? ReglasCuota.puedeRenovar(cuota.getFechaVencimiento(), hoy)
                : membresia != null;
        return new CuotaAlumnoResumen(
            cuota != null ? cuota.getFechaVencimiento() : null,
            aRenovar != null ? BigDecimal.valueOf(aRenovar.getPrecioActual()).setScale(2) : null,
            situacion,
            situacion == SituacionPago.EN_MORA ? ReglasCuota.diasEnMora(cuota.getFechaVencimiento(), hoy) : null,
            puedeRenovar,
            cuota != null ? cuota.getFechaVencimiento().minusDays(ReglasCuota.DIAS_VENTANA_RENOVACION) : null
        );
    }
}
