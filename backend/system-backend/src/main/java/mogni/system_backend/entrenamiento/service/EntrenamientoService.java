package mogni.system_backend.entrenamiento.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.entrenamiento.dto.EntrenamientoRequest;
import mogni.system_backend.entrenamiento.dto.MiPlanEntrenamientoResponse;
import mogni.system_backend.entrenamiento.entity.EjercicioEntrenamiento;
import mogni.system_backend.entrenamiento.entity.Entrenamiento;
import mogni.system_backend.entrenamiento.entity.EstadoEntrenamiento;
import mogni.system_backend.entrenamiento.entity.SerieEntrenamiento;
import mogni.system_backend.entrenamiento.repository.EntrenamientoRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.entity.EjercicioSesionPlan;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;

@Service
public class EntrenamientoService {

    private final AlumnoRepository alumnoRepository;
    private final PlanAsignadoRepository planRepository;
    private final EntrenamientoRepository entrenamientoRepository;

    public EntrenamientoService(AlumnoRepository alumnoRepository, PlanAsignadoRepository planRepository,
            EntrenamientoRepository entrenamientoRepository) {
        this.alumnoRepository = alumnoRepository;
        this.planRepository = planRepository;
        this.entrenamientoRepository = entrenamientoRepository;
    }

    public Long obtenerAlumnoIdPorEmail(String email) {
        return alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con email " + email))
                .getId();
    }

    @Transactional(readOnly = true)
    public MiPlanEntrenamientoResponse obtenerMiPlan(Long alumnoId) {
        PlanAsignado plan = planRepository.findFirstByAlumnoIdAndEstado(alumnoId, mogni.system_backend.plan.entity.EstadoPlanAsignado.ACTIVO)
                .orElse(null);
        if (plan == null) {
            return new MiPlanEntrenamientoResponse(null, null, false, List.of());
        }

        Map<Long, Entrenamiento> entrenamientosPorSesion = entrenamientoRepository.findByAlumnoAndPlan(alumnoId, plan.getId())
                .stream()
                .collect(Collectors.toMap(e -> e.getSesion().getId(), e -> e, (actual, siguiente) -> siguiente));

        List<MiPlanEntrenamientoResponse.SesionEntrenamientoResponse> sesiones = plan.getSesiones().stream()
                .map(sesion -> mapearSesion(sesion, entrenamientosPorSesion.get(sesion.getId())))
                .toList();

        return new MiPlanEntrenamientoResponse(plan.getId(), plan.getNombre(), true, sesiones);
    }

    @Transactional
    public MiPlanEntrenamientoResponse iniciarEntrenamiento(Long alumnoId, Long sesionId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId));

        PlanAsignado plan = planRepository.findFirstByAlumnoIdAndEstado(alumnoId,
                mogni.system_backend.plan.entity.EstadoPlanAsignado.ACTIVO)
                .orElseThrow(() -> new RegistroInvalidoException("El alumno no tiene un plan activo asignado"));

        SesionPlan sesion = plan.getSesiones().stream()
                .filter(s -> Objects.equals(s.getId(), sesionId))
                .findFirst()
                .orElseThrow(() -> new RegistroInvalidoException("La sesión no pertenece al plan activo del alumno"));

        Entrenamiento entrenamientoExistente = entrenamientoRepository.findFirstByAlumnoAndSesionAndEstado(alumnoId, sesionId,
                EstadoEntrenamiento.EN_CURSO)
                .orElseGet(() -> entrenamientoRepository.findByAlumnoAndSesion(alumnoId, sesionId).stream()
                        .filter(e -> e.getEstado() == EstadoEntrenamiento.COMPLETA)
                        .findFirst()
                        .orElse(null));

        if (entrenamientoExistente != null) {
            return obtenerMiPlan(alumnoId);
        }

        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setAlumno(alumno);
        entrenamiento.setPlan(plan);
        entrenamiento.setSesion(sesion);
        entrenamiento.setEstado(EstadoEntrenamiento.EN_CURSO);

        for (EjercicioSesionPlan ejercicioPlan : sesion.getEjercicios()) {
            EjercicioEntrenamiento ejercicio = new EjercicioEntrenamiento();
            ejercicio.setEntrenamiento(entrenamiento);
            ejercicio.setEjercicioPlan(ejercicioPlan);
            ejercicio.setNota(null);
            for (int i = 1; i <= ejercicioPlan.getSeries(); i++) {
                SerieEntrenamiento serie = new SerieEntrenamiento();
                serie.setEjercicio(ejercicio);
                serie.setNumero(i);
                ejercicio.getSeries().add(serie);
            }
            ejercicio.actualizarEstado();
            entrenamiento.getEjercicios().add(ejercicio);
        }

        entrenamientoRepository.save(entrenamiento);
        return obtenerMiPlan(alumnoId);
    }

    @Transactional
    public MiPlanEntrenamientoResponse guardarEntrenamiento(Long alumnoId, Long entrenamientoId, EntrenamientoRequest request) {
        Entrenamiento entrenamiento = entrenamientoRepository.findById(entrenamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un entrenamiento con id " + entrenamientoId));

        if (!Objects.equals(entrenamiento.getAlumno().getId(), alumnoId)) {
            throw new RegistroInvalidoException("No podés modificar el entrenamiento de otro alumno");
        }

        if (entrenamiento.getEstado() == EstadoEntrenamiento.COMPLETA) {
            throw new RegistroInvalidoException("El entrenamiento ya está completado y no se puede modificar");
        }

        Map<Long, EjercicioEntrenamiento> ejerciciosPorId = entrenamiento.getEjercicios().stream()
                .collect(Collectors.toMap(EjercicioEntrenamiento::getId, e -> e));

        for (EntrenamientoRequest.EjercicioEntrenamientoRequest ejercicioRequest : request.getEjercicios()) {
            EjercicioEntrenamiento ejercicio = ejerciciosPorId.get(ejercicioRequest.getId());
            if (ejercicio == null) {
                throw new RecursoNoEncontradoException("No existe el ejercicio de entrenamiento con id " + ejercicioRequest.getId());
            }

            ejercicio.setNota(ejercicioRequest.getNota());
            Map<Integer, SerieEntrenamiento> seriesPorNumero = ejercicio.getSeries().stream()
                    .collect(Collectors.toMap(SerieEntrenamiento::getNumero, s -> s));

            if (ejercicioRequest.getSeries() != null) {
                for (EntrenamientoRequest.SerieEntrenamientoRequest serieRequest : ejercicioRequest.getSeries()) {
                    SerieEntrenamiento serie = seriesPorNumero.get(serieRequest.getNumero());
                    if (serie == null) {
                        throw new RegistroInvalidoException("La serie " + serieRequest.getNumero() + " no existe para ese ejercicio");
                    }
                    serie.setPeso(serieRequest.getPeso());
                    serie.setRepeticiones(serieRequest.getRepeticiones());
                    serie.setRirRealizado(serieRequest.getRirRealizado());
                    serie.recalcularRmEstimado();
                }
            }

            ejercicio.actualizarEstado();
        }

        entrenamiento.setEstado(EstadoEntrenamiento.EN_CURSO);
        entrenamientoRepository.save(entrenamiento);
        return obtenerMiPlan(alumnoId);
    }

    @Transactional
    public MiPlanEntrenamientoResponse finalizarEntrenamiento(Long alumnoId, Long entrenamientoId) {
        Entrenamiento entrenamiento = entrenamientoRepository.findById(entrenamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un entrenamiento con id " + entrenamientoId));

        if (!Objects.equals(entrenamiento.getAlumno().getId(), alumnoId)) {
            throw new RegistroInvalidoException("No podés finalizar el entrenamiento de otro alumno");
        }

        boolean hayEjercicioIncompleto = entrenamiento.getEjercicios().stream()
                .anyMatch(ejercicio -> ejercicio.getSeries() == null || ejercicio.getSeries().isEmpty()
                        || ejercicio.getSeries().stream().anyMatch(serie -> serie.getPeso() == null
                                || serie.getPeso().compareTo(BigDecimal.ZERO) <= 0
                                || serie.getRepeticiones() == null || serie.getRepeticiones() <= 0
                                || serie.getRirRealizado() == null || serie.getRirRealizado() < 0));

        if (hayEjercicioIncompleto) {
            throw new RegistroInvalidoException("No se puede finalizar la sesión porque hay ejercicios o series incompletas");
        }

        entrenamiento.getEjercicios().forEach(EjercicioEntrenamiento::actualizarEstado);
        if (entrenamiento.getEjercicios().stream().anyMatch(e -> e.getEstado() != mogni.system_backend.entrenamiento.entity.EstadoEjercicioEntrenamiento.COMPLETO)) {
            throw new RegistroInvalidoException("No se puede finalizar la sesión porque hay ejercicios pendientes");
        }

        entrenamiento.completar();
        entrenamientoRepository.save(entrenamiento);
        return obtenerMiPlan(alumnoId);
    }

    private MiPlanEntrenamientoResponse.SesionEntrenamientoResponse mapearSesion(SesionPlan sesion, Entrenamiento entrenamiento) {
        String estado = "Pendiente";
        Long entrenamientoId = null;
        if (entrenamiento != null) {
            entrenamientoId = entrenamiento.getId();
            estado = entrenamiento.getEstado() == EstadoEntrenamiento.COMPLETA ? "Completa" : "En curso";
        }

        List<MiPlanEntrenamientoResponse.EjercicioEntrenamientoResponse> ejercicios = sesion.getEjercicios().stream()
                .map(ejercicioPlan -> mapearEjercicio(ejercicioPlan, entrenamiento != null ? buscarEjercicio(entrenamiento, ejercicioPlan.getId()) : null))
                .toList();

        return new MiPlanEntrenamientoResponse.SesionEntrenamientoResponse(sesion.getId(), entrenamientoId, sesion.getNumero(),
                sesion.getNombre() == null ? "" : sesion.getNombre(), estado, ejercicios);
    }

    private MiPlanEntrenamientoResponse.EjercicioEntrenamientoResponse mapearEjercicio(EjercicioSesionPlan ejercicioPlan,
            EjercicioEntrenamiento ejercicioEntrenamiento) {
        List<MiPlanEntrenamientoResponse.SerieEntrenamientoResponse> series = IntStream.rangeClosed(1, ejercicioPlan.getSeries())
                .mapToObj(numero -> {
                    SerieEntrenamiento serie = ejercicioEntrenamiento != null ? ejercicioEntrenamiento.getSeries().stream()
                            .filter(s -> s.getNumero() == numero)
                            .findFirst()
                            .orElse(null) : null;
                    return new MiPlanEntrenamientoResponse.SerieEntrenamientoResponse(
                            numero,
                            serie != null ? serie.getPeso() : null,
                            serie != null ? serie.getRepeticiones() : null,
                            serie != null ? serie.getRirRealizado() : null,
                            serie != null ? serie.getRmEstimado() : null);
                })
                .toList();

        String nota = ejercicioEntrenamiento != null ? ejercicioEntrenamiento.getNota() : "";
        String estado = ejercicioEntrenamiento != null
                ? ejercicioEntrenamiento.getEstado().name().equals("COMPLETO") ? "Completo" : "Pendiente"
                : "Pendiente";

        String repeticionesObjetivo = ejercicioPlan.getRepeticionesMax() != null
                ? ejercicioPlan.getRepeticiones() + "-" + ejercicioPlan.getRepeticionesMax()
                : String.valueOf(ejercicioPlan.getRepeticiones());
        String descanso = ejercicioPlan.getDescansoMinutos() != null
                ? ejercicioPlan.getDescansoMinutos().toPlainString() + " min"
                : "Sin dato";

        return new MiPlanEntrenamientoResponse.EjercicioEntrenamientoResponse(
                ejercicioEntrenamiento != null ? ejercicioEntrenamiento.getId() : null,
                ejercicioPlan.getId(),
                ejercicioPlan.getEjercicio().getNombre(),
                ejercicioPlan.getSeries(),
                repeticionesObjetivo,
                ejercicioPlan.getRirObjetivo(),
                descanso,
                nota,
                estado,
                series);
    }

    private EjercicioEntrenamiento buscarEjercicio(Entrenamiento entrenamiento, Long ejercicioPlanId) {
        return entrenamiento.getEjercicios().stream()
                .filter(e -> Objects.equals(e.getEjercicioPlan().getId(), ejercicioPlanId))
                .findFirst()
                .orElse(null);
    }
}
