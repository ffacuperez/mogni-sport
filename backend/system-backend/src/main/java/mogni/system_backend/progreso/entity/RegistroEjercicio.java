package mogni.system_backend.progreso.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;

/**
 * HU-270: lo que el alumno hizo en un ejercicio de una sesión registrada. Lo indicado (en EjercicioPrescripto) es una
 * copia de cuando se registró, así el historial no cambia si después el profe modifica el plan.
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

    /** En kg; null si no usó peso. */
    @Column(precision = 6, scale = 2)
    private BigDecimal kgUsados;

    /** Null si no lo anotó. */
    private Integer rirReal;

    /** Null si no dejó ninguna nota. */
    @Column(length = 500)
    private String notas;

    public RegistroEjercicio(EjercicioPrescripto indicado, BigDecimal kgUsados, Integer rirReal, String notas) {
        super(indicado);
        this.kgUsados = kgUsados;
        this.rirReal = rirReal;
        this.notas = notas;
    }
}
