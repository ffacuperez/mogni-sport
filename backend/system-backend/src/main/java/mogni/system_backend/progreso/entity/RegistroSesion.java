package mogni.system_backend.progreso.entity;

import java.time.LocalDate;
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
import mogni.system_backend.plan.entity.PlanAsignado;

/**
 * HU-270: una sesión que el alumno registró en una semana del plan. Apunta a la sesión por su número y no por FK,
 * porque modificar un plan recrea sus sesiones (PlanAsignado.reemplazarSesiones) y cambian los ids.
 * La restricción única garantiza un solo registro por sesión y semana, aunque dos pedidos la inicien a la vez.
 */
@Entity
@Table(name = "registros_sesion", uniqueConstraints = @UniqueConstraint(columnNames = { "plan_id", "semana", "numero_sesion" }))
@Getter
@Setter
@NoArgsConstructor
public class RegistroSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAsignado plan;

    /** 1, 2, 3... dentro del plan. */
    @Column(nullable = false)
    private int semana;

    @Column(name = "numero_sesion", nullable = false)
    private int numeroSesion;

    /** El día en que se hizo la sesión. */
    @Column(nullable = false)
    private LocalDate fecha;

    /**
     * HU-269: false mientras está en curso; pasa a true cuando el alumno la finaliza con todos los ejercicios hechos.
     * Los registros anteriores a HU-269 ya estaban hechos: por eso el default de la columna es true.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean completada;

    @OneToMany(mappedBy = "registro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<RegistroEjercicio> ejercicios = new ArrayList<>();

    public RegistroSesion(PlanAsignado plan, int semana, int numeroSesion, LocalDate fecha) {
        this.plan = plan;
        this.semana = semana;
        this.numeroSesion = numeroSesion;
        this.fecha = fecha;
    }

    /** Agrega los ejercicios en el orden recibido. */
    public void agregarEjercicios(List<RegistroEjercicio> nuevos) {
        for (RegistroEjercicio ejercicio : nuevos) {
            ejercicio.setOrden(ejercicios.size() + 1);
            ejercicio.setRegistro(this);
            ejercicios.add(ejercicio);
        }
    }

    public boolean todosLosEjerciciosCompletos() {
        return ejercicios.stream().allMatch(RegistroEjercicio::isCompleto);
    }
}
