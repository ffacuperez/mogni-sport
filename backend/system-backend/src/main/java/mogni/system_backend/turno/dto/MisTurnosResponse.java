package mogni.system_backend.turno.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** HU-256: los turnos reservados del alumno que todavía no terminaron. El backend decide, el frontend solo dibuja. */
@Getter
@AllArgsConstructor
public class MisTurnosResponse {
    /** Hoy en Argentina (para escribir "Hoy" o "Mañana" sin depender del reloj del navegador). */
    private LocalDate hoy;
    /** Del más cercano al más lejano. */
    private List<MiTurnoResponse> turnos;
}
