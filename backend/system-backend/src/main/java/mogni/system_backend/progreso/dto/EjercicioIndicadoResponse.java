package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;

/** Lo que el profe le pidió al alumno en un ejercicio. */
@Getter
@AllArgsConstructor
public class EjercicioIndicadoResponse {
    private String nombre;
    private int series;
    private int repeticiones;
    /** Máximo del rango; null si las repeticiones son un objetivo fijo. */
    private Integer repeticionesMax;
    /** Null si no se indica. */
    private Integer rirObjetivo;
    /** En minutos; null si no se sugiere un descanso. */
    private BigDecimal descansoMinutos;
    /** Null si no hay. */
    private String indicaciones;

    public static EjercicioIndicadoResponse desde(EjercicioPrescripto prescripto) {
        return new EjercicioIndicadoResponse(
                prescripto.getEjercicio().getNombre(),
                prescripto.getSeries(),
                prescripto.getRepeticiones(),
                prescripto.getRepeticionesMax(),
                prescripto.getRirObjetivo(),
                prescripto.getDescansoMinutos(),
                prescripto.getIndicaciones());
    }
}
