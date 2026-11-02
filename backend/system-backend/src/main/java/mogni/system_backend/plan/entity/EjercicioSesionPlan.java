package mogni.system_backend.plan.entity;

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
import mogni.system_backend.plantilla.entity.EjercicioPrescripto;

/** Un ejercicio dentro de una sesión del plan de un alumno. Los datos (series, repeticiones...) están en EjercicioPrescripto. */
@Entity
@Table(name = "ejercicios_sesion_plan")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioSesionPlan extends EjercicioPrescripto {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_plan_id", nullable = false)
    private SesionPlan sesion;

    public EjercicioSesionPlan(Ejercicio ejercicio, int series, int repeticiones, Integer repeticionesMax,
            BigDecimal descansoMinutos, BigDecimal pesoSugerido, Integer rirObjetivo) {
        super(ejercicio, series, repeticiones, repeticionesMax, descansoMinutos, pesoSugerido, rirObjetivo);
    }

    /** Copia de un ejercicio de plantilla: objeto nuevo, con los mismos datos. */
    public EjercicioSesionPlan(EjercicioPrescripto origen) {
        super(origen);
    }
}
