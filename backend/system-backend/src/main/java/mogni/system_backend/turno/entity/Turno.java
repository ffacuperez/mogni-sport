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
 * HU-259: turno concreto de una fecha (ej.: lunes 20/10 de 8 a 9). Es una "foto" de la configuración al
 * generarse: copia horas y cupo, y no tiene relación con la franja. La restricción única (fecha, hora_inicio)
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

    /** Turno nuevo, DISPONIBLE. */
    public Turno(LocalDate fecha, HorarioTurno horario, int cupoMaximo) {
        this.fecha = fecha;
        this.horaInicio = horario.horaInicio();
        this.horaFin = horario.horaFin();
        this.cupoMaximo = cupoMaximo;
    }
}
