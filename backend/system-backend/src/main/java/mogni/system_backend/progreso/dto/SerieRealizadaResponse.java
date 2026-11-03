package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.progreso.entity.RegistroSerie;

/** HU-269: una serie realizada, con su 1RM estimado. */
@Getter
@AllArgsConstructor
public class SerieRealizadaResponse {
    private int orden;
    private BigDecimal peso;
    private int repeticiones;
    private int rir;
    private BigDecimal rmEstimado;

    public static SerieRealizadaResponse desde(RegistroSerie serie) {
        return new SerieRealizadaResponse(serie.getOrden(), serie.getPeso(), serie.getRepeticiones(), serie.getRir(),
                serie.getRmEstimado());
    }
}
