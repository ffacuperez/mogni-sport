package mogni.system_backend.turno.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.alumno.entity.Alumno;

@Entity
@Table(name = "clases_extra", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clase_extra_reserva_origen", columnNames = "reserva_origen_id"),
        @UniqueConstraint(name = "uk_clase_extra_reserva_uso", columnNames = "reserva_uso_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ClaseExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "reserva_origen_id", nullable = false)
    private Long reservaOrigenId;

    @Column(name = "reserva_uso_id")
    private Long reservaUsoId;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    public ClaseExtra(Alumno alumno, Long reservaOrigenId, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this.alumno = alumno;
        this.reservaOrigenId = reservaOrigenId;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }
}
