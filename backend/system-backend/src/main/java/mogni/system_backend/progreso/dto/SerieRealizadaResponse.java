package mogni.system_backend.progreso.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.progreso.entity.RegistroSerie;

/** HU-269: una serie realizada. El 1RM estimado es uno solo por ejercicio (MOGNI-269), no se informa por serie. */
@Getter
@AllArgsConstructor
public class SerieRealizadaResponse {
    private int orden;
    private BigDecimal peso;
    private int repeticiones;
    private int rir;

    public static SerieRealizadaResponse desde(RegistroSerie serie) {
        return new SerieRealizadaResponse(serie.getOrden(), serie.getPeso(), serie.getRepeticiones(), serie.getRir());
    }
}
