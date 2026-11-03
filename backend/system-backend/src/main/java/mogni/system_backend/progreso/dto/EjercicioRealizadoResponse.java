package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.progreso.entity.RegistroEjercicio;
import mogni.system_backend.progreso.entity.RegistroSerie;

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
    /** Repeticiones reales realizadas. */
    private Integer repeticionesRealizadas;
    /** Null si no lo anotó. */
    private Integer rirReal;
    /** Null si no dejó ninguna nota. */
    private String notas;
    /** 1RM estimado para este ejercicio. */
    private BigDecimal rmEstimado;
    /** Si se cargaron peso, repeticiones y RIR. */
    private boolean completo;
    private List<RegistroSerieResponse> seriesRealizadas;
    /** RIR real menos el objetivo; null si coinciden o si falta alguno de los dos. */
    private Integer diferenciaRir;

    public static EjercicioRealizadoResponse desde(RegistroEjercicio registro) {
        List<RegistroSerieResponse> seriesRealizadas = registro.getSeriesRegistradas().stream()
                .map(serie -> new RegistroSerieResponse(
                        serie.getOrden(),
                        serie.getPeso(),
                        serie.getRepeticiones(),
                        serie.getRir(),
                        serie.getNota(),
                        serie.getRmEstimado(),
                        serie.isCompleto()))
                .toList();
        Integer ultimoRir = registro.getSeriesRegistradas().stream().filter(RegistroSerie::isCompleto)
                .map(RegistroSerie::getRir)
                .findFirst().orElse(null);
        return new EjercicioRealizadoResponse(
                registro.getEjercicio().getNombre(),
                registro.getSeries(),
                registro.getRepeticiones(),
                registro.getRepeticionesMax(),
                registro.getRirObjetivo(),
                registro.getDescansoMinutos(),
                registro.getIndicaciones(),
                registro.getKgUsados(),
                registro.getRepeticionesRealizadas(),
                ultimoRir,
                registro.getNotas(),
                registro.getRmEstimado(),
                registro.isCompleto(),
                seriesRealizadas,
                diferenciaRir(registro.getRirObjetivo(), ultimoRir));
    }

    private static Integer diferenciaRir(Integer objetivo, Integer real) {
        if (objetivo == null || real == null || objetivo.equals(real)) {
            return null;
        }
        return real - objetivo;
    }
}
