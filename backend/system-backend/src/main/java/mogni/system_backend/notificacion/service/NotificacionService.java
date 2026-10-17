package mogni.system_backend.notificacion.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.dto.TipoNotificacion;
import mogni.system_backend.notificacion.entity.AvisoTurno;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;

/**
 * HU-250: notificaciones del alumno. Las de cuota se calculan en cada consulta; los avisos de cancelación de turnos
 * se guardan para que sigan disponibles aunque la inscripción o la fecha del turno cambien.
 */
@Service
public class NotificacionService {

    /** Mismo umbral que "Próxima a vencer" en la pantalla de cuotas (HU-46). */
    public static final int DIAS_AVISO_VENCIMIENTO = 5;

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DIA_Y_FECHA = DateTimeFormatter.ofPattern("EEEE dd/MM", ES_AR);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM", ES_AR);

    private final AlumnoRepository alumnoRepository;
    private final CuotaRepository cuotaRepository;
    private final AvisoTurnoRepository avisoTurnoRepository;

    public NotificacionService(AlumnoRepository alumnoRepository, CuotaRepository cuotaRepository,
                               AvisoTurnoRepository avisoTurnoRepository) {
        this.alumnoRepository = alumnoRepository;
        this.cuotaRepository = cuotaRepository;
        this.avisoTurnoRepository = avisoTurnoRepository;
    }

    /** Notificaciones vigentes del alumno logueado, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarDelAlumno(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        LocalDate hoy = ZonaHoraria.hoy();
        List<NotificacionResponse> notificacionesCuotas =
                cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, alumno.getId()).stream()
                .map(cuota -> notificacionDe(cuota, hoy))
                .flatMap(Optional::stream)
                .toList();
        List<NotificacionResponse> notificacionesTurnos = avisoTurnoRepository
                .findByAlumnoIdOrderByFechaDescIdDesc(alumno.getId()).stream()
                .map(this::notificacionDe)
                .toList();
        return java.util.stream.Stream.concat(notificacionesCuotas.stream(), notificacionesTurnos.stream())
                .sorted(Comparator.comparing(NotificacionResponse::getFecha).reversed()
                        .thenComparing(NotificacionResponse::getId))
                .toList();
    }

    private NotificacionResponse notificacionDe(AvisoTurno aviso) {
        return new NotificacionResponse(
                "turno-" + aviso.getId(),
                TipoNotificacion.TURNO_CANCELADO,
                aviso.getTitulo(),
                aviso.getMensaje(),
                aviso.getFecha(),
                null,
                null,
                aviso.getTurnoId(),
                aviso.getFechaTurno());
    }

    /** Notificación vigente de una cuota (vacía si está pagada o falta para su vencimiento). También la usan las push. */
    public Optional<NotificacionResponse> notificacionDe(Cuota cuota, LocalDate hoy) {
        if (cuota.getEstado() == EstadoCuota.PAGADA) {
            return Optional.empty();
        }
        LocalDate vencimiento = cuota.getFechaVencimiento();
        String membresia = cuota.getInscripcion().getMembresia().getNombre();

        // También se mira la fecha, por si la tarea de las 01:00 todavía no la pasó a ATRASADA
        if (cuota.getEstado() == EstadoCuota.ATRASADA || vencimiento.isBefore(hoy)) {
            return Optional.of(new NotificacionResponse(
                    idDe(cuota), TipoNotificacion.CUOTA_VENCIDA,
                    "Tu cuota está vencida",
                    "Tu cuota de " + membresia + " venció el " + vencimiento.format(FECHA)
                            + ". Acercate a recepción para regularizar el pago.",
                    vencimiento.plusDays(1), cuota.getId(), vencimiento));
        }

        long dias = ChronoUnit.DAYS.between(hoy, vencimiento);
        if (dias > DIAS_AVISO_VENCIMIENTO) {
            return Optional.empty();
        }
        return Optional.of(new NotificacionResponse(
                idDe(cuota), TipoNotificacion.CUOTA_POR_VENCER,
                "Tu cuota está por vencer",
                "Tu cuota de " + membresia + " vence " + cuando(dias, vencimiento)
                        + ". Aboná antes de esa fecha para mantener tu membresía activa.",
                vencimiento.minusDays(DIAS_AVISO_VENCIMIENTO), cuota.getId(), vencimiento));
    }

    private static String idDe(Cuota cuota) {
        return "cuota-" + cuota.getId();
    }

    private static String cuando(long dias, LocalDate vencimiento) {
        if (dias == 0) return "hoy";
        if (dias == 1) return "mañana";
        return "el " + vencimiento.format(DIA_Y_FECHA);
    }
}
