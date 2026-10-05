package mogni.system_backend.alumno.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

@Entity
@Table(name = "alumnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    private int edad;

    private String telefono;

    private String sexo;

    @Column(unique = true, nullable = false)
    private String dni;

    @Column(unique = true, nullable = false)
    private String email;

    /** Requerida por HU-73 (obligatoria y validación de 16 años); de acá se calcula la edad. */
    private LocalDate fechaNacimiento;

    @Column(columnDefinition = "TEXT")
    private String objetivo;

    @Column(columnDefinition = "TEXT")
    private String cuestionarioSalud;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cuenta_id", referencedColumnName = "id", unique = true)
    private Cuenta cuenta;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public void agregarInscripcion(Inscripcion inscripcion) {
        inscripcion.setAlumno(this);
        inscripciones.add(inscripcion);
    }

    /** La inscripción ACTIVA más reciente, si tiene. */
    public Optional<Inscripcion> buscarInscripcionActiva() {
        return inscripciones.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .max(Comparator.comparing(Inscripcion::getFechaInicio));
    }
}