package mogni.system_backend.turno.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.dto.EstadoCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.turno.dto.MotivoNoReserva;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * Las reglas del alumno para reservar: un solo lugar para la reserva (HU-254), la pantalla de turnos (HU-259) y el
 * inicio (MOGNI-281), así ninguna dice que se puede cuando otra lo rechaza. Las del turno (fecha, cupo, horario) siguen
 * en ReservaService.
 */
public final class ReglasReserva {

    private ReglasReserva() {
    }

    /** La semana de un día va de lunes a domingo. El cupo se cuenta en la semana del turno, no en la de hoy. */
    public static LocalDate lunesDe(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Las reservas que cuentan para el límite semanal: las activas (también las ausentes) y las canceladas por el
     * gimnasio. Estas últimas se compensan con la clase extra, que es la única compensación: si no contaran, el
     * alumno recuperaría el día en la semana y además tendría la clase extra. Las que cancela el alumno o el
     * sistema no cuentan.
     */
    public static final List<EstadoReserva> CUENTAN_PARA_LA_SEMANA =
            List.of(EstadoReserva.RESERVADA, EstadoReserva.CANCELADA_POR_GIMNASIO);

    /** Las reservas del alumno que cuentan para el límite semanal (ver CUENTAN_PARA_LA_SEMANA) en la semana de esa fecha. */
    public static List<Reserva> reservasDeLaSemana(ReservaRepository reservaRepository, Long alumnoId, LocalDate fecha) {
        LocalDate lunes = lunesDe(fecha);
        return reservaRepository.findByAlumnoIdAndEstadoInAndTurnoFechaBetween(
                alumnoId, CUENTAN_PARA_LA_SEMANA, lunes, lunes.plusDays(6));
    }

    /** Si la reserva sigue activa (las canceladas por el gimnasio cuentan para la semana, pero ya no son un turno). */
    public static boolean estaActiva(Reserva reserva) {
        return reserva.getEstado() == EstadoReserva.RESERVADA;
    }

    /** Turnos por semana que da la membresía de la inscripción activa; 0 si no tiene. */
    public static int frecuenciaSemanal(Alumno alumno) {
        return alumno.buscarInscripcionActiva()
                .map(inscripcion -> inscripcion.getMembresia().getFrecuenciaSemanal())
                .orElse(0);
    }

    /** MOGNI-254/259: en mora no puede reservar (vencida todavía sí). Sin cuotas (datos viejos) no se bloquea. */
    public static boolean estaEnMora(Alumno alumno, LocalDate hoy) {
        return CuotaService.estadoCuotaDe(alumno, hoy).map(EstadoCuota::situacion).orElse(null) == SituacionPago.EN_MORA;
    }

    /**
     * Si el alumno puede reservar algún turno de ese día, en el orden en que lo valida la reserva: un solo turno por día,
     * membresía activa y tope semanal. Con la semana completa todavía puede usar una clase extra: hayClaseExtra solo se
     * consulta en ese caso (la reserva la bloquea; las consultas, no). La mora se valida aparte, antes.
     */
    public static Optional<MotivoNoReserva> motivoParaElDia(Alumno alumno, LocalDate dia, List<Reserva> deLaSemana,
                                                            BooleanSupplier hayClaseExtra) {
        // Un turno por día: el que le canceló el gimnasio no le impide reservar otro horario ese mismo día
        if (deLaSemana.stream().anyMatch(reserva -> estaActiva(reserva) && reserva.getTurno().getFecha().equals(dia))) {
            return Optional.of(MotivoNoReserva.SIN_DIAS_DISPONIBLES);
        }
        int frecuencia = frecuenciaSemanal(alumno);
        if (frecuencia == 0) {
            return Optional.of(MotivoNoReserva.SIN_MEMBRESIA_ACTIVA);
        }
        if (deLaSemana.size() >= frecuencia && !hayClaseExtra.getAsBoolean()) {
            return Optional.of(MotivoNoReserva.CUPO_SEMANAL_AGOTADO);
        }
        return Optional.empty();
    }
}
