package mogni.system_backend.cuota.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

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
import mogni.system_backend.inscripcion.entity.Inscripcion;

@Entity
@Table(name = "cuotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cuota {

    public static final int DIAS_VENCIMIENTO = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    /** Mes que cubre la cuota, formato "2026-10". */
    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEsperado;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuota estado = EstadoCuota.PENDIENTE;

    /** Primera cuota de una inscripción: PENDIENTE, al precio actual de la membresía, vence a 30 días del inicio. */
    public static Cuota primeraDe(Inscripcion inscripcion) {
        Cuota cuota = new Cuota();
        cuota.setInscripcion(inscripcion);
        cuota.setPeriodo(YearMonth.from(inscripcion.getFechaInicio()).toString());
        cuota.setMontoEsperado(BigDecimal.valueOf(inscripcion.getMembresia().getPrecioActual()));
        cuota.setFechaVencimiento(inscripcion.getFechaInicio().plusDays(DIAS_VENCIMIENTO));
        cuota.setEstado(EstadoCuota.PENDIENTE);
        return cuota;
    }
}
