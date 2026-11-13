package mogni.system_backend.plantilla.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;

@Getter
@AllArgsConstructor
public class EjercicioSesionResponse {
    private Long id;
    private Long ejercicioId;
    private String nombre;
    private String grupoMuscular;
    /** false si el ejercicio se dio de baja del catálogo después de agregarlo a la plantilla. */
    private boolean ejercicioActivo;
    private int series;
    private int repeticiones;
    /** Máximo del rango; null si las repeticiones son un objetivo fijo. */
    private Integer repeticionesMax;
    /** En minutos; null si no se sugiere un descanso. */
    private BigDecimal descansoMinutos;
    /** En kg; null si no se sugiere un peso. */
    private BigDecimal pesoSugerido;
    /** Repeticiones en reserva; null si no se indica. */
    private Integer rirObjetivo;
    /** Aclaración del profe; null si no hay. */
    private String indicaciones;

    /** Sirve para los ejercicios de una plantilla y para los de un plan asignado. */
    public static EjercicioSesionResponse desde(EjercicioPrescripto prescripto) {
        Ejercicio ejercicio = prescripto.getEjercicio();
        return new EjercicioSesionResponse(
                prescripto.getId(),
                ejercicio.getId(),
                ejercicio.getNombre(),
                ejercicio.getGrupoMuscular(),
                ejercicio.isActivo(),
                prescripto.getSeries(),
                prescripto.getRepeticiones(),
                prescripto.getRepeticionesMax(),
                prescripto.getDescansoMinutos(),
                prescripto.getPesoSugerido(),
                prescripto.getRirObjetivo(),
                prescripto.getIndicaciones());
    }
}
