package mogni.system_backend.turno.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.FranjaHorariaRequest;
import mogni.system_backend.turno.dto.FranjaHorariaResponse;
import mogni.system_backend.turno.dto.HorarioTurnoResponse;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;

/**
 * HU-252: alta, modificación, baja y listado de las franjas horarias semanales.
 * Modificar o eliminar una franja nunca toca los turnos ya generados (no dependen de la franja),
 * por eso se permite siempre, aunque haya reservas.
 */
@Service
public class FranjaHorariaService {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final FranjaHorariaRepository franjaHorariaRepository;

    public FranjaHorariaService(FranjaHorariaRepository franjaHorariaRepository) {
        this.franjaHorariaRepository = franjaHorariaRepository;
    }

    /** Todas las franjas, de lunes a domingo y por hora de inicio. */
    @Transactional(readOnly = true)
    public List<FranjaHorariaResponse> listarFranjas() {
        return franjaHorariaRepository.findAll().stream()
                .sorted(Comparator.comparing(FranjaHoraria::getDiaSemana).thenComparing(FranjaHoraria::getHoraInicio))
                .map(FranjaHorariaService::mapearAResponse)
                .toList();
    }

    @Transactional
    public FranjaHorariaResponse crearFranja(FranjaHorariaRequest request) {
        LocalTime inicio = sinSegundos(request.getHoraInicio());
        LocalTime fin = sinSegundos(request.getHoraFin());
        validar(request.getDiaSemana(), inicio, fin, null);
        FranjaHoraria franja = new FranjaHoraria(request.getDiaSemana(), inicio, fin);
        return mapearAResponse(franjaHorariaRepository.save(franja));
    }

    /** Se puede cambiar también el día. La superposición se valida sin contar la propia franja. */
    @Transactional
    public FranjaHorariaResponse actualizarFranja(Long id, FranjaHorariaRequest request) {
        FranjaHoraria franja = buscarFranjaOLanzarError(id);
        LocalTime inicio = sinSegundos(request.getHoraInicio());
        LocalTime fin = sinSegundos(request.getHoraFin());
        validar(request.getDiaSemana(), inicio, fin, id);
        franja.setDiaSemana(request.getDiaSemana());
        franja.setHoraInicio(inicio);
        franja.setHoraFin(fin);
        return mapearAResponse(franjaHorariaRepository.save(franja));
    }

    /** Borrado físico. Siempre se permite: los turnos ya generados (y sus reservas) se mantienen. */
    @Transactional
    public void eliminarFranja(Long id) {
        franjaHorariaRepository.delete(buscarFranjaOLanzarError(id));
    }

    private void validar(DayOfWeek dia, LocalTime inicio, LocalTime fin, Long idAExcluir) {
        if (!fin.isAfter(inicio)) {
            throw new RegistroInvalidoException("La hora de fin debe ser posterior a la de inicio.");
        }
        if (Duration.between(inicio, fin).toMinutes() % FranjaHoraria.MINUTOS_POR_TURNO != 0) {
            throw new RegistroInvalidoException("La franja tiene que durar horas completas (los turnos son de 1 hora).");
        }
        franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(dia).stream()
                .filter(otra -> !Objects.equals(otra.getId(), idAExcluir))
                .filter(otra -> otra.seSuperponeCon(inicio, fin))
                .findFirst()
                .ifPresent(otra -> {
                    throw new RegistroInvalidoException("Esta franja se superpone con "
                            + formatearHora(otra.getHoraInicio()) + " – " + formatearHora(otra.getHoraFin())
                            + " del " + nombreDia(dia) + ".");
                });
    }

    private FranjaHoraria buscarFranjaOLanzarError(Long id) {
        return franjaHorariaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una franja horaria con id " + id));
    }

    private static LocalTime sinSegundos(LocalTime hora) {
        return hora.truncatedTo(ChronoUnit.MINUTES);
    }

    /** "lunes", "miércoles", ... */
    static String nombreDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, ES_AR);
    }

    /** "08:00" */
    public static String formatearHora(LocalTime hora) {
        return hora.format(HORA);
    }

    private static FranjaHorariaResponse mapearAResponse(FranjaHoraria franja) {
        List<HorarioTurnoResponse> turnos = franja.turnosCalculados().stream()
                .map(t -> new HorarioTurnoResponse(formatearHora(t.horaInicio()), formatearHora(t.horaFin())))
                .toList();
        return new FranjaHorariaResponse(
                franja.getId(),
                franja.getDiaSemana(),
                formatearHora(franja.getHoraInicio()),
                formatearHora(franja.getHoraFin()),
                franja.cantidadTurnos(),
                turnos);
    }
}
