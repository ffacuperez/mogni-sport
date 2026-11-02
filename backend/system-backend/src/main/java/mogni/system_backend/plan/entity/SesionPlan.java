package mogni.system_backend.plan.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.SesionPlantilla;

/** Sesión numerada del plan de un alumno, con sus ejercicios en orden. */
@Entity
@Table(name = "sesiones_plan")
@Getter
@Setter
@NoArgsConstructor
public class SesionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1, 2, 3... según el orden en el plan. */
    @Column(nullable = false)
    private int numero;

    /** Nombre descriptivo opcional, por ejemplo "Full body A". */
    @Column(length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAsignado plan;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EjercicioSesionPlan> ejercicios = new ArrayList<>();

    public SesionPlan(String nombre) {
        this.nombre = nombre;
    }

    /** Agrega los ejercicios en el orden recibido. */
    public void agregarEjercicios(List<EjercicioSesionPlan> nuevos) {
        for (EjercicioSesionPlan ejercicio : nuevos) {
            ejercicio.setOrden(ejercicios.size() + 1);
            ejercicio.setSesion(this);
            ejercicios.add(ejercicio);
        }
    }

    /** Copia profunda de una sesión de plantilla: sesión y ejercicios nuevos, sin ninguna referencia a la original. */
    static SesionPlan copiaDe(SesionPlantilla origen) {
        SesionPlan copia = new SesionPlan(origen.getNombre());
        copia.agregarEjercicios(origen.getEjercicios().stream().map(EjercicioSesionPlan::new).toList());
        return copia;
    }
}
