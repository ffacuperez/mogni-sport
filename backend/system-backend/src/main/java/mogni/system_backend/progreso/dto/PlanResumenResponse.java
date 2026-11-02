package mogni.system_backend.progreso.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.MetodoPlan;

/** HU-270: un plan en el listado de planes del alumno. */
@Getter
@AllArgsConstructor
public class PlanResumenResponse {
    private Long id;
    private String nombre;
    /** Null en los planes que no lo tienen cargado, igual que mesociclo. */
    private MetodoPlan metodo;
    private String mesociclo;
    private EstadoPlanAsignado estado;
    private LocalDate fechaInicio;
    /** Activo: el fin previsto. Finalizado: el día en que terminó. */
    private LocalDate fechaFin;
    private int cantidadSemanas;
    private int sesionesRegistradas;
    private int sesionesTotales;
}
