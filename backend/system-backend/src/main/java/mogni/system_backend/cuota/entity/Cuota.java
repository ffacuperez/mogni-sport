package mogni.system_backend.cuota.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

/**
 * Un período de 30 días ya pagado (regla C1): se crea solo al pagar (alta, renovación o reactivación), siempre
 * junto con su único pago, así que no tiene estado. Lo que cambia con el tiempo es la situación de pago del alumno
 * ({@link SituacionPago}), que se calcula con el vencimiento de su última cuota.
 *
 * <p>Las columnas viejas {@code estado} y {@code periodo} siguen en la base (las usan otras ramas) pero ya no se
 * cargan: ver {@code MigracionCuotasPeriodoPagado}.
 */
@Entity
@Table(name = "cuotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    /** Membresía que cubrió este período (C4): el historial la muestra aunque después cambie. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    /** Sin NOT NULL en la base (Postgres no deja agregarlo con filas existentes); siempre lo carga el código. */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    /** Fin del período: inicio + 30 días. */
    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    /** Precio vigente de la membresía al pagar, sin recargo. La columna conserva su nombre viejo. */
    @Column(name = "monto_esperado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Cuota de 30 días desde {@code inicio}, al precio actual de la membresía. */
    public static Cuota nueva(Inscripcion inscripcion, Membresia membresia, LocalDate inicio) {
        Cuota cuota = new Cuota();
        cuota.setInscripcion(inscripcion);
        cuota.setMembresia(membresia);
        cuota.setFechaInicio(inicio);
        cuota.setFechaVencimiento(inicio.plusDays(ReglasCuota.DIAS_CUOTA));
        cuota.setPrecio(BigDecimal.valueOf(membresia.getPrecioActual()).setScale(2));
        return cuota;
    }

    /** Membresía de la cuota; las cuotas viejas sin migrar usan la de su inscripción. */
    public Membresia membresiaCubierta() {
        return membresia != null ? membresia : inscripcion.getMembresia();
    }
}
