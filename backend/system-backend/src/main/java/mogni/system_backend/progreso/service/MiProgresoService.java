package mogni.system_backend.progreso.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
import jakarta.validation.Valid;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.progreso.dto.EjercicioIndicadoResponse;
import mogni.system_backend.progreso.dto.EjercicioRealizadoResponse;
import mogni.system_backend.progreso.dto.EstadoSesion;
import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.PlanResumenResponse;
import mogni.system_backend.progreso.dto.RegistroEjercicioRequest;
import mogni.system_backend.progreso.dto.RegistroSesionRequest;
import mogni.system_backend.progreso.dto.RegistroSesionResponse;
import mogni.system_backend.progreso.dto.SemanaPlanResponse;
import mogni.system_backend.progreso.dto.SesionDetalleResponse;
import mogni.system_backend.progreso.dto.SesionRegistradaResponse;
import mogni.system_backend.progreso.dto.SesionSemanaResponse;
import mogni.system_backend.progreso.entity.RegistroEjercicio;
import mogni.system_backend.progreso.entity.RegistroSerie;
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
        LocalDate hoy = ZonaHoraria.hoy();
        return buscarPlanVigente(alumno.getId(), hoy).map(plan -> mapearDetalle(plan, hoy));
    }

    @Transactional(readOnly = true)
    public MisPlanesResponse listarPlanes(String email) {
        Long alumnoId = buscarAlumnoConAcceso(email).getId();
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

    /** 404 también si el plan es de otro alumno: no se revela que existe. */
    @Transactional(readOnly = true)
    public PlanDetalleResponse obtenerPlan(String email, Long planId) {
        return mapearDetalle(buscarPlanDelAlumno(email, planId), ZonaHoraria.hoy());
    }

    @Transactional(readOnly = true)
    public List<SesionRegistradaResponse> listarSesionesRegistradas(String email, Long planId) {
        PlanAsignado plan = buscarPlanDelAlumno(email, planId);
        return registroRepository.findByPlanIdOrderBySemanaDescFechaDescIdDesc(plan.getId()).stream()
                .map(registro -> new SesionRegistradaResponse(
                        registro.getId(),
                        registro.getSemana(),
                        registro.getNumeroSesion(),
                        registro.getFecha(),
                        registro.getEjercicios().size()))
                .toList();
    }

    /** 404 también si la sesión es de otro alumno: no se revela que existe. */
    @Transactional(readOnly = true)
    public SesionDetalleResponse obtenerSesion(String email, Long registroId) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        RegistroSesion registro = registroRepository.findByIdAndPlanAlumnoId(registroId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa sesión"));
        PlanAsignado plan = registro.getPlan();
        return new SesionDetalleResponse(
                registro.getId(),
                registro.getSemana(),
                registro.getNumeroSesion(),
                registro.getFecha(),
                plan.getNombre(),
                plan.getMetodo(),
                plan.getMesociclo(),
                registro.getEjercicios().stream().map(EjercicioRealizadoResponse::desde).toList());
    }

    @Transactional
    public RegistroSesionResponse iniciarSesion(String email, Long planId, RegistroSesionRequest request) {
        PlanAsignado plan = buscarPlanDelAlumno(email, planId);
        if (!plan.estaVigente(ZonaHoraria.hoy())) {
            throw new RegistroInvalidoException("No podés registrar una sesión en un plan que ya no está vigente");
        }
        int semana = request.getSemana();
        int numeroSesion = request.getNumeroSesion();
        if (semana < 1 || semana > cantidadSemanas(plan)) {
            throw new RegistroInvalidoException("La semana indicada no corresponde a este plan");
        }
        if (numeroSesion < 1 || numeroSesion > plan.getSesiones().size()) {
            throw new RegistroInvalidoException("La sesión indicada no existe en este plan");
        }
        return registroRepository.findByPlanId(plan.getId()).stream()
                .filter(registro -> registro.getSemana() == semana && registro.getNumeroSesion() == numeroSesion)
                .findFirst()
                .map(registro -> new RegistroSesionResponse(
                        registro.getId(), registro.getSemana(), registro.getNumeroSesion(), registro.getFecha(), registro.isCompletada()))
                .orElseGet(() -> {
                    SesionPlan sesionPlan = plan.getSesiones().get(numeroSesion - 1);
                    RegistroSesion registro = new RegistroSesion(plan, semana, numeroSesion, ZonaHoraria.hoy());
                    registro.agregarEjercicios(sesionPlan.getEjercicios().stream()
                            .map(indicado -> {
                                RegistroEjercicio ejercicio = new RegistroEjercicio(indicado);
                                ejercicio.agregarSeries(IntStream.rangeClosed(1, indicado.getSeries())
                                        .mapToObj(i -> new RegistroSerie(null, null, null, null))
                                        .toList());
                                return ejercicio;
                            })
                            .toList());
                    RegistroSesion guardado = registroRepository.save(registro);
                    return new RegistroSesionResponse(
                            guardado.getId(), guardado.getSemana(), guardado.getNumeroSesion(), guardado.getFecha(), guardado.isCompletada());
                });
    }

    @Transactional
    public RegistroSesionResponse guardarEjercicio(String email, Long registroId, int orden, RegistroEjercicioRequest request) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        RegistroSesion registro = registroRepository.findByIdAndPlanAlumnoId(registroId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa sesión"));
        RegistroEjercicio ejercicio = registro.getEjercicios().stream()
                .filter(item -> item.getOrden() == orden)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese ejercicio en la sesión"));

        if (request.getSeries() == null || request.getSeries().isEmpty()) {
            throw new RegistroInvalidoException("Debes completar al menos una serie del ejercicio");
        }

        List<RegistroSerie> nuevas = request.getSeries().stream()
                .map(serieRequest -> {
                    RegistroSerie serie = ejercicio.getSeriesRegistradas().stream()
                            .filter(item -> item.getOrden() == serieRequest.getOrden())
                            .findFirst()
                            .orElse(new RegistroSerie(null, null, null, null));
                    serie.actualizar(serieRequest.getPeso(), serieRequest.getRepeticiones(), serieRequest.getRir(), serieRequest.getNota());
                    return serie;
                })
                .toList();

        ejercicio.actualizarSeries(nuevas);
        ejercicio.recalcularEstado();
        registro.setCompletada(registro.getEjercicios().stream().allMatch(RegistroEjercicio::isCompleto));
        registroRepository.save(registro);
        return new RegistroSesionResponse(registro.getId(), registro.getSemana(), registro.getNumeroSesion(), registro.getFecha(), registro.isCompletada());
    }

    @Transactional
    public RegistroSesionResponse completarSesion(String email, Long registroId) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        RegistroSesion registro = registroRepository.findByIdAndPlanAlumnoId(registroId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa sesión"));

        boolean todosCompletos = registro.getEjercicios().stream().allMatch(RegistroEjercicio::isCompleto);
        if (!todosCompletos) {
            throw new RegistroInvalidoException("Antes de cerrar la sesión, completá todos los ejercicios");
        }

        registro.setCompletada(true);
        registroRepository.save(registro);
        return new RegistroSesionResponse(registro.getId(), registro.getSemana(), registro.getNumeroSesion(), registro.getFecha(), true);
    }

    /** HU-266: el plan activo es el ACTIVO de la base, solo si no venció (misma regla que en todos lados). */
    private Optional<PlanAsignado> buscarPlanVigente(Long alumnoId, LocalDate hoy) {
        return planRepository.findFirstByAlumnoIdAndEstado(alumnoId, EstadoPlanAsignado.ACTIVO)
                .filter(plan -> plan.estaVigente(hoy));
    }

    private PlanAsignado buscarPlanDelAlumno(String email, Long planId) {
        Alumno alumno = buscarAlumnoConAcceso(email);
        return planRepository.findByIdAndAlumnoId(planId, alumno.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese plan"));
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
                .mapToObj(numero -> mapearSemana(plan, numero, registrosPorSesion, hoy))
                .toList();

        return new PlanDetalleResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getMetodo(),
                plan.getMesociclo(),
                plan.getObjetivoMesociclo(),
                plan.estadoAl(hoy),
                plan.getFechaAsignacion(),
                fechaFin(plan, cantidadSemanas, hoy),
                cantidadSemanas,
                semanaActual(plan, cantidadSemanas, hoy),
                contarRegistradas(plan, cantidadSemanas, registros),
                plan.getSesiones().size() * cantidadSemanas,
                semanas);
    }

    private PlanResumenResponse mapearResumen(PlanAsignado plan, LocalDate hoy) {
        int cantidadSemanas = cantidadSemanas(plan);
        return new PlanResumenResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getMetodo(),
                plan.getMesociclo(),
                plan.estadoAl(hoy),
                plan.getFechaAsignacion(),
                fechaFin(plan, cantidadSemanas, hoy),
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

    private SemanaPlanResponse mapearSemana(PlanAsignado plan, int numero, Map<ClaveSesion, RegistroSesion> registros,
                                            LocalDate hoy) {
        LocalDate desde = plan.getFechaAsignacion().plusWeeks(numero - 1L);
        LocalDate hasta = desde.plusDays(6);
        if (plan.getFechaFin() != null && plan.getFechaFin().isBefore(hasta)) {
            hasta = plan.getFechaFin();
        }
        return new SemanaPlanResponse(
                numero,
                desde,
                hasta,
                plan.getSesiones().stream()
                        .map(sesion -> mapearSesion(plan, sesion, registros.get(new ClaveSesion(numero, sesion.getNumero())), hoy))
                        .toList());
    }

    private SesionSemanaResponse mapearSesion(PlanAsignado plan, SesionPlan sesion, RegistroSesion registro, LocalDate hoy) {
        EstadoSesion sinRegistro = plan.estaVigente(hoy) ? EstadoSesion.PENDIENTE : EstadoSesion.NO_REGISTRADA;
        return new SesionSemanaResponse(
                sesion.getNumero(),
                registro == null ? sinRegistro : EstadoSesion.REGISTRADA,
                registro == null ? null : registro.getFecha(),
                sesion.getEjercicios().stream().map(EjercicioIndicadoResponse::desde).toList());
    }

    /**
     * La cantidad cargada. Los planes que se asignaron eligiendo una fecha fin suelta no la tienen: para esos, las
     * semanas de 7 días que cubren del inicio al fin inclusive (si sobran días, la última queda incompleta).
     * Sin ninguna de las dos, una sola semana.
     */
    private static int cantidadSemanas(PlanAsignado plan) {
        Integer cantidad = plan.getCantidadSemanas();
        if (cantidad != null && cantidad >= 1) {
            return cantidad;
        }
        if (plan.getFechaFin() != null) {
            long dias = ChronoUnit.DAYS.between(plan.getFechaAsignacion(), plan.getFechaFin()) + 1;
            return (int) Math.max(1, Math.ceilDiv(dias, 7));
        }
        return 1;
    }

    /**
     * Activo: la fecha fin real (HU-266) o, en los planes anteriores a esa regla, el fin previsto por semanas.
     * Terminado: el día en que terminó (finalizado a mano o vencido).
     */
    private static LocalDate fechaFin(PlanAsignado plan, int cantidadSemanas, LocalDate hoy) {
        if (!plan.estaVigente(hoy)) {
            return plan.fechaFinalizacionAl(hoy);
        }
        return plan.getFechaFin() != null
                ? plan.getFechaFin()
                : plan.getFechaAsignacion().plusWeeks(cantidadSemanas).minusDays(1);
    }

    private static int semanaActual(PlanAsignado plan, int cantidadSemanas, LocalDate hoy) {
        if (!plan.estaVigente(hoy)) {
            return cantidadSemanas;
        }
        return Math.clamp(plan.semanaEn(hoy), 1, cantidadSemanas);
    }

    private record ClaveSesion(int semana, int numeroSesion) {
    }
}
