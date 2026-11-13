package mogni.system_backend.plan.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import mogni.system_backend.plantilla.entity.SemanaPlantilla;

/**
 * Una semana del plan de un alumno, con sus propias sesiones. La semana 1 empieza el día de la asignación; no se
 * alinea con las semanas del calendario.
 */
@Entity
@Table(name = "semanas_plan",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class SemanaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1, 2, 3... contando desde la semana de la asignación. */
    @Column(nullable = false)
    private int numero;

    /** Opcional, por ejemplo "Semana de descarga". */
    @Column(length = SemanaPlantilla.NOTA_MAX)
    private String nota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAsignado plan;

    @OneToMany(mappedBy = "semana", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SesionPlan> sesiones = new ArrayList<>();

    public SemanaPlan(String nota) {
        this.nota = nota;
    }

    /** Agrega las sesiones en el orden recibido y las numera 1, 2, 3... dentro de la semana. */
    public void agregarSesiones(List<SesionPlan> nuevas) {
        for (SesionPlan sesion : nuevas) {
            sesion.setNumero(sesiones.size() + 1);
            sesion.setSemana(this);
            sesiones.add(sesion);
        }
    }

    public Optional<SesionPlan> buscarSesion(int numero) {
        return sesiones.stream().filter(sesion -> sesion.getNumero() == numero).findFirst();
    }

    /** Copia profunda de una semana de plantilla: semana, sesiones y ejercicios nuevos. */
    static SemanaPlan copiaDe(SemanaPlantilla origen) {
        SemanaPlan copia = new SemanaPlan(origen.getNota());
        copia.agregarSesiones(origen.getSesiones().stream().map(SesionPlan::copiaDe).toList());
        return copia;
    }
}
