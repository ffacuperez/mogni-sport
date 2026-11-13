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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.SesionPlantilla;

/** Sesión numerada de una semana del plan de un alumno, con sus ejercicios en orden. */
@Entity
@Table(name = "sesiones_plan",
        uniqueConstraints = @UniqueConstraint(columnNames = {"semana_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class SesionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1, 2, 3... según el orden dentro de la semana. */
    @Column(nullable = false)
    private int numero;

    /** Nombre descriptivo opcional, por ejemplo "Full body A". */
    @Column(length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semana_id", nullable = false)
    private SemanaPlan semana;

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

    /** Copia de otra sesión del plan, con los mismos datos (para conservar una sesión ya registrada al editar). */
    public SesionPlan copiar() {
        SesionPlan copia = new SesionPlan(nombre);
        copia.agregarEjercicios(ejercicios.stream().map(EjercicioSesionPlan::new).toList());
        return copia;
    }
}
