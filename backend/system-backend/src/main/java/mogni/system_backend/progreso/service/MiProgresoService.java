package mogni.system_backend.progreso.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SemanaPlan;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;
import mogni.system_backend.progreso.dto.EjercicioRealizadoResponse;
import mogni.system_backend.progreso.dto.EjercicioSesionResponse;
import mogni.system_backend.progreso.dto.EstadoSesion;
import mogni.system_backend.progreso.dto.IniciarSesionRequest;
import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.PlanInicioResponse;
import mogni.system_backend.progreso.dto.PlanResumenResponse;
import mogni.system_backend.progreso.dto.RegistrarEjercicioRequest;
import mogni.system_backend.progreso.dto.SemanaPlanResponse;
import mogni.system_backend.progreso.dto.SesionDetalleResponse;
import mogni.system_backend.progreso.dto.SesionRegistradaResponse;
import mogni.system_backend.progreso.dto.SerieRequest;
import mogni.system_backend.progreso.dto.SesionSemanaResponse;
import mogni.system_backend.progreso.entity.RegistroEjercicio;
import mogni.system_backend.progreso.entity.RegistroSerie;
import mogni.system_backend.progreso.entity.RegistroSesion;
import mogni.system_backend.progreso.repository.RegistroSesionRepository;

/**
 * HU-270: el progreso del alumno logueado. El alumno sale siempre de la sesión, así que nunca puede ver los datos
 * de otro. Las semanas se cuentan de a 7 días desde la fecha de asignación y cada una tiene sus propias sesiones.
 * El alumno ve todas las semanas, también las futuras, pero solo registra las de la semana actual. HU-269: el alumno registra las sesiones de la semana actual de su plan activo, ejercicio por ejercicio y
 * en cualquier orden; la sesión queda en curso hasta que la finaliza con todos los ejercicios hechos.
 */
@Service
public class MiProgresoService {

    private final AlumnoRepository alumnoRepository;
    private final PlanAsignadoRepository planRepository;
    private final RegistroSesionRepository registroRepository;
    private final AltaRegistroSesionService altaRegistro;

    public MiProgresoService(AlumnoRepository alumnoRepository, PlanAsignadoRepository planRepository,
            RegistroSesionRepository registroRepository, AltaRegistroSesionService altaRegistro) {
        this.alumnoRepository = alumnoRepository;
        this.planRepository = planRepository;
        this.registroRepository = registroRepository;
        this.altaRegistro = altaRegistro;
    }

    @Transactional(readOnly = true)
    public Optional<PlanDetalleResponse> obtenerPlanActivo(String email) {
        return planActivo(buscarAlumnoConAcceso(email).getId(), true);
    }

    @Transactional(readOnly = true)
    public MisPlanesResponse listarPlanes(String email) {
        return planes(buscarAlumnoConAcceso(email).getId());
    }

    /** 404 también si el plan es de otro alumno: no se revela que existe. */
    @Transactional(readOnly = true)
    public PlanDetalleResponse obtenerPlan(String email, Long planId) {
        return mapearDetalle(buscarPlanDelAlumno(email, planId), ZonaHoraria.hoy(), true);
    }

    @Transactional(readOnly = true)
    public List<SesionRegistradaResponse> listarSesionesRegistradas(String email, Long planId) {
        return sesionesRegistradas(buscarPlanDelAlumno(email, planId));
    }

    /** 404 también si la sesión es de otro alumno: no se revela que existe. */
    @Transactional(readOnly = true)
    public SesionDetalleResponse obtenerSesion(String email, Long registroId) {
        return detalleSesion(buscarAlumnoConAcceso(email).getId(), registroId);
    }

    // MOGNI-268: lo mismo, de un alumno cualquiera y de solo lectura, para el entrenador o el propietario (ver
    // SecurityConfig). 404 si el alumno no existe, o si el plan o la sesión no son de ese alumno.

    @Transactional(readOnly = true)
    public Optional<PlanDetalleResponse> obtenerPlanActivoDeAlumno(Long alumnoId) {
        return planActivo(buscarAlumno(alumnoId).getId(), false);
    }

    @Transactional(readOnly = true)
    public MisPlanesResponse listarPlanesDeAlumno(Long alumnoId) {
        return planes(buscarAlumno(alumnoId).getId());
    }

    @Transactional(readOnly = true)
    public PlanDetalleResponse obtenerPlanDeAlumno(Long alumnoId, Long planId) {
        return mapearDetalle(buscarPlanDe(buscarAlumno(alumnoId), planId), ZonaHoraria.hoy(), false);
    }

    @Transactional(readOnly = true)
    public List<SesionRegistradaResponse> listarSesionesRegistradasDeAlumno(Long alumnoId, Long planId) {
        return sesionesRegistradas(buscarPlanDe(buscarAlumno(alumnoId), planId));
    }

    @Transactional(readOnly = true)
    public SesionDetalleResponse obtenerSesionDeAlumno(Long alumnoId, Long registroId) {
        return detalleSesion(buscarAlumno(alumnoId).getId(), registroId);
    }

    /** registrable: false en la vista del personal, que no registra sesiones por el alumno. */
    private Optional<PlanDetalleResponse> planActivo(Long alumnoId, boolean registrable) {
        LocalDate hoy = ZonaHoraria.hoy();
        return buscarPlanVigente(alumnoId, hoy).map(plan -> mapearDetalle(plan, hoy, registrable));
    }

    private MisPlanesResponse planes(Long alumnoId) {
        LocalDate hoy = ZonaHoraria.hoy();
        Optional<PlanAsignado> activoEnLaBase = planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO);
        PlanResumenResponse activo = activoEnLaBase
                .filter(plan -> plan.estaVigente(hoy))
                .map(plan -> mapearResumen(plan, hoy))
                .orElse(null);
        // HU-266: un plan vencido va a "Anteriores" como finalizado el día de su fecha fin, aunque en la base siga ACTIVO
        List<PlanAsignado> terminados = new ArrayList<>(planRepository
                .findByAlumnoIdAndEstadoOrderByFechaFinalizacionDescIdDesc(alumnoId, EstadoPlanAsignado.FINALIZADO));
        activoEnLaBase.filter(plan -> plan.estaVencido(hoy)).ifPresent(terminados::add);
        List<PlanResumenResponse> anteriores = terminados.stream()
                .sorted(Comparator.comparing((PlanAsignado plan) -> plan.fechaFinalizacionAl(hoy),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlanAsignado::getId, Comparator.reverseOrder()))
                .map(plan -> mapearResumen(plan, hoy))
                .toList();
        return new MisPlanesResponse(activo, anteriores);
    }

    /** Solo las finalizadas, de la más reciente a la más antigua. */
    private List<SesionRegistradaResponse> sesionesRegistradas(PlanAsignado plan) {
        return registroRepository.findByPlanIdAndCompletadaTrueOrderBySemanaDescFechaDescIdDesc(plan.getId()).stream()
                .map(registro -> new SesionRegistradaResponse(
                        registro.getId(),
                        registro.getSemana(),
                        registro.getNumeroSesion(),
                        nombreDeSesion(registro),
                        registro.getFecha(),
                        // La copia de lo indicado se guarda entera al empezar la sesión (HU-269): el total sale de ahí y no
                        // del plan de hoy, que el profe pudo cambiar. Limitación: los registros anteriores a HU-269 solo
                        // guardaron los ejercicios hechos, así que siempre se ven completos.
                        (int) registro.getEjercicios().stream().filter(RegistroEjercicio::isCompleto).count(),
                        registro.getEjercicios().size()))
                .toList();
    }

    private SesionDetalleResponse detalleSesion(Long alumnoId, Long registroId) {
        RegistroSesion registro = registroRepository.findByIdAndPlanAlumnoId(registroId, alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa sesión"));
        PlanAsignado plan = registro.getPlan();
        return new SesionDetalleResponse(
                registro.getId(),
                registro.getSemana(),
                registro.getNumeroSesion(),
                nombreDeSesion(registro),
                registro.getFecha(),
                plan.getNombre(),
                registro.getEjercicios().stream().map(EjercicioRealizadoResponse::desde).toList());
    }

    /** HU-269: empieza una sesión de la semana actual o, si ya estaba empezada, devuelve esa. */
    @Transactional
    public SesionSemanaResponse iniciarSesion(String email, Long planId, IniciarSesionRequest request) {
        PlanAsignado plan = buscarPlanDelAlumno(email, planId);
        LocalDate hoy = ZonaHoraria.hoy();
        int semana = request.getSemana();
        validarSemanaRegistrable(plan, semana, hoy);
        SesionPlan sesion = plan.buscarSesion(semana, request.getNumeroSesion())
                .orElseThrow(() -> new RegistroInvalidoException("Esa sesión no existe en tu plan"));
        RegistroSesion registro = registroRepository
                .findByPlanIdAndSemanaAndNumeroSesion(plan.getId(), semana, sesion.getNumero())
                .orElseGet(() -> crearRegistro(plan, semana, sesion.getNumero(), hoy));
        return mapearSesion(plan, semana, sesion.getNumero(), sesion.getNombre(), sesion.getEjercicios(), registro, hoy, true);
    }

    /**
     * HU-269: carga o corrige un ejercicio de una sesión en curso, en cualquier orden. Tienen que venir todas las
     * series que pide el plan, cada una con peso, repeticiones y RIR.
     */
    @Transactional
    public SesionSemanaResponse registrarEjercicio(String email, Long registroId, int orden,
            RegistrarEjercicioRequest request) {
        LocalDate hoy = ZonaHoraria.hoy();
        RegistroSesion registro = buscarRegistroEnCurso(email, registroId, hoy);
        RegistroEjercicio ejercicio = registro.getEjercicios().stream()
                .filter(e -> e.getOrden() == orden)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese ejercicio en la sesión"));
        ejercicio.registrar(seriesDelEjercicio(ejercicio, request.getSeries()), textoONull(request.getNotas()));
        return mapearRegistro(registro, hoy);
    }

    /** HU-269: la sesión se finaliza recién con todos sus ejercicios registrados; después ya no se modifica. */
    @Transactional
    public SesionSemanaResponse completarSesion(String email, Long registroId) {
        LocalDate hoy = ZonaHoraria.hoy();
        RegistroSesion registro = buscarRegistroEnCurso(email, registroId, hoy);
        if (!registro.todosLosEjerciciosCompletos()) {
            throw new RegistroInvalidoException("Para finalizar la sesión, registrá todos sus ejercicios");
        }
        registro.setCompletada(true);
        return mapearRegistro(registro, hoy);
    }

    /**
     * El alta va en su propia transacción: si otro pedido inició la misma sesión a la vez, la restricción única deja
     * uno solo y se devuelve ese.
     */
    private RegistroSesion crearRegistro(PlanAsignado plan, int semana, int numeroSesion, LocalDate hoy) {
        try {
            altaRegistro.crear(plan.getId(), semana, numeroSesion, hoy);
        } catch (DataIntegrityViolationException e) {
            // Ya la creó el otro pedido: se lee abajo
        }
        return registroRepository.findByPlanIdAndSemanaAndNumeroSesion(plan.getId(), semana, numeroSesion).orElseThrow();
    }

    /** 404 si es de otro alumno; 400 si ya se finalizó o si ya no se puede registrar (plan terminado, otra semana). */
    private RegistroSesion buscarRegistroEnCurso(String email, Long registroId, LocalDate hoy) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        RegistroSesion registro = registroRepository.findByIdAndPlanAlumnoId(registroId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa sesión"));
        if (registro.isCompletada()) {
            throw new RegistroInvalidoException("La sesión ya está finalizada");
        }
        validarSemanaRegistrable(registro.getPlan(), registro.getSemana(), hoy);
        return registro;
    }

    private static void validarSemanaRegistrable(PlanAsignado plan, int semana, LocalDate hoy) {
        if (!plan.estaVigente(hoy)) {
            throw new RegistroInvalidoException("Solo podés registrar sesiones de tu plan activo");
        }
        if (semana != plan.semanaActual(hoy)) {
            throw new RegistroInvalidoException("Solo podés registrar sesiones de la semana actual");
        }
    }

    private static boolean esSemanaRegistrable(PlanAsignado plan, int semana, LocalDate hoy) {
        return plan.estaVigente(hoy) && semana == plan.semanaActual(hoy);
    }

    /** Exactamente las series del plan, numeradas 1..N sin repetir; los datos de cada una ya los validó el request. */
    private static List<RegistroSerie> seriesDelEjercicio(RegistroEjercicio ejercicio, List<SerieRequest> series) {
        List<Integer> ordenes = series.stream().map(SerieRequest::getOrden).sorted().toList();
        if (!ordenes.equals(IntStream.rangeClosed(1, ejercicio.getSeries()).boxed().toList())) {
            throw new RegistroInvalidoException(ejercicio.getSeries() == 1
                    ? "Cargá peso, repeticiones y RIR de la serie del ejercicio"
                    : "Cargá peso, repeticiones y RIR de las " + ejercicio.getSeries() + " series del ejercicio");
        }
        return series.stream()
                .sorted(Comparator.comparing(SerieRequest::getOrden))
                .map(s -> new RegistroSerie(s.getOrden(), s.getPeso(), s.getRepeticiones(), s.getRir()))
                .toList();
    }

    private static String textoONull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** MOGNI-281: el plan vigente del alumno resumido para su inicio (nombre y en qué semana va), o vacío. */
    public Optional<PlanInicioResponse> resumenPlanActivo(Alumno alumno, LocalDate hoy) {
        return buscarPlanVigente(alumno.getId(), hoy).map(plan -> new PlanInicioResponse(plan.getId(),
                plan.getNombre(), plan.semanaActual(hoy), plan.cantidadSemanas()));
    }

    /** HU-266: el plan activo es el ACTIVO de la base, solo si no venció (misma regla que en todos lados). */
    private Optional<PlanAsignado> buscarPlanVigente(Long alumnoId, LocalDate hoy) {
        return planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO)
                .filter(plan -> plan.estaVigente(hoy));
    }

    private PlanAsignado buscarPlanDelAlumno(String email, Long planId) {
        return buscarPlanDe(buscarAlumnoConAcceso(email), planId);
    }

    private PlanAsignado buscarPlanDe(Alumno alumno, Long planId) {
        return planRepository.findByIdAndAlumnoId(planId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese plan"));
    }

    /** MOGNI-268: también los dados de baja, para ver su historial. */
    private Alumno buscarAlumno(Long alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese alumno"));
    }

    private Alumno buscarAlumnoConAcceso(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        if (!alumno.getCuenta().isActiva()) {
            throw new AccesoDenegadoException("Tu cuenta está inactiva.");
        }
        return alumno;
    }

    /** registrable: si el que consulta puede registrar sus sesiones (el alumno sí; el personal, no). */
    private PlanDetalleResponse mapearDetalle(PlanAsignado plan, LocalDate hoy, boolean registrable) {
        int cantidadSemanas = plan.cantidadSemanas();
        List<RegistroSesion> registros = registroRepository.findByPlanId(plan.getId());
        Map<ClaveSesion, RegistroSesion> registrosPorSesion = registros.stream()
                .collect(Collectors.toMap(r -> new ClaveSesion(r.getSemana(), r.getNumeroSesion()), Function.identity()));

        List<SemanaPlanResponse> semanas = IntStream.rangeClosed(1, cantidadSemanas)
                .mapToObj(numero -> mapearSemana(plan, numero, registrosPorSesion, hoy, registrable))
                .toList();

        return new PlanDetalleResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getNivel(),
                plan.getObjetivo(),
                plan.estadoAl(hoy),
                plan.getFechaAsignacion(),
                fechaFin(plan, hoy),
                cantidadSemanas,
                plan.semanaActual(hoy),
                contarRegistradas(plan, registros),
                plan.sesionesTotales(),
                semanas);
    }

    private PlanResumenResponse mapearResumen(PlanAsignado plan, LocalDate hoy) {
        return new PlanResumenResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getNivel(),
                plan.getObjetivo(),
                plan.estadoAl(hoy),
                plan.getFechaAsignacion(),
                fechaFin(plan, hoy),
                plan.cantidadSemanas(),
                contarRegistradas(plan, registroRepository.findByPlanId(plan.getId())),
                plan.sesionesTotales());
    }

    /**
     * Solo las finalizadas (HU-269) cuya semana y sesión siguen existiendo en el plan: "registradas" nunca supera al
     * total.
     */
    private static int contarRegistradas(PlanAsignado plan, List<RegistroSesion> registros) {
        return (int) registros.stream()
                .filter(RegistroSesion::isCompletada)
                .filter(r -> plan.buscarSesion(r.getSemana(), r.getNumeroSesion()).isPresent())
                .count();
    }

    private SemanaPlanResponse mapearSemana(PlanAsignado plan, int numero, Map<ClaveSesion, RegistroSesion> registros,
                                            LocalDate hoy, boolean registrable) {
        LocalDate desde = plan.getFechaAsignacion().plusWeeks(numero - 1L);
        LocalDate hasta = desde.plusDays(6);
        if (plan.getFechaFin().isBefore(hasta)) {
            hasta = plan.getFechaFin();
        }
        Optional<SemanaPlan> semana = plan.buscarSemana(numero);
        return new SemanaPlanResponse(
                numero,
                desde,
                hasta,
                semana.map(SemanaPlan::getNota).orElse(null),
                semana.map(SemanaPlan::getSesiones).orElse(List.of()).stream()
                        .map(sesion -> mapearSesion(plan, numero, sesion.getNumero(), sesion.getNombre(), sesion.getEjercicios(),
                                registros.get(new ClaveSesion(numero, sesion.getNumero())), hoy, registrable))
                        .toList());
    }

    /** El nombre que el profe le puso a esa sesión de esa semana del plan; null si no tiene. */
    private static String nombreDeSesion(RegistroSesion registro) {
        return registro.getPlan().buscarSesion(registro.getSemana(), registro.getNumeroSesion())
                .map(SesionPlan::getNombre)
                .filter(nombre -> !nombre.isBlank())
                .orElse(null);
    }

    /** Una sesión ya empezada; sus ejercicios salen de la copia que se guardó al empezarla. Solo la usa el alumno. */
    private SesionSemanaResponse mapearRegistro(RegistroSesion registro, LocalDate hoy) {
        return mapearSesion(registro.getPlan(), registro.getSemana(), registro.getNumeroSesion(),
                nombreDeSesion(registro), List.of(), registro, hoy, true);
    }

    /**
     * Con registro, los ejercicios salen de la copia guardada (con lo que cargó el alumno); sin registro, de lo que
     * indica hoy el plan. Una sesión sin terminar de un plan que ya terminó cuenta como no registrada.
     */
    private SesionSemanaResponse mapearSesion(PlanAsignado plan, int semana, int numero, String nombre,
            List<? extends EjercicioPrescripto> delPlan, RegistroSesion registro, LocalDate hoy, boolean registrable) {
        boolean vigente = plan.estaVigente(hoy);
        RegistroSesion visible = registro != null && (registro.isCompletada() || vigente) ? registro : null;
        EstadoSesion estado;
        if (visible == null) {
            estado = vigente ? EstadoSesion.PENDIENTE : EstadoSesion.NO_REGISTRADA;
        } else {
            estado = visible.isCompletada() ? EstadoSesion.REGISTRADA : EstadoSesion.EN_CURSO;
        }
        List<EjercicioSesionResponse> ejercicios = visible == null
                ? delPlan.stream().map(EjercicioSesionResponse::indicado).toList()
                : visible.getEjercicios().stream().map(EjercicioSesionResponse::desde).toList();
        return new SesionSemanaResponse(
                numero,
                nombre,
                estado,
                visible == null ? null : visible.getFecha(),
                visible == null ? null : visible.getId(),
                registrable && estado != EstadoSesion.REGISTRADA && esSemanaRegistrable(plan, semana, hoy),
                ejercicios);
    }

    /** Activo: la fecha fin (HU-266). Terminado: el día en que terminó (finalizado a mano o vencido). */
    private static LocalDate fechaFin(PlanAsignado plan, LocalDate hoy) {
        return plan.estaVigente(hoy) ? plan.getFechaFin() : plan.fechaFinalizacionAl(hoy);
    }

    private record ClaveSesion(int semana, int numeroSesion) {
    }
}
