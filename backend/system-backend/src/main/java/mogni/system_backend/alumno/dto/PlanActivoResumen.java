package mogni.system_backend.alumno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.MetodoPlan;

/** Plan de entrenamiento ACTIVO del alumno, para el listado: lo que arma su título y en qué semana va. */
@Getter
@AllArgsConstructor
public class PlanActivoResumen {
    private Long id;
    private String nombre;
    /** Null en los planes que no lo tienen cargado, igual que el mesociclo. */
    private MetodoPlan metodo;
    private String mesociclo;
    private int semanaActual;
    private int cantidadSemanas;
}
