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

/**
 * HU-250: notificaciones de cuota del alumno. No se guardan en la base: se calculan en cada consulta a partir
 * del estado de sus cuotas. Así una cuota impaga siempre tiene su notificación (y una sola), y cuando la cuota
 * pasa a PAGADA la notificación deja de existir sin que nadie tenga que borrarla.
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

    public NotificacionService(AlumnoRepository alumnoRepository, CuotaRepository cuotaRepository) {
        this.alumnoRepository = alumnoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    /** Notificaciones vigentes del alumno logueado, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarDelAlumno(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        LocalDate hoy = ZonaHoraria.hoy();
        return cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, alumno.getId()).stream()
                .map(cuota -> notificacionDe(cuota, hoy))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(NotificacionResponse::getFecha).reversed())
                .toList();
    }

    private Optional<NotificacionResponse> notificacionDe(Cuota cuota, LocalDate hoy) {
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
