package mogni.system_backend.entrenamiento.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series_entrenamiento")
@Getter
@Setter
@NoArgsConstructor
public class SerieEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_entrenamiento_id", nullable = false)
    private EjercicioEntrenamiento ejercicio;

    @Column(nullable = false)
    private int numero;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    private Integer repeticiones;

    private Integer rirRealizado;

    @Column(precision = 6, scale = 2)
    private BigDecimal rmEstimado;

    public boolean esValida() {
        return peso != null && peso.compareTo(BigDecimal.ZERO) > 0
                && repeticiones != null && repeticiones > 0
                && rirRealizado != null && rirRealizado >= 0;
    }

    public void recalcularRmEstimado() {
        if (!esValida()) {
            rmEstimado = null;
            return;
        }
        BigDecimal divisor = BigDecimal.valueOf(30L);
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(repeticiones).divide(divisor, 10, RoundingMode.HALF_UP));
        rmEstimado = peso.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
