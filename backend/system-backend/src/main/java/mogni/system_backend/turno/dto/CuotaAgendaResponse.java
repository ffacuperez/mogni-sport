package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.EstadoCuota;

/**
 * MOGNI-257: cuota más reciente del alumno, con los datos tal cual están guardados. "Al día", "Por vencer" y
 * "Vencida" los calcula el frontend con la misma lógica que la pantalla de Cuotas (HU-46).
 */
@Getter
@AllArgsConstructor
public class CuotaAgendaResponse {
    private EstadoCuota estado;
    private LocalDate fechaVencimiento;
}
