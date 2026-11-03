package mogni.system_backend.entrenamiento.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import mogni.system_backend.plan.entity.EjercicioSesionPlan;

@Entity
@Table(name = "ejercicios_entrenamiento")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenamiento_id", nullable = false)
    private Entrenamiento entrenamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_plan_id", nullable = false)
    private EjercicioSesionPlan ejercicioPlan;

    @Enumerated(EnumType.STRING)
    private EstadoEjercicioEntrenamiento estado = EstadoEjercicioEntrenamiento.PENDIENTE;

    private String nota;

    @OneToMany(mappedBy = "ejercicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SerieEntrenamiento> series = new ArrayList<>();

    public void actualizarEstado() {
        this.estado = (series != null && !series.isEmpty() && series.stream().allMatch(SerieEntrenamiento::esValida))
                ? EstadoEjercicioEntrenamiento.COMPLETO
                : EstadoEjercicioEntrenamiento.PENDIENTE;
    }
}
