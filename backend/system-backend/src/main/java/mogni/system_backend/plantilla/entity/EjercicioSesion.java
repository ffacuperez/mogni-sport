package mogni.system_backend.plantilla.entity;

import java.math.BigDecimal;

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
import mogni.system_backend.ejercicio.entity.Ejercicio;

/** Un ejercicio del catálogo dentro de una sesión, con lo que se pide hacer: series, repeticiones, peso y RIR. */
@Entity
@Table(name = "ejercicios_sesion")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Posición dentro de la sesión: 1, 2, 3... */
    @Column(nullable = false)
    private int orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;

    @Column(nullable = false)
    private int series;

    /** Un número entero, no un rango. */
    @Column(nullable = false)
    private int repeticiones;

    /** En kg; null si no se sugiere un peso. */
    @Column(precision = 6, scale = 2)
    private BigDecimal pesoSugerido;

    /** RIR objetivo (repeticiones en reserva); null si no se indica. */
    private Integer rirObjetivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionPlantilla sesion;

    public EjercicioSesion(Ejercicio ejercicio, int series, int repeticiones, BigDecimal pesoSugerido,
            Integer rirObjetivo) {
        this.ejercicio = ejercicio;
        this.series = series;
        this.repeticiones = repeticiones;
        this.pesoSugerido = pesoSugerido;
        this.rirObjetivo = rirObjetivo;
    }

    EjercicioSesion copiar() {
        return new EjercicioSesion(ejercicio, series, repeticiones, pesoSugerido, rirObjetivo);
    }
}
