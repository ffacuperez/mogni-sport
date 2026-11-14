package mogni.system_backend.progreso.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/** HU-270: el plan como lo ve el alumno, con sus semanas y sesiones. */
@Getter
@AllArgsConstructor
public class PlanDetalleResponse {
    private Long id;
    private String nombre;
    /** MOGNI-268: lo que muestra el historial de planes al entrenador. */
    private NivelPlantilla nivel;
    private ObjetivoPlantilla objetivo;
    private EstadoPlanAsignado estado;
    private LocalDate fechaInicio;
    /** Activo: el fin previsto. Finalizado: el día en que terminó. */
    private LocalDate fechaFin;
    private int cantidadSemanas;
    /** La semana en curso según la fecha de hoy; en un plan finalizado, la última. */
    private int semanaActual;
    private int sesionesRegistradas;
    /** La suma de las sesiones de cada semana. */
    private int sesionesTotales;
    private List<SemanaPlanResponse> semanas;
}
