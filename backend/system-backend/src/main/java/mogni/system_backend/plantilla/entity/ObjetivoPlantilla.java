package mogni.system_backend.plantilla.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ObjetivoPlantilla {
    HIPERTROFIA,
    FUERZA,
    RESISTENCIA,
    PERDIDA_DE_GRASA;

    /** Solo por nombre: por defecto Jackson también acepta la posición (1 sería FUERZA). */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ObjetivoPlantilla desdeTexto(String valor) {
        return valueOf(valor);
    }
}
