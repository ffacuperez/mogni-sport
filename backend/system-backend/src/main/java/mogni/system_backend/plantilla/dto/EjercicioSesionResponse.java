package mogni.system_backend.plantilla.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    /** En kg; null si no se sugiere un peso. */
    private BigDecimal pesoSugerido;
    /** Repeticiones en reserva; null si no se indica. */
    private Integer rirObjetivo;
}
