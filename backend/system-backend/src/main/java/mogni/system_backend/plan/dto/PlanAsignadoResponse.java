package mogni.system_backend.plan.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plantilla.dto.SemanaResponse;
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
    /** Fecha de inicio del plan: el primer día de la semana 1. */
    private LocalDate fechaAsignacion;
    /** Cuándo terminó: el día que se finalizó o, si venció, su fecha fin. Null mientras está activo. */
    private LocalDate fechaFinalizacion;
    /** HU-266: último día del plan (inclusiva): el último día de la última semana. */
    private LocalDate fechaFin;
    private int cantidadSemanas;
    /**
     * La semana en curso, o null si el plan ya no está vigente. Al modificar, las anteriores son de solo lectura y
     * esta no se puede eliminar.
     */
    private Integer semanaEnCurso;
    /** Mismo formato que las semanas de una plantilla. */
    private List<SemanaResponse> semanas;
    /** Las sesiones de la semana en curso que el alumno ya empezó o registró: en el editor son de solo lectura. */
    private List<SesionConRegistroResponse> sesionesConRegistro;
}
