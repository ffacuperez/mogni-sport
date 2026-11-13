package mogni.system_backend.alumno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Plan de entrenamiento ACTIVO del alumno, para el listado: su nombre y en qué semana va. */
@Getter
@AllArgsConstructor
public class PlanActivoResumen {
    private Long id;
    private String nombre;
    private int semanaActual;
    private int cantidadSemanas;
}
