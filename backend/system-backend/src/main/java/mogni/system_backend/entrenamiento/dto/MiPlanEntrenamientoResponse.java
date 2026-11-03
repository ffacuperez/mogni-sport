package mogni.system_backend.entrenamiento.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MiPlanEntrenamientoResponse {
    private Long id;
    private String nombre;
    private boolean activo;
    private List<SesionEntrenamientoResponse> sesiones;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SesionEntrenamientoResponse {
        private Long id;
        private Long entrenamientoId;
        private int numero;
        private String nombre;
        private String estado;
        private List<EjercicioEntrenamientoResponse> ejercicios;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EjercicioEntrenamientoResponse {
        private Long id;
        private Long ejercicioPlanId;
        private String nombre;
        private int cantidadSeries;
        private String repeticionesObjetivo;
        private Integer rirObjetivo;
        private String descanso;
        private String nota;
        private String estado;
        private List<SerieEntrenamientoResponse> series;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieEntrenamientoResponse {
        private int numero;
        private BigDecimal peso;
        private Integer repeticiones;
        private Integer rirRealizado;
        private BigDecimal rmEstimado;
    }
}
