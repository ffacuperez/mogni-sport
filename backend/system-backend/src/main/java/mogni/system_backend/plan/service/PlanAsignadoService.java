package mogni.system_backend.plan.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.dto.AsignarPlanDesdeCeroRequest;
import mogni.system_backend.plan.dto.AsignarPlanDesdePlantillaRequest;
import mogni.system_backend.plan.dto.PlanAsignadoResponse;
import mogni.system_backend.plan.dto.PlanRequest;
import mogni.system_backend.plan.entity.EjercicioSesionPlan;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.dto.EjercicioSesionResponse;
import mogni.system_backend.plantilla.dto.SesionPlantillaResponse;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;
import mogni.system_backend.plantilla.service.CatalogoEjercicios;

/**
 * HU-266: asignar un plan de entrenamiento a un alumno, copiando una plantilla o armándolo desde cero.
 * Un alumno tiene como mucho un plan ACTIVO: para asignarle otro hay que finalizar el actual.
 */
@Service
public class PlanAsignadoService {

    private final PlanAsignadoRepository planRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlantillaPlanRepository plantillaRepository;
    private final CatalogoEjercicios catalogoEjercicios;

    public PlanAsignadoService(PlanAsignadoRepository planRepository, AlumnoRepository alumnoRepository,
                               PlantillaPlanRepository plantillaRepository, EjercicioRepository ejercicioRepository) {
        this.planRepository = planRepository;
        this.alumnoRepository = alumnoRepository;
        this.plantillaRepository = plantillaRepository;
        this.catalogoEjercicios = new CatalogoEjercicios(ejercicioRepository);
    }

    /** El plan activo del alumno, si tiene. Lo usa la pantalla para avisar antes de armar uno nuevo. */
    @Transactional(readOnly = true)
    public Optional<PlanAsignadoResponse> obtenerPlanActivo(Long alumnoId) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId);
        }
        return planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO).map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public PlanAsignadoResponse obtenerPlan(Long id) {
        return mapearAResponse(buscarPlanOLanzarError(id));
    }

    /** Copia profunda de la plantilla: el plan del alumno y la plantilla quedan independientes. */
    @Transactional
    public PlanAsignadoResponse asignarDesdePlantilla(AsignarPlanDesdePlantillaRequest request) {
        Alumno alumno = alumnoSinPlanActivo(request.getAlumnoId());
        PlantillaPlan plantilla = plantillaRepository.findByIdAndActivaTrue(request.getPlantillaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una plantilla con id " + request.getPlantillaId()));

        PlanAsignado plan = planRepository.save(PlanAsignado.copiaDe(plantilla, alumno, ZonaHoraria.hoy()));
        plantillaRepository.sumarAsignacion(plantilla.getId());
        return mapearAResponse(plan);
    }

    /** Plan armado a mano para el alumno; no queda guardado como plantilla. */
    @Transactional
    public PlanAsignadoResponse asignarDesdeCero(AsignarPlanDesdeCeroRequest request) {
        Alumno alumno = alumnoSinPlanActivo(request.getAlumnoId());
        PlanAsignado plan = new PlanAsignado(alumno, ZonaHoraria.hoy());
        aplicarDatos(plan, request, Set.of());
        return mapearAResponse(planRepository.save(plan));
    }

    /** Reemplaza nombre, nivel, objetivo y sesiones del plan. No toca la plantilla de la que salió. */
    @Transactional
    public PlanAsignadoResponse actualizarPlan(Long id, PlanRequest request) {
        PlanAsignado plan = buscarPlanOLanzarError(id);
        if (!plan.estaActivo()) {
            throw new ConflictoException("El plan está finalizado y ya no se puede modificar");
        }
        // Un ejercicio que se dio de baja del catálogo puede quedar en el plan si ya estaba
        Set<Long> ejerciciosQueYaTenia = plan.getSesiones().stream()
                .flatMap(sesion -> sesion.getEjercicios().stream())
                .map(ejercicioSesion -> ejercicioSesion.getEjercicio().getId())
                .collect(Collectors.toSet());
        aplicarDatos(plan, request, ejerciciosQueYaTenia);
        return mapearAResponse(planRepository.save(plan));
    }

    /** Deja al alumno sin plan activo, para poder asignarle otro. */
    @Transactional
    public PlanAsignadoResponse finalizarPlan(Long id) {
        PlanAsignado plan = buscarPlanOLanzarError(id);
        if (!plan.estaActivo()) {
            throw new ConflictoException("El plan ya está finalizado");
        }
        plan.finalizar(ZonaHoraria.hoy());
        if (plan.getPlantillaOrigen() != null) {
            plantillaRepository.restarAsignacion(plan.getPlantillaOrigen().getId());
        }
        return mapearAResponse(planRepository.save(plan));
    }

    // --- Auxiliares ---

    /**
     * El alumno, bloqueado hasta el fin de la transacción: dos asignaciones al mismo alumno a la vez se hacen una
     * detrás de otra, así la segunda ya ve el plan activo de la primera y no quedan dos.
     */
    private Alumno alumnoSinPlanActivo(Long alumnoId) {
        Alumno alumno = alumnoRepository.findByIdConBloqueo(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId));
        if (alumno.getCuenta() != null && !alumno.getCuenta().isActiva()) {
            throw new RegistroInvalidoException("No se puede asignar un plan a un alumno dado de baja");
        }
        planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO).ifPresent(activo -> {
            throw new ConflictoException(alumno.getNombre() + " ya tiene un plan activo (\"" + activo.getNombre()
                    + "\"). Para asignarle uno nuevo, primero finalizá su plan actual");
        });
        return alumno;
    }

    private void aplicarDatos(PlanAsignado plan, PlanRequest request, Set<Long> ejerciciosPermitidosInactivos) {
        if (request.getSesiones() == null || request.getSesiones().isEmpty()) {
            throw new RegistroInvalidoException("El plan tiene que tener al menos una sesión");
        }
        Map<Long, Ejercicio> catalogo = catalogoEjercicios.buscar(request.getSesiones(), ejerciciosPermitidosInactivos);

        plan.setNombre(request.getNombre().trim());
        plan.setNivel(request.getNivel());
        plan.setObjetivo(request.getObjetivo());
        plan.reemplazarSesiones(request.getSesiones().stream()
                .map(sesionRequest -> crearSesion(sesionRequest, catalogo))
                .toList());
    }

    private SesionPlan crearSesion(SesionRequest request, Map<Long, Ejercicio> catalogo) {
        if (request.getEjercicios() == null || request.getEjercicios().isEmpty()) {
            throw new RegistroInvalidoException("Cada sesión tiene que tener al menos un ejercicio");
        }
        SesionPlan sesion = new SesionPlan(CatalogoEjercicios.nombreDeSesion(request));
        sesion.agregarEjercicios(request.getEjercicios().stream()
                .map(e -> new EjercicioSesionPlan(catalogo.get(e.getEjercicioId()), e.getSeries(), e.getRepeticiones(),
                        e.getRepeticionesMax(), e.getDescansoMinutos(), e.getPesoSugerido(), e.getRirObjetivo()))
                .toList());
        return sesion;
    }

    private PlanAsignado buscarPlanOLanzarError(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un plan asignado con id " + id));
    }

    private PlanAsignadoResponse mapearAResponse(PlanAsignado plan) {
        Alumno alumno = plan.getAlumno();
        return new PlanAsignadoResponse(
                plan.getId(),
                alumno.getId(),
                alumno.getNombre() + " " + alumno.getApellido(),
                plan.getNombre(),
                plan.getNivel(),
                plan.getObjetivo(),
                plan.getEstado(),
                plan.getPlantillaOrigen() == null ? null : plan.getPlantillaOrigen().getId(),
                plan.getFechaAsignacion(),
                plan.getFechaFinalizacion(),
                plan.getSesiones().stream().map(this::mapearSesion).toList());
    }

    private SesionPlantillaResponse mapearSesion(SesionPlan sesion) {
        return new SesionPlantillaResponse(
                sesion.getId(),
                sesion.getNumero(),
                sesion.getNombre(),
                sesion.getEjercicios().stream().map(EjercicioSesionResponse::desde).toList());
    }
}
