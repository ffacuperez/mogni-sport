package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;
import mogni.system_backend.progreso.entity.RegistroEjercicio;

/**
 * Un ejercicio de una sesión del plan: lo que le pidió el profe y, si la sesión ya se empezó (HU-269), lo que cargó
 * el alumno para poder corregirlo.
 */
@Getter
@AllArgsConstructor
public class EjercicioSesionResponse {
    /** Posición dentro de la sesión: 1, 2, 3... Es la que se usa para registrarlo. */
    private int orden;
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
    /** Si ya se cargaron todas sus series. */
    private boolean completo;
    /** Null mientras no esté completo. */
    private BigDecimal rmEstimado;
    /** Null si no dejó ninguna nota. */
    private String notas;
    /** Vacía mientras no esté completo. */
    private List<SerieRealizadaResponse> seriesRealizadas;

    /** Un ejercicio de una sesión que todavía no se empezó. */
    public static EjercicioSesionResponse indicado(EjercicioPrescripto prescripto) {
        return new EjercicioSesionResponse(
                prescripto.getOrden(),
                prescripto.getEjercicio().getNombre(),
                prescripto.getSeries(),
                prescripto.getRepeticiones(),
                prescripto.getRepeticionesMax(),
                prescripto.getRirObjetivo(),
                prescripto.getDescansoMinutos(),
                prescripto.getIndicaciones(),
                false,
                null,
                null,
                List.of());
    }

    /** Un ejercicio de una sesión empezada: lo indicado sale de la copia que se guardó al empezarla. */
    public static EjercicioSesionResponse desde(RegistroEjercicio registro) {
        return new EjercicioSesionResponse(
                registro.getOrden(),
                registro.getEjercicio().getNombre(),
                registro.getSeries(),
                registro.getRepeticiones(),
                registro.getRepeticionesMax(),
                registro.getRirObjetivo(),
                registro.getDescansoMinutos(),
                registro.getIndicaciones(),
                registro.isCompleto(),
                registro.getRmEstimado(),
                registro.getNotas(),
                registro.getSeriesRealizadas().stream().map(SerieRealizadaResponse::desde).toList());
    }
}
