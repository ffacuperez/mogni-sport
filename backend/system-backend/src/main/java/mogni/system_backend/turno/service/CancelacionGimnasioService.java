package mogni.system_backend.turno.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.dto.PeriodoCuota;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.notificacion.entity.AvisoTurno;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;
import mogni.system_backend.turno.dto.CancelacionGimnasioResponse;
import mogni.system_backend.turno.dto.CancelacionGimnasioResumenResponse;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

@Service
public class CancelacionGimnasioService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");

    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;
    private final ClaseExtraRepository claseExtraRepository;
    private final AvisoTurnoRepository avisoTurnoRepository;
    private final GeneracionTurnosService generacionTurnosService;
    private final ApplicationEventPublisher eventPublisher;

    public CancelacionGimnasioService(TurnoRepository turnoRepository,
                                      ReservaRepository reservaRepository,
                                      ClaseExtraRepository claseExtraRepository,
                                      AvisoTurnoRepository avisoTurnoRepository,
                                      GeneracionTurnosService generacionTurnosService,
                                      ApplicationEventPublisher eventPublisher) {
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
        this.claseExtraRepository = claseExtraRepository;
        this.avisoTurnoRepository = avisoTurnoRepository;
        this.generacionTurnosService = generacionTurnosService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CancelacionGimnasioResponse cancelarTurno(LocalDate fecha, String horaInicio) {
        return cancelarTurno(fecha, horaInicio, ZonaHoraria.ahora());
    }

    @Transactional
    public CancelacionGimnasioResponse cancelarDia(LocalDate fecha) {
        return cancelarDia(fecha, ZonaHoraria.ahora());
    }

    @Transactional(readOnly = true)
    public CancelacionGimnasioResumenResponse resumenTurno(LocalDate fecha, String horaInicio) {
        return resumenTurno(fecha, horaInicio, ZonaHoraria.ahora());
    }

    CancelacionGimnasioResumenResponse resumenTurno(LocalDate fecha, String horaInicio, LocalDateTime ahora) {
        validarFecha(fecha, ahora);
        Turno turno = generacionTurnosService.obtenerOGenerarTurnos(fecha).stream()
                .filter(candidato -> FranjaHorariaService.formatearHora(candidato.getHoraInicio()).equals(horaInicio))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese turno para la fecha indicada."));
        validarNoIniciado(turno, ahora);
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ConflictoException("Este turno ya fue cancelado por el gimnasio.");
        }
        int reservas = Math.toIntExact(reservaRepository.countByTurnoIdAndEstado(
                turno.getId(), EstadoReserva.RESERVADA));
        return new CancelacionGimnasioResumenResponse(1, reservas);
    }

    @Transactional(readOnly = true)
    public CancelacionGimnasioResumenResponse resumenDia(LocalDate fecha) {
        return resumenDia(fecha, ZonaHoraria.ahora());
    }

    CancelacionGimnasioResumenResponse resumenDia(LocalDate fecha, LocalDateTime ahora) {
        validarFecha(fecha, ahora);
        List<Long> ids = generacionTurnosService.obtenerOGenerarTurnos(fecha).stream()
                .filter(turno -> turno.getEstado() != EstadoTurno.CANCELADO)
                .filter(turno -> fecha.atTime(turno.getHoraInicio()).isAfter(ahora))
                .map(Turno::getId)
                .toList();
        int reservas = ids.isEmpty() ? 0
                : reservaRepository.findByTurnoIdInAndEstado(ids, EstadoReserva.RESERVADA).size();
        return new CancelacionGimnasioResumenResponse(ids.size(), reservas);
    }

    CancelacionGimnasioResponse cancelarTurno(LocalDate fecha, String horaInicio, LocalDateTime ahora) {
        validarFecha(fecha, ahora);
        Turno turno = generacionTurnosService.obtenerOGenerarTurnos(fecha).stream()
                .filter(candidato -> FranjaHorariaService.formatearHora(candidato.getHoraInicio()).equals(horaInicio))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese turno para la fecha indicada."));
        Turno bloqueado = turnoRepository.findByIdConBloqueo(turno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese turno para la fecha indicada."));
        validarNoIniciado(bloqueado, ahora);
        return cancelar(bloqueado, ahora, MotivoCancelacionTurno.MANUAL);
    }

    CancelacionGimnasioResponse cancelarDia(LocalDate fecha, LocalDateTime ahora) {
        validarFecha(fecha, ahora);
        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(fecha);
        int turnosCancelados = 0;
        int reservasCanceladas = 0;
        int clasesOtorgadas = 0;
        for (Turno turno : turnos) {
            if (turno.getEstado() == EstadoTurno.CANCELADO
                    || !fecha.atTime(turno.getHoraInicio()).isAfter(ahora)) {
                continue;
            }
            Turno bloqueado = turnoRepository.findByIdConBloqueo(turno.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un turno del día indicado."));
            if (bloqueado.getEstado() == EstadoTurno.CANCELADO) {
                continue;
            }
            CancelacionGimnasioResponse resultado = cancelar(bloqueado, ahora, MotivoCancelacionTurno.MANUAL);
            turnosCancelados += resultado.getTurnosCancelados();
            reservasCanceladas += resultado.getReservasCanceladas();
            clasesOtorgadas += resultado.getClasesExtraOtorgadas();
        }
        return new CancelacionGimnasioResponse(turnosCancelados, reservasCanceladas, clasesOtorgadas);
    }

    /**
     * Cancela el turno (que ya tiene que venir bloqueado), pasa sus reservas a CANCELADA_POR_GIMNASIO y les da a los
     * alumnos una clase extra y un aviso. Lo usa la cancelación manual y la reconciliación de franjas. La clase extra es
     * la única compensación: la reserva cancelada sigue contando para el límite semanal (ver ReglasReserva).
     * Si la canceló el propietario, el aviso también sale por push (al confirmarse la cancelación, ver
     * PushTurnoCanceladoListener). Por cambio de franja, por ahora, solo queda en la app.
     */
    public CancelacionGimnasioResponse cancelar(Turno turno, LocalDateTime ahora, MotivoCancelacionTurno motivo) {
        validarNoIniciado(turno, ahora);
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ConflictoException("Este turno ya fue cancelado por el gimnasio.");
        }

        List<Reserva> reservas = reservaRepository.findWithAlumnoByTurnoIdInAndEstado(
                List.of(turno.getId()), EstadoReserva.RESERVADA);
        turno.cancelar(motivo);
        turnoRepository.save(turno);
        LocalDate desde = turno.getFecha().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (Reserva reserva : reservas) {
            LocalDate hasta = vigenciaClaseExtra(reserva.getAlumno(), turno.getFecha());
            reserva.setEstado(EstadoReserva.CANCELADA_POR_GIMNASIO);
            claseExtraRepository.save(new ClaseExtra(reserva.getAlumno(), reserva.getId(), desde, hasta));
            AvisoTurno aviso = avisoTurnoRepository.save(new AvisoTurno(
                    reserva.getAlumno(),
                    reserva.getId(),
                    turno.getId(),
                    ahora.toLocalDate(),
                    turno.getFecha(),
                    "Turno cancelado por el gimnasio",
                    "Se canceló tu turno del " + turno.getFecha().format(FECHA)
                            + " a las " + FranjaHorariaService.formatearHora(turno.getHoraInicio())
                            + ". " + textoRecuperacion(hasta, turno.getFecha())));
            Cuenta cuenta = reserva.getAlumno().getCuenta();
            if (motivo == MotivoCancelacionTurno.MANUAL && cuenta != null) {
                eventPublisher.publishEvent(new TurnoCanceladoPorGimnasioEvent(
                        cuenta.getId(), aviso.getId(), aviso.getTitulo(), aviso.getMensaje()));
            }
        }
        reservaRepository.saveAll(reservas);
        return new CancelacionGimnasioResponse(1, reservas.size(), reservas.size());
    }

    /**
     * La clase a recuperar vale desde el lunes de la semana del turno hasta el fin de la cuota en la que estaba el turno
     * cancelado: es la clase de ese período que no pudo hacer. Aunque ya haya pagado el período siguiente, no pasa a
     * ese período. Si ninguna cuota cubre el día del turno (la cuota ya había vencido), vale solo ese día. Sin cuotas
     * (datos viejos), la semana del turno y la siguiente.
     */
    static LocalDate vigenciaClaseExtra(Alumno alumno, LocalDate fechaTurno) {
        if (alumno.buscarUltimaCuotaPagada().isEmpty()) {
            return fechaTurno.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(13);
        }
        return CuotaService.periodoQueCubre(alumno, fechaTurno).map(PeriodoCuota::hasta).orElse(fechaTurno);
    }

    /**
     * Cómo se le cuenta al alumno que puede recuperar la clase. Si la cuota ya venció antes del turno, la recuperación
     * vale solo ese día (ver vigenciaClaseExtra): decirle "hasta que venza tu cuota" lo confundiría.
     */
    static String textoRecuperacion(LocalDate hasta, LocalDate fechaTurno) {
        return hasta.equals(fechaTurno)
                ? "Podés recuperar la clase en otro horario de ese día."
                : "Podés recuperar la clase cualquier día hasta que venza tu cuota.";
    }

    private static void validarFecha(LocalDate fecha, LocalDateTime ahora) {
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new ConflictoException("No se puede cancelar un turno de una fecha que ya pasó.");
        }
    }

    private static void validarNoIniciado(Turno turno, LocalDateTime ahora) {
        if (!turno.getFecha().atTime(turno.getHoraInicio()).isAfter(ahora)) {
            throw new ConflictoException("Este turno ya empezó o ya pasó y no se puede cancelar.");
        }
    }
}
