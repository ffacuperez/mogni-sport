package mogni.system_backend.plan.service;

import java.time.LocalDate;
import java.util.List;
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
 * HU-266: asignar un plan de entrenamiento a un alumno, copiando una plantilla o armándolo desde cero, por una
 * cantidad de semanas (de ahí sale la fecha fin). Un alumno tiene como mucho un plan activo (ACTIVO y sin vencer, ver {@link PlanAsignado#estaVigente}): para
 * asignarle otro hay que finalizar el actual o esperar a que venza.
 * Los métodos públicos usan el día de hoy en Argentina; los que reciben "hoy" son para probarlos con cualquier fecha.
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
        return obtenerPlanActivo(alumnoId, ZonaHoraria.hoy());
    }

    public Optional<PlanAsignadoResponse> obtenerPlanActivo(Long alumnoId, LocalDate hoy) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId);
        }
        return buscarPlanVigente(alumnoId, hoy).map(plan -> mapearAResponse(plan, hoy));
    }

    @Transactional(readOnly = true)
    public PlanAsignadoResponse obtenerPlan(Long id) {
        return mapearAResponse(buscarPlanOLanzarError(id), ZonaHoraria.hoy());
    }

    /** Copia profunda de la plantilla: el plan del alumno y la plantilla quedan independientes. */
    @Transactional
    public PlanAsignadoResponse asignarDesdePlantilla(AsignarPlanDesdePlantillaRequest request) {
        return asignarDesdePlantilla(request, ZonaHoraria.hoy());
    }

    public PlanAsignadoResponse asignarDesdePlantilla(AsignarPlanDesdePlantillaRequest request, LocalDate hoy) {
        validarSemanas(request.getCantidadSemanas(), 1);
        Alumno alumno = alumnoSinPlanActivo(request.getAlumnoId(), hoy);
        PlantillaPlan plantilla = plantillaRepository.findByIdAndActivaTrue(request.getPlantillaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una plantilla con id " + request.getPlantillaId()));

        PlanAsignado copia = PlanAsignado.copiaDe(plantilla, alumno, hoy);
        copia.definirDuracion(request.getCantidadSemanas());
        PlanAsignado plan = planRepository.save(copia);
        plantillaRepository.sumarAsignacion(plantilla.getId());
        return mapearAResponse(plan, hoy);
    }

    /** Plan armado a mano para el alumno; no queda guardado como plantilla. */
    @Transactional
    public PlanAsignadoResponse asignarDesdeCero(AsignarPlanDesdeCeroRequest request) {
        return asignarDesdeCero(request, ZonaHoraria.hoy());
    }

    public PlanAsignadoResponse asignarDesdeCero(AsignarPlanDesdeCeroRequest request, LocalDate hoy) {
        validarSemanas(request.getCantidadSemanas(), 1);
        Alumno alumno = alumnoSinPlanActivo(request.getAlumnoId(), hoy);
        PlanAsignado plan = new PlanAsignado(alumno, hoy);
        aplicarDatos(plan, request, Set.of());
        plan.definirDuracion(request.getCantidadSemanas());
        return mapearAResponse(planRepository.save(plan), hoy);
    }

    /**
     * Reemplaza nombre, nivel, objetivo, sesiones y semanas (y con ellas la fecha fin) del plan. No toca la plantilla de la que salió.
     * Solo el plan activo: uno finalizado o vencido ya no se modifica.
     */
    @Transactional
    public PlanAsignadoResponse actualizarPlan(Long id, PlanRequest request) {
        return actualizarPlan(id, request, ZonaHoraria.hoy());
    }

    public PlanAsignadoResponse actualizarPlan(Long id, PlanRequest request, LocalDate hoy) {
        PlanAsignado plan = buscarPlanOLanzarError(id);
        if (!plan.estaVigente(hoy)) {
            throw new ConflictoException("El plan está finalizado y ya no se puede modificar");
        }
        // La semana en curso no se puede recortar: el plan termina, como pronto, al final de esta semana
        validarSemanas(request.getCantidadSemanas(), plan.semanaEn(hoy));
        // Un ejercicio que se dio de baja del catálogo puede quedar en el plan si ya estaba
        Set<Long> ejerciciosQueYaTenia = plan.getSesiones().stream()
                .flatMap(sesion -> sesion.getEjercicios().stream())
                .map(ejercicioSesion -> ejercicioSesion.getEjercicio().getId())
                .collect(Collectors.toSet());
        aplicarDatos(plan, request, ejerciciosQueYaTenia);
        plan.definirDuracion(request.getCantidadSemanas());
        return mapearAResponse(planRepository.save(plan), hoy);
    }

    /** Deja al alumno sin plan activo, para poder asignarle otro. */
    @Transactional
    public PlanAsignadoResponse finalizarPlan(Long id) {
        return finalizarPlan(id, ZonaHoraria.hoy());
    }

    public PlanAsignadoResponse finalizarPlan(Long id, LocalDate hoy) {
        PlanAsignado plan = buscarPlanOLanzarError(id);
        if (!plan.estaVigente(hoy)) {
            throw new ConflictoException("El plan ya está finalizado");
        }
        return mapearAResponse(finalizar(plan, hoy), hoy);
    }

    /**
     * Al dar de baja a un alumno, su plan activo (si tiene) se finaliza. Si ya estaba vencido, queda cerrado con su
     * fecha fin. Corre en la transacción de la baja.
     */
    @Transactional
    public void finalizarPlanActivoDe(Long alumnoId) {
        LocalDate hoy = ZonaHoraria.hoy();
        planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO)
                .ifPresent(plan -> cerrar(plan, hoy));
    }

    /**
     * Puesta al día: los planes vencidos (ACTIVO con la fecha fin ya pasada) quedan FINALIZADO con su fecha fin y
     * liberan la plantilla. No cambia lo que se ve, que ya se calcula con la fecha; deja prolijos la base y el
     * contador de asignaciones de la plantilla (que no deja eliminarla). Es idempotente. Devuelve cuántos cerró.
     */
    @Transactional
    public int cerrarVencidos(LocalDate hoy) {
        List<PlanAsignado> vencidos = planRepository.findByEstado(EstadoPlanAsignado.ACTIVO).stream()
                .filter(plan -> plan.estaVencido(hoy))
                .toList();
        vencidos.forEach(plan -> finalizar(plan, plan.getFechaFin()));
        return vencidos.size();
    }

    // --- Auxiliares ---

    /** El único plan activo del alumno: el ACTIVO de la base, solo si no venció. */
    private Optional<PlanAsignado> buscarPlanVigente(Long alumnoId, LocalDate hoy) {
        return planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO)
                .filter(plan -> plan.estaVigente(hoy));
    }

    /** Un plan vencido terminó el día de su fecha fin; uno vigente, hoy. */
    private void cerrar(PlanAsignado plan, LocalDate hoy) {
        finalizar(plan, plan.estaVencido(hoy) ? plan.getFechaFin() : hoy);
    }

    private PlanAsignado finalizar(PlanAsignado plan, LocalDate fecha) {
        plan.finalizar(fecha);
        if (plan.getPlantillaOrigen() != null) {
            plantillaRepository.restarAsignacion(plan.getPlantillaOrigen().getId());
        }
        return planRepository.save(plan);
    }

    /** Entre el mínimo (1 al asignar, la semana en curso al modificar) y un año. */
    private static void validarSemanas(Integer semanas, int minimo) {
        if (semanas == null) {
            throw new RegistroInvalidoException("La cantidad de semanas es obligatoria");
        }
        if (semanas < 1) {
            throw new RegistroInvalidoException("El plan tiene que durar al menos una semana");
        }
        if (semanas < minimo) {
            throw new RegistroInvalidoException("El plan ya va por la semana " + minimo
                    + ": no puede durar menos de " + minimo + " semanas");
        }
        if (semanas > PlanAsignado.MAXIMO_SEMANAS) {
            throw new RegistroInvalidoException("El plan no puede durar más de 52 semanas");
        }
    }

    /**
     * El alumno, bloqueado hasta el fin de la transacción: dos asignaciones al mismo alumno a la vez se hacen una
     * detrás de otra, así la segunda ya ve el plan activo de la primera y no quedan dos. Si su plan venció, se cierra
     * acá (con su fecha fin), para que nunca haya dos planes ACTIVO del mismo alumno en la base.
     */
    private Alumno alumnoSinPlanActivo(Long alumnoId, LocalDate hoy) {
        Alumno alumno = alumnoRepository.findByIdConBloqueo(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId));
        if (alumno.getCuenta() != null && !alumno.getCuenta().isActiva()) {
            throw new RegistroInvalidoException("No se puede asignar un plan a un alumno dado de baja");
        }
        planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO).ifPresent(activo -> {
            if (activo.estaVencido(hoy)) {
                finalizar(activo, activo.getFechaFin());
                return;
            }
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

    /** Estado y fecha de finalización como los ve la gente: un plan vencido figura FINALIZADO el día de su fecha fin. */
    private PlanAsignadoResponse mapearAResponse(PlanAsignado plan, LocalDate hoy) {
        Alumno alumno = plan.getAlumno();
        return new PlanAsignadoResponse(
                plan.getId(),
                alumno.getId(),
                alumno.getNombre() + " " + alumno.getApellido(),
                plan.getNombre(),
                plan.getNivel(),
                plan.getObjetivo(),
                plan.estadoAl(hoy),
                plan.getPlantillaOrigen() == null ? null : plan.getPlantillaOrigen().getId(),
                plan.getFechaAsignacion(),
                plan.fechaFinalizacionAl(hoy),
                plan.getFechaFin(),
                plan.getCantidadSemanas(),
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
