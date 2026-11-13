package mogni.system_backend.inicio.dto;

import java.time.LocalDate;

import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.progreso.dto.PlanInicioResponse;
import mogni.system_backend.turno.dto.MotivoNoReserva;

/**
 * MOGNI-281: todo lo que muestra el inicio del alumno, en una sola respuesta. El backend decide (estado de la cuota, si
 * puede reservar y por qué no); el frontend solo dibuja.
 *
 * @param hoy            hoy en Argentina, para que el frontend diga "Hoy" o "Mañana" sin usar el reloj del navegador
 * @param membresia      la de la inscripción activa; null si no tiene
 * @param diasPorSemana  turnos por semana de esa membresía (0 sin membresía activa)
 * @param activo         la cuenta está activa (una inactiva no llega: recibe 403)
 * @param cuota          siempre presente; sin cuotas (datos viejos) viene con todo en null, como en el resto del sistema
 * @param proximoTurno   el primero de Mis turnos; null si no tiene ninguno por delante
 * @param turnosSemana   reservas activas de esta semana (lunes a domingo) y el total de la membresía
 * @param clasesCuota    a cuántas clases fue en el período de la cuota que cubre hoy; null si ninguna lo cubre
 * @param puedeReservar  si puede reservar algún turno de hoy o de mañana, con las mismas reglas que la reserva
 * @param motivoNoReserva null si puede reservar
 * @param planActivo     el plan vigente; null si no tiene
 */
public record MiInicioResponse(
        LocalDate hoy,
        String nombre,
        String membresia,
        int diasPorSemana,
        boolean activo,
        Cuota cuota,
        ProximoTurno proximoTurno,
        TurnosSemana turnosSemana,
        ClasesCuota clasesCuota,
        boolean puedeReservar,
        MotivoNoReserva motivoNoReserva,
        PlanInicioResponse planActivo) {

    /** situacion null: no tiene cuotas. diasParaVencer es negativo una vez vencida (ayer = -1). */
    public record Cuota(SituacionPago situacion, LocalDate fechaVencimiento, Long diasParaVencer) {
    }

    /** Horas como "HH:mm". */
    public record ProximoTurno(LocalDate fecha, String horaInicio, String horaFin) {
    }

    public record TurnosSemana(int usados, int total) {
    }

    /**
     * Clases con asistencia «Presente» en el período de la cuota que cubre hoy y de cuántas podía ir en ese período
     * (una por día, solo días con franjas y hasta la frecuencia por semana). ultima: la última clase con «Presente»
     * (de cualquier período); null si nunca fue.
     */
    public record ClasesCuota(long presentes, long total, UltimaClase ultima) {
    }

    /** Hora como "HH:mm". */
    public record UltimaClase(LocalDate fecha, String horaInicio) {
    }
}
