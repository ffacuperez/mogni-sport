package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.DiaTurnosResponse;
import mogni.system_backend.turno.dto.EstadoTurnoAlumno;
import mogni.system_backend.turno.dto.TurnoDisponibleResponse;
import mogni.system_backend.turno.dto.TurnosDisponiblesResponse;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;

/**
 * HU-259: turnos disponibles de hoy y mañana para el alumno logueado. Arma todo lo que muestra la pantalla
 * (días del selector, contador semanal, estado de cada turno y si se puede elegir); el frontend solo dibuja.
 */
@Service
public class ConsultaTurnosService {

    /** El selector muestra hoy y los 4 días siguientes. */
    static final int DIAS_EN_SELECTOR = 5;

    private final AlumnoRepository alumnoRepository;
    private final ReservaRepository reservaRepository;
    private final GeneracionTurnosService generacionTurnosService;
    private final ClaseExtraRepository claseExtraRepository;

    public ConsultaTurnosService(AlumnoRepository alumnoRepository,
                                 ReservaRepository reservaRepository,
                                 GeneracionTurnosService generacionTurnosService,
                                 ClaseExtraRepository claseExtraRepository) {
        this.alumnoRepository = alumnoRepository;
        this.reservaRepository = reservaRepository;
        this.generacionTurnosService = generacionTurnosService;
        this.claseExtraRepository = claseExtraRepository;
    }

    /** fecha null = hoy. Tiene que ser hoy o mañana (en Argentina). */
    @Transactional(readOnly = true)
    public TurnosDisponiblesResponse consultarTurnosDisponibles(String email, LocalDate fecha) {
        return consultarTurnosDisponibles(email, fecha, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    TurnosDisponiblesResponse consultarTurnosDisponibles(String email, LocalDate fecha, LocalDateTime ahora) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        validarAcceso(alumno);

        LocalDate hoy = ahora.toLocalDate();
        LocalDate maniana = hoy.plusDays(1);
        LocalDate seleccionada = fecha != null ? fecha : hoy;
        if (!seleccionada.equals(hoy) && !seleccionada.equals(maniana)) {
            throw new RegistroInvalidoException("Solo podés consultar los turnos de hoy y mañana.");
        }

        // Siempre quedan generados hoy y mañana, se consulte el día que se consulte
        List<Turno> turnosHoy = generacionTurnosService.obtenerOGenerarTurnos(hoy);
        List<Turno> turnosManiana = generacionTurnosService.obtenerOGenerarTurnos(maniana);
        List<Turno> turnosDelDia = seleccionada.equals(hoy) ? turnosHoy : turnosManiana;

        // Reservas del alumno que cuentan en la semana (lunes a domingo) del día seleccionado
        List<Reserva> reservasDeLaSemana = ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), seleccionada);
        int frecuenciaSemanal = ReglasReserva.frecuenciaSemanal(alumno);
        int turnosRestantes = ReglasReserva.turnosQueQuedan(frecuenciaSemanal, reservasDeLaSemana.size(),
                claseExtraRepository.contarDisponibles(alumno.getId(), seleccionada));

        Set<Long> turnosReservadosEnElDia = reservasDeLaSemana.stream()
                .filter(ReglasReserva::estaActiva)
                .filter(reserva -> reserva.getTurno().getFecha().equals(seleccionada))
                .map(reserva -> reserva.getTurno().getId())
                .collect(Collectors.toSet());
        boolean tieneReservaEnElDia = !turnosReservadosEnElDia.isEmpty();

        // En el día de hoy no se muestran los turnos que ya empezaron
        LocalTime horaActual = ahora.toLocalTime();
        List<Turno> turnosAMostrar = turnosDelDia.stream()
                .filter(turno -> !seleccionada.equals(hoy) || turno.getHoraInicio().isAfter(horaActual))
                .sorted(Comparator.comparing(Turno::getHoraInicio))
                .toList();

        Map<Long, Long> ocupados = contarReservasActivas(turnosAMostrar);
        boolean bloqueadoPorMora = ReglasReserva.estaEnMora(alumno, hoy);
        // Día de gracia: los turnos de mañana se ven pero no se pueden elegir hasta que pague
        boolean bloqueadoPorCuotaVencida = ReglasReserva.requierePagoPara(alumno, hoy, seleccionada);
        // MOGNI-281: con la semana completa todavía puede usar una clase extra, igual que al reservar
        boolean tieneClaseExtra = claseExtraRepository.existeDisponible(alumno.getId(), seleccionada);
        boolean puedeElegir = !bloqueadoPorMora && !bloqueadoPorCuotaVencida && ReglasReserva
                .motivoParaElDia(alumno, seleccionada, reservasDeLaSemana, () -> tieneClaseExtra).isEmpty();
        List<TurnoDisponibleResponse> turnos = turnosAMostrar.stream()
                .map(turno -> mapearTurno(turno, ocupados.getOrDefault(turno.getId(), 0L),
                        turnosReservadosEnElDia.contains(turno.getId()), puedeElegir,
                        ReservaService.faltaMenosDeUnaHora(turno, ahora)))
                .toList();

        // Hoy y mañana se habilitan solo si tienen turnos: un día sin franjas (por ejemplo, un sábado sin clases) no se puede elegir
        List<DiaTurnosResponse> dias = IntStream.range(0, DIAS_EN_SELECTOR)
                .mapToObj(i -> new DiaTurnosResponse(hoy.plusDays(i), switch (i) {
                    case 0 -> !turnosHoy.isEmpty();
                    case 1 -> !turnosManiana.isEmpty();
                    default -> false;
                }))
                .toList();

        return new TurnosDisponiblesResponse(hoy, seleccionada, dias, turnosRestantes, frecuenciaSemanal,
                tieneReservaEnElDia, turnos, bloqueadoPorMora, tieneClaseExtra, bloqueadoPorCuotaVencida);
    }

    /** Solo los alumnos con la cuenta activa acceden a los turnos (y a su inicio, MOGNI-281). */
    public static void validarAcceso(Alumno alumno) {
        if (!alumno.getCuenta().isActiva()) {
            throw new AccesoDenegadoException("Tu cuenta está inactiva.");
        }
    }

    /** Reservas RESERVADA de cada turno (las canceladas no ocupan lugar). */
    private Map<Long, Long> contarReservasActivas(List<Turno> turnos) {
        if (turnos.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = turnos.stream().map(Turno::getId).toList();
        return reservaRepository.findByTurnoIdInAndEstado(ids, EstadoReserva.RESERVADA).stream()
                .collect(Collectors.groupingBy(reserva -> reserva.getTurno().getId(), Collectors.counting()));
    }

    private TurnoDisponibleResponse mapearTurno(Turno turno, long reservasActivas, boolean reservadoPorElAlumno,
                                                boolean puedeElegir, boolean faltaMenosDeUnaHora) {
        int lugaresLibres = (int) Math.max(0, turno.getCupoMaximo() - reservasActivas);
        EstadoTurnoAlumno estado;
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            estado = EstadoTurnoAlumno.CANCELADO;
        } else if (reservadoPorElAlumno) {
            estado = EstadoTurnoAlumno.RESERVADO;
        } else if (lugaresLibres == 0) {
            estado = EstadoTurnoAlumno.COMPLETO;
        } else {
            estado = EstadoTurnoAlumno.DISPONIBLE;
        }
        return new TurnoDisponibleResponse(
                turno.getId(),
                FranjaHorariaService.formatearHora(turno.getHoraInicio()),
                FranjaHorariaService.formatearHora(turno.getHoraFin()),
                lugaresLibres,
                estado,
                estado == EstadoTurnoAlumno.DISPONIBLE && puedeElegir && !faltaMenosDeUnaHora,
                faltaMenosDeUnaHora);
    }
}
