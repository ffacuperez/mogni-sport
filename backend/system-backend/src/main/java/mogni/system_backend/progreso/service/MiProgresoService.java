package mogni.system_backend.progreso.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.AccesoDenegadoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.progreso.dto.EjercicioIndicadoResponse;
import mogni.system_backend.progreso.dto.EstadoSesion;
import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.PlanResumenResponse;
import mogni.system_backend.progreso.dto.SemanaPlanResponse;
import mogni.system_backend.progreso.dto.SesionSemanaResponse;
import mogni.system_backend.progreso.entity.RegistroSesion;
import mogni.system_backend.progreso.repository.RegistroSesionRepository;

/**
 * HU-270: el progreso del alumno logueado. El alumno sale siempre de la sesión, así que nunca puede ver los datos
 * de otro. Las semanas se cuentan de a 7 días desde la fecha de asignación y las sesiones del plan se repiten en
 * cada una.
 */
@Service
public class MiProgresoService {

    private final AlumnoRepository alumnoRepository;
    private final PlanAsignadoRepository planRepository;
    private final RegistroSesionRepository registroRepository;

    public MiProgresoService(AlumnoRepository alumnoRepository, PlanAsignadoRepository planRepository,
            RegistroSesionRepository registroRepository) {
        this.alumnoRepository = alumnoRepository;
        this.planRepository = planRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PlanDetalleResponse> obtenerPlanActivo(String email) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        return planRepository.findFirstByAlumnoIdAndEstado(alumno.getId(), EstadoPlanAsignado.ACTIVO)
                .map(plan -> mapearDetalle(plan, ZonaHoraria.hoy()));
    }

    @Transactional(readOnly = true)
    public MisPlanesResponse listarPlanes(String email) {
        Long alumnoId = buscarAlumnoConAcceso(email).getId();
        PlanResumenResponse activo = planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO)
                .map(this::mapearResumen)
                .orElse(null);
        List<PlanResumenResponse> anteriores = planRepository
                .findByAlumnoIdAndEstadoOrderByFechaFinalizacionDescIdDesc(alumnoId, EstadoPlanAsignado.FINALIZADO).stream()
                .map(this::mapearResumen)
                .toList();
        return new MisPlanesResponse(activo, anteriores);
    }

    private Alumno buscarAlumnoConAcceso(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        if (!alumno.getCuenta().isActiva()) {
            throw new AccesoDenegadoException("Tu cuenta está inactiva.");
        }
        return alumno;
    }

    private PlanDetalleResponse mapearDetalle(PlanAsignado plan, LocalDate hoy) {
        int cantidadSemanas = cantidadSemanas(plan);
        List<RegistroSesion> registros = registroRepository.findByPlanId(plan.getId());
        Map<ClaveSesion, RegistroSesion> registrosPorSesion = registros.stream()
                .collect(Collectors.toMap(r -> new ClaveSesion(r.getSemana(), r.getNumeroSesion()), Function.identity()));

        List<SemanaPlanResponse> semanas = IntStream.rangeClosed(1, cantidadSemanas)
                .mapToObj(numero -> mapearSemana(plan, numero, registrosPorSesion))
                .toList();

        return new PlanDetalleResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getMetodo(),
                plan.getMesociclo(),
                plan.getObjetivoMesociclo(),
                plan.getEstado(),
                plan.getFechaAsignacion(),
                fechaFin(plan, cantidadSemanas),
                cantidadSemanas,
                semanaActual(plan, cantidadSemanas, hoy),
                contarRegistradas(plan, cantidadSemanas, registros),
                plan.getSesiones().size() * cantidadSemanas,
                semanas);
    }

    private PlanResumenResponse mapearResumen(PlanAsignado plan) {
        int cantidadSemanas = cantidadSemanas(plan);
        return new PlanResumenResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getMetodo(),
                plan.getMesociclo(),
                plan.getEstado(),
                plan.getFechaAsignacion(),
                fechaFin(plan, cantidadSemanas),
                cantidadSemanas,
                contarRegistradas(plan, cantidadSemanas, registroRepository.findByPlanId(plan.getId())),
                plan.getSesiones().size() * cantidadSemanas);
    }

    /** Si el profe le sacó sesiones al plan, las que ya no existen no suman: "registradas" nunca supera al total. */
    private static int contarRegistradas(PlanAsignado plan, int cantidadSemanas, List<RegistroSesion> registros) {
        return (int) registros.stream()
                .filter(r -> r.getSemana() >= 1 && r.getSemana() <= cantidadSemanas)
                .filter(r -> r.getNumeroSesion() >= 1 && r.getNumeroSesion() <= plan.getSesiones().size())
                .count();
    }

    private SemanaPlanResponse mapearSemana(PlanAsignado plan, int numero, Map<ClaveSesion, RegistroSesion> registros) {
        LocalDate desde = plan.getFechaAsignacion().plusWeeks(numero - 1L);
        return new SemanaPlanResponse(
                numero,
                desde,
                desde.plusDays(6),
                plan.getSesiones().stream()
                        .map(sesion -> mapearSesion(plan, sesion, registros.get(new ClaveSesion(numero, sesion.getNumero()))))
                        .toList());
    }

    private SesionSemanaResponse mapearSesion(PlanAsignado plan, SesionPlan sesion, RegistroSesion registro) {
        EstadoSesion sinRegistro = plan.estaActivo() ? EstadoSesion.PENDIENTE : EstadoSesion.NO_REGISTRADA;
        return new SesionSemanaResponse(
                sesion.getNumero(),
                registro == null ? sinRegistro : EstadoSesion.REGISTRADA,
                registro == null ? null : registro.getFecha(),
                sesion.getEjercicios().stream().map(EjercicioIndicadoResponse::desde).toList());
    }

    /** Un plan sin la cantidad cargada se toma como de una sola semana. */
    private static int cantidadSemanas(PlanAsignado plan) {
        Integer cantidad = plan.getCantidadSemanas();
        return cantidad == null || cantidad < 1 ? 1 : cantidad;
    }

    private static LocalDate fechaFin(PlanAsignado plan, int cantidadSemanas) {
        return plan.estaActivo()
                ? plan.getFechaAsignacion().plusWeeks(cantidadSemanas).minusDays(1)
                : plan.getFechaFinalizacion();
    }

    private static int semanaActual(PlanAsignado plan, int cantidadSemanas, LocalDate hoy) {
        if (!plan.estaActivo()) {
            return cantidadSemanas;
        }
        long semanasCompletas = ChronoUnit.WEEKS.between(plan.getFechaAsignacion(), hoy);
        return Math.clamp(semanasCompletas + 1, 1, cantidadSemanas);
    }

    private record ClaveSesion(int semana, int numeroSesion) {
    }
}
