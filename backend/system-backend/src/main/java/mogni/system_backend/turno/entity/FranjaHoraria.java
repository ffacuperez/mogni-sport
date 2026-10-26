package mogni.system_backend.turno.entity;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-252: bloque semanal en el que se atiende (ej.: lunes de 8 a 12). Es la "plantilla" de los turnos:
 * se repite todas las semanas. Los turnos generados copian sus datos y no dependen de ella.
 */
@Entity
@Table(name = "franjas_horarias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FranjaHoraria {

    /** Todos los turnos duran 1 hora. */
    public static final int MINUTOS_POR_TURNO = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    public FranjaHoraria(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    /** Cantidad de turnos de 1 hora que entran en la franja (su duración en horas). */
    public int cantidadTurnos() {
        return (int) (Duration.between(horaInicio, horaFin).toMinutes() / MINUTOS_POR_TURNO);
    }

    /**
     * Turnos consecutivos de 1 hora desde la hora de inicio. Solo los calcula, no crea nada.
     * Se itera por cantidad (y no con un while sobre LocalTime, que daría la vuelta a la medianoche).
     */
    public List<HorarioTurno> turnosCalculados() {
        List<HorarioTurno> turnos = new ArrayList<>();
        for (int i = 0; i < cantidadTurnos(); i++) {
            LocalTime inicio = horaInicio.plusMinutes((long) i * MINUTOS_POR_TURNO);
            turnos.add(new HorarioTurno(inicio, inicio.plusMinutes(MINUTOS_POR_TURNO)));
        }
        return turnos;
    }

    /** Dos franjas se superponen si una empieza antes de que termine la otra. Las contiguas no se superponen. */
    public boolean seSuperponeCon(LocalTime inicio, LocalTime fin) {
        return inicio.isBefore(horaFin) && horaInicio.isBefore(fin);
    }
}
