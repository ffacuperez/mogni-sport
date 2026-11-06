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

/** HU-269: una serie realizada de un ejercicio. Solo se guarda completa: con peso, repeticiones y RIR. */
@Entity
@Table(name = "registros_serie")
@Getter
@NoArgsConstructor
public class RegistroSerie {

    private static final BigDecimal TREINTA = BigDecimal.valueOf(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_ejercicio_id", nullable = false)
    private RegistroEjercicio ejercicio;

    /** 1, 2, 3... dentro del ejercicio. */
    @Column(nullable = false)
    private int orden;

    /** En kg; 0 si es con el peso del cuerpo. */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal peso;

    @Column(nullable = false)
    private int repeticiones;

    @Column(nullable = false)
    private int rir;

    /**
     * 1RM de esta serie sola. Desde MOGNI-269 no se muestra (el 1RM es uno por ejercicio); la columna queda porque ya
     * existe y es NOT NULL en las bases creadas antes del cambio.
     */
    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal rmEstimado;

    public RegistroSerie(int orden, BigDecimal peso, int repeticiones, int rir) {
        this.orden = orden;
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.rir = rir;
        this.rmEstimado = calcularRm(peso, repeticiones, rir);
    }

    /**
     * Epley con RIR: las repeticiones que se podrían haber hecho son las realizadas más las que quedaron en reserva.
     * Con una sola posible, el 1RM es el peso levantado.
     */
    public static BigDecimal calcularRm(BigDecimal peso, int repeticiones, int rir) {
        int posibles = repeticiones + rir;
        if (posibles <= 1) {
            return peso.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(posibles).divide(TREINTA, 8, RoundingMode.HALF_UP));
        return peso.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
