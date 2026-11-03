package mogni.system_backend.plan.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plantilla.dto.SesionPlantillaResponse;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

@Getter
@AllArgsConstructor
public class PlanAsignadoResponse {
    private Long id;
    private Long alumnoId;
    /** Nombre y apellido. */
    private String alumnoNombre;
    private String nombre;
    private NivelPlantilla nivel;
    private ObjetivoPlantilla objetivo;
    /** Un plan vencido figura FINALIZADO aunque nadie lo haya finalizado a mano. */
    private EstadoPlanAsignado estado;
    /** De qué plantilla se copió; null si se armó desde cero. */
    private Long plantillaOrigenId;
    /** Fecha de inicio del plan. */
    private LocalDate fechaAsignacion;
    /** Cuándo terminó: el día que se finalizó o, si venció, su fecha fin. Null mientras está activo. */
    private LocalDate fechaFinalizacion;
    /** HU-266: último día del plan (inclusiva). Null en los planes asignados antes de esta regla. */
    private LocalDate fechaFin;
    /** HU-270: null si el plan no lo tiene cargado. El formulario lo usa para sugerir la fecha fin. */
    private Integer cantidadSemanas;
    /** Mismo formato que las sesiones de una plantilla. */
    private List<SesionPlantillaResponse> sesiones;
}
