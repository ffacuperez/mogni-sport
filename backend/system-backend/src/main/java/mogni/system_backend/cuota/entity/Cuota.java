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

    /** Inicio del período pagado. */
    @Column(nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fin del período: el mismo día del mes siguiente, o el 1 del posterior si ese día no existe (ver
     * {@link ReglasCuota#vencimientoDesde}). La cuota de un cambio de membresía termina con la anterior.
     */
    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    /**
     * Importe cobrado por el período, sin recargo: el precio vigente de la membresía al pagar o, en un cambio de
     * membresía, la diferencia (0 si la nueva no es más cara).
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Cuota de un mes desde {@code inicio} (ver {@link ReglasCuota#vencimientoDesde}), al precio actual de la membresía. */
    public static Cuota nueva(Inscripcion inscripcion, LocalDate inicio) {
        return conPeriodo(inscripcion, inicio, ReglasCuota.vencimientoDesde(inicio),
                BigDecimal.valueOf(inscripcion.getMembresia().getPrecioActual()).setScale(2));
    }

    /**
     * Cuota de un cambio de membresía (regla M4): desde {@code inicio} (hoy) hasta el mismo vencimiento de la cuota
     * anterior, por la diferencia a cobrar (puede ser 0).
     */
    public static Cuota porCambioDeMembresia(Inscripcion inscripcion, LocalDate inicio, LocalDate vencimiento,
                                             BigDecimal diferencia) {
        return conPeriodo(inscripcion, inicio, vencimiento, diferencia.setScale(2));
    }

    private static Cuota conPeriodo(Inscripcion inscripcion, LocalDate inicio, LocalDate vencimiento, BigDecimal precio) {
        Cuota cuota = new Cuota();
        cuota.setInscripcion(inscripcion);
        cuota.setFechaInicio(inicio);
        cuota.setFechaVencimiento(vencimiento);
        cuota.setPrecio(precio);
        return cuota;
    }

    /** Membresía que cubrió este período: la de su inscripción (cada cuota queda en la inscripción con la que se pagó). */
    public Membresia membresiaCubierta() {
        return inscripcion.getMembresia();
    }
}
