package mogni.system_backend.turno.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Crear o modificar una franja. El resto de las validaciones (fin posterior, horas completas, superposición) están en el servicio. */
@Getter
@Setter
public class FranjaHorariaRequest {

    @NotNull(message = "El día es obligatorio")
    private DayOfWeek diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}
