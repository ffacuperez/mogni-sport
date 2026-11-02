package mogni.system_backend.turno.entity;

import java.time.LocalDateTime;

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
import mogni.system_backend.alumno.entity.Alumno;

/**
 * Lugar de un alumno en un turno (HU-254). Por ahora es solo lo mínimo para que la HU-259 calcule los lugares
 * libres y el contador semanal: crear y cancelar reservas lo hacen las HU-254 y HU-255. La asistencia la
 * registra el entrenador desde la agenda (HU-262).
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Column(nullable = false)
    private LocalDateTime fechaHoraReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoReserva estado = EstadoReserva.RESERVADA;

    /**
     * HU-262: null mientras el entrenador no la marcó. No cambia el estado: una reserva AUSENTE sigue RESERVADA,
     * así cuenta para el límite semanal y no genera clase extra sin tocar esa lógica.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Asistencia asistencia;

    /** Reserva sin asistencia registrada (todas las reservas nacen así). */
    public Reserva(Long id, Alumno alumno, Turno turno, LocalDateTime fechaHoraReserva, EstadoReserva estado) {
        this(id, alumno, turno, fechaHoraReserva, estado, null);
    }
}
