package mogni.system_backend.turno.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-259: turno concreto de una fecha (ej.: lunes 20/10 de 8 a 9). Copia horas y cupo de la configuración al
 * generarse y no tiene relación con la franja: cuando la franja cambia, ReconciliacionTurnosService lo
 * actualiza comparando por (fecha, hora_inicio). La restricción única (fecha, hora_inicio)
 * evita turnos duplicados aunque dos alumnos consulten a la vez.
 */
@Entity
@Table(name = "turnos", uniqueConstraints = @UniqueConstraint(
        name = "uk_turnos_fecha_hora_inicio", columnNames = {"fecha", "hora_inicio"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    /** Copia del cupo vigente al generarse; los lugares libres se calculan con este valor. */
    @Column(nullable = false)
    private int cupoMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTurno estado = EstadoTurno.DISPONIBLE;

    /** Solo tiene valor si el turno está CANCELADO; null en un CANCELADO se interpreta como MANUAL. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MotivoCancelacionTurno motivoCancelacion;

    /** Turno con todos sus datos menos el motivo de cancelación. */
    public Turno(Long id, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int cupoMaximo, EstadoTurno estado) {
        this(id, fecha, horaInicio, horaFin, cupoMaximo, estado, null);
    }

    /** Turno nuevo, DISPONIBLE. */
    public Turno(LocalDate fecha, HorarioTurno horario, int cupoMaximo) {
        this.fecha = fecha;
        this.horaInicio = horario.horaInicio();
        this.horaFin = horario.horaFin();
        this.cupoMaximo = cupoMaximo;
    }

    public void cancelar(MotivoCancelacionTurno motivo) {
        this.estado = EstadoTurno.CANCELADO;
        this.motivoCancelacion = motivo;
    }

    /** Vuelve a DISPONIBLE con el cupo indicado y sin motivo de cancelación. */
    public void reactivar(int cupoVigente) {
        this.estado = EstadoTurno.DISPONIBLE;
        this.motivoCancelacion = null;
        this.cupoMaximo = cupoVigente;
    }

    /** CANCELADO por orfandad de franja (los CANCELADO sin motivo cuentan como manuales). */
    public boolean fueCanceladoPorFranja() {
        return estado == EstadoTurno.CANCELADO && motivoCancelacion == MotivoCancelacionTurno.FRANJA_ELIMINADA;
    }
}
