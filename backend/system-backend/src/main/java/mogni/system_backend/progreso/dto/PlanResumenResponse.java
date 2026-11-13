package mogni.system_backend.progreso.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/** HU-270: un plan en el listado de planes del alumno. */
@Getter
@AllArgsConstructor
public class PlanResumenResponse {
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
    private int sesionesRegistradas;
    private int sesionesTotales;
}
