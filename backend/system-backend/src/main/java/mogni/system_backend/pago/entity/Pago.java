package mogni.system_backend.pago.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.cuota.entity.Cuota;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuota_id", nullable = false)
    private Cuota cuota;

    /** Total cobrado: precio de la cuota + recargo. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Recargo por renovar en mora (HU-80); 0 si no tuvo. Sin NOT NULL en la base, siempre lo carga el código. */
    @Column(precision = 12, scale = 2)
    private BigDecimal recargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedioDePago medioDePago;

    @Column(nullable = false)
    private LocalDate fechaPago;

    /**
     * Pago de una cuota recién creada: el total es su precio más el recargo. Cada cuota tiene un solo pago (C3); la
     * base no lo garantiza (pagos.cuota_id no es UNIQUE, ver el rediseño de cuotas), lo garantiza el código.
     */
    public static Pago de(Cuota cuota, BigDecimal recargo, MedioDePago medioDePago, LocalDate fechaPago) {
        Pago pago = new Pago();
        pago.setCuota(cuota);
        pago.setRecargo(recargo);
        pago.setMonto(cuota.getPrecio().add(recargo));
        pago.setMedioDePago(medioDePago);
        pago.setFechaPago(fechaPago);
        return pago;
    }

    /** Recargo, 0 en los pagos viejos que no lo tienen cargado. */
    public BigDecimal recargoOCero() {
        return recargo != null ? recargo : BigDecimal.ZERO;
    }
}
