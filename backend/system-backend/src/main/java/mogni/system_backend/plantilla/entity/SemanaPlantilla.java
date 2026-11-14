package mogni.system_backend.plantilla.entity;

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

/**
 * Una semana de la plantilla, con sus propias sesiones: así el entrenador puede bajar la intensidad en una semana de
 * descarga o cambiar ejercicios de una semana a otra. La semana 1 es la primera semana del plan al asignarla.
 */
@Entity
@Table(name = "semanas_plantilla",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plantilla_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class SemanaPlantilla {

    /** Largo máximo de la nota de la semana ("Semana de descarga"). */
    public static final int NOTA_MAX = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1, 2, 3... según el orden en la plantilla. */
    @Column(nullable = false)
    private int numero;

    /** Opcional, por ejemplo "Semana de descarga". */
    @Column(length = NOTA_MAX)
    private String nota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private PlantillaPlan plantilla;

    @OneToMany(mappedBy = "semana", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SesionPlantilla> sesiones = new ArrayList<>();

    public SemanaPlantilla(String nota) {
        this.nota = nota;
    }

    /** Agrega las sesiones en el orden recibido y las numera 1, 2, 3... dentro de la semana. */
    public void agregarSesiones(List<SesionPlantilla> nuevas) {
        for (SesionPlantilla sesion : nuevas) {
            sesion.setNumero(sesiones.size() + 1);
            sesion.setSemana(this);
            sesiones.add(sesion);
        }
    }

    SemanaPlantilla copiar() {
        SemanaPlantilla copia = new SemanaPlantilla(nota);
        copia.agregarSesiones(sesiones.stream().map(SesionPlantilla::copiar).toList());
        return copia;
    }
}
