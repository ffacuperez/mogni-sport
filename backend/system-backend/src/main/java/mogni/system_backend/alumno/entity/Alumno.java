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
import mogni.system_backend.cuota.entity.Cuota;
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

    /** La inscripción PROGRAMADA (como mucho hay una): la membresía ya pagada para el período siguiente. */
    public Optional<Inscripcion> buscarInscripcionProgramada() {
        return inscripciones.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.PROGRAMADA)
                .max(Comparator.comparing(Inscripcion::getFechaInicio));
    }

    /** Última cuota de la inscripción ACTIVA (sin la programada). */
    public Optional<Cuota> buscarUltimaCuotaActiva() {
        return buscarInscripcionActiva().flatMap(Inscripcion::buscarUltimaCuota);
    }

    /**
     * Hasta cuándo pagó: la última cuota entre la inscripción ACTIVA y la PROGRAMADA. Con ella se calcula la situación
     * de pago. La membresía que usa hoy, en cambio, es siempre la de la ACTIVA (ver 20.10, "dos preguntas").
     */
    public Optional<Cuota> buscarUltimaCuotaPagada() {
        return inscripciones.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA || i.getEstado() == EstadoInscripcion.PROGRAMADA)
                .flatMap(i -> i.getCuotas().stream())
                .max(Inscripcion.ORDEN_CUOTAS);
    }

    /** Última cuota de cualquier inscripción (por ejemplo, para saber si un alumno inactivo todavía tiene días pagos). */
    public Optional<Cuota> buscarUltimaCuota() {
        return inscripciones.stream()
                .flatMap(inscripcion -> inscripcion.getCuotas().stream())
                .max(Inscripcion.ORDEN_CUOTAS);
    }

    /** La inscripción ACTIVA si tiene; si no, la más reciente (CANCELADA). Vacío si nunca tuvo. */
    public Optional<Inscripcion> buscarInscripcionActual() {
        return buscarInscripcionActiva().or(() -> inscripciones.stream()
                .max(Comparator.comparing(Inscripcion::getFechaInicio)
                        .thenComparing(Inscripcion::getId, Comparator.nullsFirst(Comparator.naturalOrder()))));
    }
}