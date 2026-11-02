package mogni.system_backend.alumno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Plan de entrenamiento ACTIVO del alumno, para el listado (solo id y nombre). */
@Getter
@AllArgsConstructor
public class PlanActivoResumen {
    private Long id;
    private String nombre;
}
