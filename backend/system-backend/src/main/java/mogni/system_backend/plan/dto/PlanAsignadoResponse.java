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
    private EstadoPlanAsignado estado;
    /** De qué plantilla se copió; null si se armó desde cero. */
    private Long plantillaOrigenId;
    private LocalDate fechaAsignacion;
    private LocalDate fechaFinalizacion;
    /** Mismo formato que las sesiones de una plantilla. */
    private List<SesionPlantillaResponse> sesiones;
}
