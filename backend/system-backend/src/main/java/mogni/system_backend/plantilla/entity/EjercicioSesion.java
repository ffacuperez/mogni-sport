package mogni.system_backend.plantilla.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.ejercicio.entity.Ejercicio;

/** Un ejercicio dentro de una sesión de plantilla. Los datos (series, repeticiones...) están en EjercicioPrescripto. */
@Entity
@Table(name = "ejercicios_sesion")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioSesion extends EjercicioPrescripto {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionPlantilla sesion;

    public EjercicioSesion(Ejercicio ejercicio, int series, int repeticiones, Integer repeticionesMax,
            BigDecimal descansoMinutos, BigDecimal pesoSugerido, Integer rirObjetivo) {
        super(ejercicio, series, repeticiones, repeticionesMax, descansoMinutos, pesoSugerido, rirObjetivo);
    }

    private EjercicioSesion(EjercicioSesion origen) {
        super(origen);
    }

    EjercicioSesion copiar() {
        return new EjercicioSesion(this);
    }
}
