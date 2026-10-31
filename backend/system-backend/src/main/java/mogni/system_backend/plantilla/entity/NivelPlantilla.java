package mogni.system_backend.plantilla.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NivelPlantilla {
    PRINCIPIANTE,
    INTERMEDIO,
    AVANZADO;

    /** Solo por nombre: por defecto Jackson también acepta la posición (1 sería INTERMEDIO). */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NivelPlantilla desdeTexto(String valor) {
        return valueOf(valor);
    }
}
