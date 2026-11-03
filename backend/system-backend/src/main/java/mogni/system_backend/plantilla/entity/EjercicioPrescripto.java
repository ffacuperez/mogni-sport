package mogni.system_backend.plantilla.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.ejercicio.entity.Ejercicio;

/**
 * Un ejercicio del catálogo dentro de una sesión, con lo que se pide hacer: series, repeticiones (fijas o rango),
 * descanso, peso y RIR. Lo comparten las sesiones de una plantilla (HU-265) y las del plan asignado a un alumno
 * (HU-266), cada una en su propia tabla.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class EjercicioPrescripto {

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

    /** Objetivo fijo, o el mínimo del rango. */
    @Column(nullable = false)
    private int repeticiones;

    /** Máximo del rango (8-10 → 10); null si las repeticiones son un objetivo fijo. */
    private Integer repeticionesMax;

    /** En minutos; null si no se sugiere un descanso. */
    @Column(precision = 4, scale = 2)
    private BigDecimal descansoMinutos;

    /** En kg; null si no se sugiere un peso. */
    @Column(precision = 6, scale = 2)
    private BigDecimal pesoSugerido;

    /** RIR objetivo (repeticiones en reserva); null si no se indica. */
    private Integer rirObjetivo;

    /** HU-270: aclaración del profe ("Tempo 2-1-3", "Superserie con Gemelos"); null si no hay. */
    @Column(length = 200)
    private String indicaciones;

    protected EjercicioPrescripto(Ejercicio ejercicio, int series, int repeticiones, Integer repeticionesMax,
            BigDecimal descansoMinutos, BigDecimal pesoSugerido, Integer rirObjetivo) {
        this.ejercicio = ejercicio;
        this.series = series;
        this.repeticiones = repeticiones;
        // "8-8" es lo mismo que 8: se guarda como objetivo fijo
        this.repeticionesMax = repeticionesMax != null && repeticionesMax > repeticiones ? repeticionesMax : null;
        this.descansoMinutos = descansoMinutos;
        this.pesoSugerido = pesoSugerido;
        this.rirObjetivo = rirObjetivo;
    }

    /** Los mismos datos que otro ejercicio prescripto (para copiar de una plantilla a un plan). */
    protected EjercicioPrescripto(EjercicioPrescripto origen) {
        this(origen.ejercicio, origen.series, origen.repeticiones, origen.repeticionesMax, origen.descansoMinutos,
                origen.pesoSugerido, origen.rirObjetivo);
        this.indicaciones = origen.indicaciones;
    }
}
