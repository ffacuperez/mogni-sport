package mogni.system_backend.progreso.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
 * HU-270: lo que el alumno hizo en un ejercicio de una sesión registrada. Lo indicado (en EjercicioPrescripto) es una
 * copia de cuando se registró, así el historial no cambia si después el profe modifica el plan.
 * HU-269: se completa cargando todas sus series; kg, RIR real y 1RM estimado salen de la serie de más peso.
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
    private List<RegistroSerie> seriesRealizadas = new ArrayList<>();

    /** En kg; null si no usó peso. */
    @Column(precision = 6, scale = 2)
    private BigDecimal kgUsados;

    /** Null si no lo anotó. */
    private Integer rirReal;

    /** Null si no dejó ninguna nota. */
    @Column(length = 500)
    private String notas;

    /** El 1RM estimado de la serie de más peso (ver serieDeReferencia); null mientras no esté completo. */
    @Column(precision = 7, scale = 2)
    private BigDecimal rmEstimado;

    /** Los registros anteriores a HU-269 ya estaban hechos: por eso el default de la columna es true. */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean completo;

    /** Recién agregado a una sesión en curso: todavía sin series. */
    public RegistroEjercicio(EjercicioPrescripto indicado) {
        super(indicado);
    }

    public RegistroEjercicio(EjercicioPrescripto indicado, BigDecimal kgUsados, Integer rirReal, String notas) {
        super(indicado);
        this.kgUsados = kgUsados;
        this.rirReal = rirReal;
        this.notas = notas;
        this.completo = true;
    }

    /** Carga o corrige el ejercicio: reemplaza todas sus series y su nota. */
    public void registrar(List<RegistroSerie> series, String notas) {
        seriesRealizadas.clear();
        series.forEach(serie -> {
            serie.setEjercicio(this);
            seriesRealizadas.add(serie);
        });
        RegistroSerie referencia = serieDeReferencia(series);
        this.rmEstimado = RegistroSerie.calcularRm(referencia.getPeso(), referencia.getRepeticiones(), referencia.getRir());
        this.kgUsados = referencia.getPeso();
        this.rirReal = referencia.getRir();
        this.notas = notas;
        this.completo = true;
    }

    /**
     * MOGNI-269: el 1RM del ejercicio sale de una sola serie, la de más peso. Si empatan en peso, la de más repeticiones;
     * si también empatan, la primera.
     */
    public static RegistroSerie serieDeReferencia(List<RegistroSerie> series) {
        return series.stream()
                .max(Comparator.comparing(RegistroSerie::getPeso)
                        .thenComparingInt(RegistroSerie::getRepeticiones)
                        .thenComparing(Comparator.comparingInt(RegistroSerie::getOrden).reversed()))
                .orElseThrow();
    }
}
