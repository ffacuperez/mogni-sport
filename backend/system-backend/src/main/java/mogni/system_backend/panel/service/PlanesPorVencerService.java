package mogni.system_backend.panel.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.panel.dto.PlanesPorVencerResponse;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;

/**
 * Inicio del personal: planes por vencer. Qué es un plan vigente lo decide {@link PlanAsignado#estaVigente} (HU-266,
 * fecha fin inclusiva), igual que en el listado de alumnos.
 */
@Service
public class PlanesPorVencerService {

    /** Días hacia adelante, incluido hoy, en los que se avisa que un plan vence (hoy y los 6 siguientes). */
    public static final int DIAS_AVISO_VENCIMIENTO_PLAN = 7;

    private static final Comparator<Alumno> POR_APELLIDO_Y_NOMBRE = Comparator
            .comparing(Alumno::getApellido, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Alumno::getNombre, String.CASE_INSENSITIVE_ORDER);

    private final PlanAsignadoRepository planAsignadoRepository;

    public PlanesPorVencerService(PlanAsignadoRepository planAsignadoRepository) {
        this.planAsignadoRepository = planAsignadoRepository;
    }

    @Transactional(readOnly = true)
    public PlanesPorVencerResponse consultar() {
        return consultar(ZonaHoraria.hoy());
    }

    /** Igual que el anterior, con "hoy" como parámetro para poder probarlo con cualquier fecha. */
    @Transactional(readOnly = true)
    public PlanesPorVencerResponse consultar(LocalDate hoy) {
        LocalDate ultimoDiaAviso = hoy.plusDays(DIAS_AVISO_VENCIMIENTO_PLAN - 1L);
        List<PlanesPorVencerResponse.PlanPorVencer> planes = planAsignadoRepository
                .findByEstado(EstadoPlanAsignado.ACTIVO).stream()
                .filter(plan -> plan.estaVigente(hoy))
                .filter(plan -> plan.getAlumno().getCuenta().isActiva())
                // Un plan sin fecha fin (anterior a la regla) no vence solo
                .filter(plan -> plan.getFechaFin() != null && !plan.getFechaFin().isAfter(ultimoDiaAviso))
                .sorted(Comparator.comparing(PlanAsignado::getFechaFin)
                        .thenComparing(PlanAsignado::getAlumno, POR_APELLIDO_Y_NOMBRE))
                .map(plan -> new PlanesPorVencerResponse.PlanPorVencer(
                        plan.getId(),
                        plan.getAlumno().getId(),
                        plan.getAlumno().getNombre(),
                        plan.getAlumno().getApellido(),
                        plan.getNombre(),
                        plan.getFechaFin(),
                        ChronoUnit.DAYS.between(hoy, plan.getFechaFin())))
                .toList();
        return new PlanesPorVencerResponse(hoy, DIAS_AVISO_VENCIMIENTO_PLAN, planes);
    }
}
