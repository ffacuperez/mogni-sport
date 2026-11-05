package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.progreso.entity.RegistroEjercicio;

/** HU-270: un ejercicio de una sesión registrada, con lo indicado y lo realizado. */
@Getter
@AllArgsConstructor
public class EjercicioRealizadoResponse {
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
    /** Null si no usó peso. */
    private BigDecimal kgUsados;
    /** Null si no lo anotó. */
    private Integer rirReal;
    /** Null si no dejó ninguna nota. */
    private String notas;
    /** HU-269: el de la serie de más peso; null en los registros anteriores a HU-269. */
    private BigDecimal rmEstimado;
    /** HU-269: vacía en los registros anteriores a HU-269. */
    private List<SerieRealizadaResponse> seriesRealizadas;
    /** RIR real menos el objetivo; null si coinciden o si falta alguno de los dos. */
    private Integer diferenciaRir;
    /** Si el alumno lo cargó. Los registros anteriores a HU-269 vienen siempre completos. */
    private boolean completo;

    public static EjercicioRealizadoResponse desde(RegistroEjercicio registro) {
        return new EjercicioRealizadoResponse(
                registro.getEjercicio().getNombre(),
                registro.getSeries(),
                registro.getRepeticiones(),
                registro.getRepeticionesMax(),
                registro.getRirObjetivo(),
                registro.getDescansoMinutos(),
                registro.getIndicaciones(),
                registro.getKgUsados(),
                registro.getRirReal(),
                registro.getNotas(),
                registro.getRmEstimado(),
                registro.getSeriesRealizadas().stream().map(SerieRealizadaResponse::desde).toList(),
                diferenciaRir(registro.getRirObjetivo(), registro.getRirReal()),
                registro.isCompleto());
    }

    private static Integer diferenciaRir(Integer objetivo, Integer real) {
        if (objetivo == null || real == null || objetivo.equals(real)) {
            return null;
        }
        return real - objetivo;
    }
}
