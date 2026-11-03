package mogni.system_backend.progreso.entity;

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
@Table(name = "registros_serie")
@Getter
@Setter
@NoArgsConstructor
public class RegistroSerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_ejercicio_id", nullable = false)
    private RegistroEjercicio ejercicio;

    @Column(nullable = false)
    private int orden;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    private Integer repeticiones;

    private Integer rir;

    @Column(length = 500)
    private String nota;

    @Column(precision = 10, scale = 2)
    private BigDecimal rmEstimado;

    @Column(nullable = false)
    private boolean completo = false;

    public RegistroSerie(BigDecimal peso, Integer repeticiones, Integer rir, String nota) {
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.rir = rir;
        this.nota = nota;
        recalcular();
    }

    public void actualizar(BigDecimal peso, Integer repeticiones, Integer rir, String nota) {
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.rir = rir;
        this.nota = nota;
        recalcular();
    }

    public void recalcular() {
        this.completo = peso != null && repeticiones != null && rir != null;
        if (!completo) {
            this.rmEstimado = null;
            return;
        }
        int repsTotales = repeticiones + rir;
        if (repsTotales <= 0) {
            this.rmEstimado = null;
            return;
        }
        if (repsTotales == 1) {
            this.rmEstimado = peso;
            return;
        }
        BigDecimal factor = BigDecimal.valueOf(repsTotales).divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
        this.rmEstimado = peso.multiply(BigDecimal.ONE.add(factor)).setScale(2, RoundingMode.HALF_UP);
    }
}