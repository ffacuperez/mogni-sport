package mogni.system_backend.cuota.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.dto.CuotaAlumnoResumen;
import mogni.system_backend.cuota.dto.EstadoCuota;
import mogni.system_backend.cuota.dto.PeriodoCuota;
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
     * El período de la cuota que cubre hoy. Si ese día empieza otra cuota (renovó a tiempo), es el de la nueva. Un
     * cambio de membresía no lo corta: el período empieza con la primera cuota que termina en ese vencimiento. Vacío
     * si ninguna cuota cubre hoy (vencida, en mora o sin cuotas).
     */
    public static Optional<PeriodoCuota> periodoQueCubre(Alumno alumno, LocalDate hoy) {
        List<Cuota> cuotas = cuotasDe(alumno);
        return cuotas.stream()
                .filter(cuota -> !cuota.getFechaInicio().isAfter(hoy) && !cuota.getFechaVencimiento().isBefore(hoy))
                .max(Comparator.comparing(Cuota::getFechaInicio))
                .map(cuota -> new PeriodoCuota(inicioDelPeriodo(cuotas, cuota.getFechaVencimiento()),
                        cuota.getFechaVencimiento()));
    }

    private static List<Cuota> cuotasDe(Alumno alumno) {
        return alumno.getInscripciones().stream().flatMap(inscripcion -> inscripcion.getCuotas().stream()).toList();
    }

    private static LocalDate inicioDelPeriodo(List<Cuota> cuotas, LocalDate vencimiento) {
        return cuotas.stream()
                .filter(cuota -> cuota.getFechaVencimiento().equals(vencimiento))
                .map(Cuota::getFechaInicio)
                .min(Comparator.naturalOrder())
                .orElseThrow();
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
            cuota != null ? cuota.getFechaVencimiento().minusDays(ReglasCuota.DIAS_VENTANA_RENOVACION) : null,
            // Vigencia: desde que empezó el período de hoy (o el de la última cuota, si ya venció) hasta el vencimiento
            cuota == null ? null : periodoQueCubre(alumno, hoy).map(PeriodoCuota::desde)
                    .orElseGet(() -> inicioDelPeriodo(cuotasDe(alumno), cuota.getFechaVencimiento()))
        );
    }
}
