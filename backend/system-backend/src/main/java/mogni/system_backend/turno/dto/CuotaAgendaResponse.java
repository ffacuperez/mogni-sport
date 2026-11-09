package mogni.system_backend.turno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.cuota.entity.SituacionPago;

/**
 * MOGNI-257: situación de pago del alumno según su última cuota (la misma que el listado de alumnos, MOGNI-46). El
 * frontend la muestra con estadoVisible de cuotasService.
 */
@Getter
@AllArgsConstructor
public class CuotaAgendaResponse {
    private SituacionPago situacion;
    private LocalDate fechaVencimiento;
}
