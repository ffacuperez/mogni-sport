package mogni.system_backend.inicio.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.inicio.dto.MiInicioResponse;
import mogni.system_backend.progreso.service.MiProgresoService;
import mogni.system_backend.turno.dto.MotivoNoReserva;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.service.ConsultaTurnosService;
import mogni.system_backend.turno.service.MisTurnosService;
import mogni.system_backend.turno.service.ReglasReserva;

/**
 * MOGNI-281: el inicio del alumno logueado. No tiene reglas propias: cada dato sale del servicio que ya lo resuelve
 * (cuotas, turnos, reservas y progreso), así el inicio nunca dice algo distinto que el resto del sistema.
 */
@Service
public class MiInicioService {

    private final AlumnoRepository alumnoRepository;
    private final ReservaRepository reservaRepository;
    private final ClaseExtraRepository claseExtraRepository;
    private final MisTurnosService misTurnosService;
    private final MiProgresoService miProgresoService;

    public MiInicioService(AlumnoRepository alumnoRepository, ReservaRepository reservaRepository,
                           ClaseExtraRepository claseExtraRepository, MisTurnosService misTurnosService,
                           MiProgresoService miProgresoService) {
        this.alumnoRepository = alumnoRepository;
        this.reservaRepository = reservaRepository;
        this.claseExtraRepository = claseExtraRepository;
        this.misTurnosService = misTurnosService;
        this.miProgresoService = miProgresoService;
    }

    @Transactional(readOnly = true)
    public MiInicioResponse consultar(String email) {
        return consultar(email, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    MiInicioResponse consultar(String email, LocalDateTime ahora) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        ConsultaTurnosService.validarAcceso(alumno);
        LocalDate hoy = ahora.toLocalDate();

        MiInicioResponse.Cuota cuota = CuotaService.estadoCuotaDe(alumno, hoy)
                .map(estado -> new MiInicioResponse.Cuota(estado.situacion(), estado.fechaVencimiento(), estado.diasParaVencer()))
                .orElse(new MiInicioResponse.Cuota(null, null, null));
        MiInicioResponse.ProximoTurno proximoTurno = misTurnosService.proximoTurno(alumno, ahora)
                .map(turno -> new MiInicioResponse.ProximoTurno(turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin()))
                .orElse(null);
        int frecuencia = ReglasReserva.frecuenciaSemanal(alumno);
        LocalDate lunes = ReglasReserva.lunesDe(hoy);
        int quedan = ReglasReserva.turnosQueQuedan(frecuencia,
                ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), hoy).size(),
                claseExtraRepository.contarDeLaSemana(alumno.getId(), hoy, lunes));
        // Con dos turnos cancelados la misma semana le pueden quedar más que los de la membresía
        int total = Math.max(frecuencia, quedan);
        int usados = total - quedan;
        MotivoNoReserva motivo = motivoNoReserva(alumno, hoy);

        return new MiInicioResponse(
                hoy,
                alumno.getNombre(),
                alumno.buscarInscripcionActiva().map(inscripcion -> inscripcion.getMembresia().getNombre()).orElse(null),
                frecuencia,
                alumno.getCuenta().isActiva(),
                cuota,
                proximoTurno,
                new MiInicioResponse.TurnosSemana(usados, total),
                motivo == null,
                motivo,
                miProgresoService.resumenPlanActivo(alumno, hoy).orElse(null));
    }

    /**
     * Las mismas reglas que la reserva, para los dos días que se pueden reservar: puede si alguno las cumple. La mora
     * va primero, como en la reserva. Si ninguno las cumple, se informa el motivo que más le sirve al alumno: sin
     * membresía antes que cupo, y cupo antes que "ya tenés turno hoy y mañana".
     */
    private MotivoNoReserva motivoNoReserva(Alumno alumno, LocalDate hoy) {
        if (ReglasReserva.estaEnMora(alumno, hoy)) {
            return MotivoNoReserva.EN_MORA;
        }
        List<MotivoNoReserva> porDia = Stream.of(hoy, hoy.plusDays(1))
                .map(dia -> ReglasReserva.requierePagoPara(alumno, hoy, dia)
                        ? MotivoNoReserva.CUOTA_VENCIDA
                        : ReglasReserva.motivoParaElDia(alumno, dia,
                                ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), dia),
                                () -> claseExtraRepository.existeDisponible(alumno.getId(), dia)).orElse(null))
                .toList();
        if (porDia.contains(null)) {
            return null;
        }
        if (porDia.contains(MotivoNoReserva.SIN_MEMBRESIA_ACTIVA)) {
            return MotivoNoReserva.SIN_MEMBRESIA_ACTIVA;
        }
        if (porDia.contains(MotivoNoReserva.CUPO_SEMANAL_AGOTADO)) {
            return MotivoNoReserva.CUPO_SEMANAL_AGOTADO;
        }
        // Hoy ya tiene turno (o no le quedan) y mañana necesita pagar: pagar es lo que le sirve
        if (porDia.contains(MotivoNoReserva.CUOTA_VENCIDA)) {
            return MotivoNoReserva.CUOTA_VENCIDA;
        }
        return MotivoNoReserva.SIN_DIAS_DISPONIBLES;
    }
}
