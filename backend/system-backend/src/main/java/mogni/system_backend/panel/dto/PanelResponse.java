package mogni.system_backend.panel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Panel del propietario: el resumen del gimnasio en el mes calendario de {@code fecha}. Sin datos los conteos y montos
 * vienen en 0 y los porcentajes en null (el frontend muestra "—").
 *
 * @param ingresosPorMes      los últimos meses (incluido el actual), del más viejo al más nuevo; sin pagos, total 0
 * @param activosPorMembresia por la inscripción ACTIVA; membresia null = alumnos activos sin inscripción activa
 */
public record PanelResponse(
        LocalDate fecha,
        Ingresos ingresos,
        List<IngresoMes> ingresosPorMes,
        Alumnos alumnos,
        List<ActivosMembresia> activosPorMembresia,
        Ocupacion ocupacion,
        Asistencia asistencia) {

    /**
     * @param mes                 total cobrado en el mes (incluye recargos y diferencias por cambio de membresía)
     * @param variacionPorcentaje contra el mes anterior, con un decimal; null si el mes anterior fue $0
     * @param recargos            parte del total que corresponde a recargos por mora
     */
    public record Ingresos(BigDecimal mes, BigDecimal mesAnterior, BigDecimal variacionPorcentaje,
                           BigDecimal efectivo, BigDecimal transferencia, BigDecimal recargos) {
    }

    /** @param mes 1 a 12 */
    public record IngresoMes(int anio, int mes, BigDecimal total) {
    }

    /** @param enMora con la misma regla que la lista de Alumnos (SituacionPago.de sobre la última cuota pagada) */
    public record Alumnos(long activos, long nuevosDelMes, long bajasDelMes, long enMora) {
    }

    public record ActivosMembresia(String membresia, long cantidad) {
    }

    /** Reservas activas sobre el cupo de los turnos del mes que ya empezaron, sin los cancelados. */
    public record Ocupacion(long reservas, long cupo, BigDecimal porcentaje) {
    }

    /** Presentes sobre presentes + ausentes, en los turnos del mes hasta hoy (las reservas sin marcar no cuentan). */
    public record Asistencia(long presentes, long ausentes, BigDecimal porcentaje) {
    }
}
