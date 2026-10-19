package mogni.system_backend.notificacion.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.alumno.entity.Alumno;

/**
 * Aviso al alumno de que un turno que el gimnasio le había cancelado volvió a estar disponible. Es aparte de
 * AvisoTurno (que tiene un único aviso por reserva): un mismo turno se puede cancelar y reactivar más de una vez.
 */
@Entity
@Table(name = "avisos_turno_reactivado", indexes = @Index(name = "ix_aviso_reactivado_alumno", columnList = "alumno_id"))
@Getter
@Setter
@NoArgsConstructor
public class AvisoTurnoReactivado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_turno", nullable = false)
    private LocalDate fechaTurno;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    public AvisoTurnoReactivado(Alumno alumno, Long reservaId, Long turnoId, LocalDate fecha, LocalDate fechaTurno,
                                String titulo, String mensaje) {
        this.alumno = alumno;
        this.reservaId = reservaId;
        this.turnoId = turnoId;
        this.fecha = fecha;
        this.fechaTurno = fechaTurno;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }
}
