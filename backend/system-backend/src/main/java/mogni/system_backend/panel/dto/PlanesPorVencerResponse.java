package mogni.system_backend.panel.dto;

import java.time.LocalDate;
import java.util.List;

import mogni.system_backend.plan.entity.MetodoPlan;

/**
 * Inicio del personal: planes vigentes de alumnos activos que vencen pronto.
 *
 * @param diasAviso cuántos días hacia adelante (incluido hoy) se avisa que un plan vence
 * @param planes    los que vencen en esos días, por fecha fin
 */
public record PlanesPorVencerResponse(LocalDate fecha, int diasAviso, List<PlanPorVencer> planes) {

    /**
     * nombre, metodo y mesociclo: con ellos el frontend arma el título del plan igual que en el resto de las pantallas.
     *
     * @param diasParaVencer 0 si vence hoy (la fecha fin es inclusiva: hoy todavía es vigente)
     */
    public record PlanPorVencer(Long planId, Long alumnoId, String nombreAlumno, String apellidoAlumno,
                                String nombre, MetodoPlan metodo, String mesociclo, LocalDate fechaFin,
                                long diasParaVencer) {
    }
}
