package mogni.system_backend.inscripcion.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /** Orden de las cuotas de un alumno: por vencimiento y, a igual vencimiento, por id. */
    public static final Comparator<Cuota> ORDEN_CUOTAS = Comparator.comparing(Cuota::getFechaVencimiento)
            .thenComparing(Cuota::getId, Comparator.nullsLast(Comparator.naturalOrder()));

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

    /** Inscripción nueva (todavía sin guardar) del alumno, con esa membresía, desde {@code inicio}. */
    public static Inscripcion nueva(Alumno alumno, Membresia membresia, LocalDate inicio, EstadoInscripcion estado) {
        Inscripcion inscripcion = new Inscripcion(null, inicio, null, estado, null, membresia);
        alumno.agregarInscripcion(inscripcion);
        return inscripcion;
    }

    /** Crea una cuota pagada de 30 días desde {@code inicio}, con la membresía de esta inscripción. */
    public Cuota agregarCuota(LocalDate inicio) {
        Cuota cuota = Cuota.nueva(this, inicio);
        cuotas.add(cuota);
        return cuota;
    }

    /** Cuota del cambio de membresía (regla M4): de hoy al vencimiento anterior, por la diferencia (puede ser 0). */
    public Cuota agregarCuotaPorCambio(LocalDate inicio, LocalDate vencimiento, BigDecimal diferencia) {
        Cuota cuota = Cuota.porCambioDeMembresia(this, inicio, vencimiento, diferencia);
        cuotas.add(cuota);
        return cuota;
    }

    /** La de vencimiento más reciente (a igual vencimiento, la última creada). Vacío si no tiene cuotas. */
    public Optional<Cuota> buscarUltimaCuota() {
        return cuotas.stream().max(ORDEN_CUOTAS);
    }

    public void cancelar(LocalDate fecha) {
        this.estado = EstadoInscripcion.CANCELADA;
        this.fechaFin = fecha;
    }

    /** Vuelve a ACTIVA (reactivación de un alumno que todavía tiene días pagos). */
    public void reactivar() {
        activar();
    }

    /** Pasa a ACTIVA: una programada cuyo período ya empezó, o una que se reactiva. */
    public void activar() {
        this.estado = EstadoInscripcion.ACTIVA;
        this.fechaFin = null;
    }

    /** Pasa a PROGRAMADA (o vuelve, al reactivar a un alumno antes de que empiece el período que ya pagó). */
    public void programar() {
        this.estado = EstadoInscripcion.PROGRAMADA;
        this.fechaFin = null;
    }
}
