package mogni.system_backend.progreso.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;

/**
 * HU-270: lo que el alumno hizo en un ejercicio de una sesión registrada. Cada ejercicio puede tener varias series
 * con peso, repeticiones reales y RIR. El 1RM estimado del ejercicio es el máximo entre sus series completadas.
 */
@Entity
@Table(name = "registros_ejercicio")
@Getter
@Setter
@NoArgsConstructor
public class RegistroEjercicio extends EjercicioPrescripto {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_sesion_id", nullable = false)
    private RegistroSesion registro;

    @OneToMany(mappedBy = "ejercicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<RegistroSerie> seriesRegistradas = new ArrayList<>();

    /** En kg; null si no usó peso. */
    @Column(precision = 6, scale = 2)
    private BigDecimal kgUsados;

    /** Repeticiones realizadas en esta ejecución del ejercicio. */
    private Integer repeticionesRealizadas;

    /** Null si no lo anotó. */
    private Integer rirReal;

    /** Null si no dejó ninguna nota. */
    @Column(length = 500)
    private String notas;

    /** El 1RM estimado máximo del ejercicio entre todas sus series completas. */
    @Column(precision = 10, scale = 2)
    private BigDecimal rmEstimado;

    @Column(nullable = false)
    private boolean completo = false;

    public RegistroEjercicio(EjercicioPrescripto indicado) {
        super(indicado);
        this.notas = null;
    }

    /** Compatibilidad con pruebas y código previo: sin repeticiones reales cargadas todavía. */
    public RegistroEjercicio(EjercicioPrescripto indicado, BigDecimal kgUsados, Integer rirReal, String notas) {
        this(indicado);
        this.kgUsados = kgUsados;
        this.rirReal = rirReal;
        this.notas = notas;
        this.repeticionesRealizadas = null;
        recalcularEstado();
    }

    public RegistroEjercicio(EjercicioPrescripto indicado, BigDecimal kgUsados, Integer repeticionesRealizadas,
            Integer rirReal, String notas) {
        this(indicado);
        this.kgUsados = kgUsados;
        this.repeticionesRealizadas = repeticionesRealizadas;
        this.rirReal = rirReal;
        this.notas = notas;
        recalcularEstado();
    }

    public void agregarSeries(List<RegistroSerie> nuevas) {
        for (RegistroSerie serie : nuevas) {
            serie.setOrden(seriesRegistradas.size() + 1);
            serie.setEjercicio(this);
            seriesRegistradas.add(serie);
        }
        recalcularEstado();
    }

    public void actualizarSeries(List<RegistroSerie> nuevas) {
        this.seriesRegistradas.clear();
        agregarSeries(nuevas);
    }

    public void recalcularEstado() {
        if (seriesRegistradas.isEmpty()) {
            return;
        }
        BigDecimal maxRm = null;
        boolean todasCompletas = seriesRegistradas.stream().allMatch(RegistroSerie::isCompleto);
        for (RegistroSerie serie : seriesRegistradas) {
            if (serie.getRmEstimado() != null && (maxRm == null || serie.getRmEstimado().compareTo(maxRm) > 0)) {
                maxRm = serie.getRmEstimado();
            }
        }
        this.rmEstimado = maxRm;
        this.completo = todasCompletas;
        if (todasCompletas) {
            RegistroSerie ultimaCompleta = seriesRegistradas.stream().filter(RegistroSerie::isCompleto).findFirst().orElse(null);
            if (ultimaCompleta != null) {
                this.kgUsados = ultimaCompleta.getPeso();
                this.repeticionesRealizadas = ultimaCompleta.getRepeticiones();
                this.rirReal = ultimaCompleta.getRir();
                this.notas = ultimaCompleta.getNota();
            }
        }
    }

    public static BigDecimal calcularRmEstimado(BigDecimal peso, Integer repeticionesRealizadas, Integer rirReal) {
        if (peso == null || repeticionesRealizadas == null || rirReal == null) {
            return null;
        }
        int repsTotales = repeticionesRealizadas + rirReal;
        if (repsTotales <= 0) {
            return null;
        }
        if (repsTotales == 1) {
            return peso;
        }
        BigDecimal factor = BigDecimal.valueOf(repsTotales).divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
        return peso.multiply(BigDecimal.ONE.add(factor)).setScale(2, RoundingMode.HALF_UP);
    }
}
