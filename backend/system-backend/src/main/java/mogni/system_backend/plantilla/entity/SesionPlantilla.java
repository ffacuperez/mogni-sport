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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesión numerada de una plantilla ("Sesión 1 · Full body A"), con sus ejercicios en orden. */
@Entity
@Table(name = "sesiones_plantilla")
@Getter
@Setter
@NoArgsConstructor
public class SesionPlantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1, 2, 3... según el orden en la plantilla. */
    @Column(nullable = false)
    private int numero;

    /** Nombre descriptivo opcional, por ejemplo "Full body A". */
    @Column(length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private PlantillaPlan plantilla;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EjercicioSesion> ejercicios = new ArrayList<>();

    public SesionPlantilla(String nombre) {
        this.nombre = nombre;
    }

    /** Agrega los ejercicios en el orden recibido. */
    public void agregarEjercicios(List<EjercicioSesion> nuevos) {
        for (EjercicioSesion ejercicio : nuevos) {
            ejercicio.setOrden(ejercicios.size() + 1);
            ejercicio.setSesion(this);
            ejercicios.add(ejercicio);
        }
    }

    SesionPlantilla copiar() {
        SesionPlantilla copia = new SesionPlantilla(nombre);
        copia.agregarEjercicios(ejercicios.stream().map(EjercicioSesion::copiar).toList());
        return copia;
    }
}
