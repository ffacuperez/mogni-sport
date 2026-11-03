package mogni.system_backend.entrenamiento.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntrenamientoRequest {
    private List<EjercicioEntrenamientoRequest> ejercicios;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EjercicioEntrenamientoRequest {
        private Long id;
        private String nota;
        private List<SerieEntrenamientoRequest> series;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieEntrenamientoRequest {
        private Integer numero;
        private java.math.BigDecimal peso;
        private Integer repeticiones;
        private Integer rirRealizado;
    }
}
