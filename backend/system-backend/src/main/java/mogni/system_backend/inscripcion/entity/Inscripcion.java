package mogni.system_backend.inscripcion.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.membresia.entity.Membresia;

@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    /** Null mientras está ACTIVA; al cancelarse guarda el día de la cancelación (solo trazabilidad). */
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membresia_id", nullable = false)
    private Membresia membresia;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL)
    private List<Cuota> cuotas = new ArrayList<>();

    public Inscripcion(Long id, LocalDate fechaInicio, LocalDate fechaFin, EstadoInscripcion estado,
                       Alumno alumno, Membresia membresia) {
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.alumno = alumno;
        this.membresia = membresia;
    }

    public void generarPrimeraCuota() {
        cuotas.add(Cuota.primeraDe(this));
    }

    public void cancelar(LocalDate fecha) {
        this.estado = EstadoInscripcion.CANCELADA;
        this.fechaFin = fecha;
    }
}
