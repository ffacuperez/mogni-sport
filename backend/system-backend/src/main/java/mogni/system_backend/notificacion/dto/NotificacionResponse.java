package mogni.system_backend.notificacion.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificacionResponse {
    /** Una sola notificación por cuota: "cuota-{id}". El frontend lo usa (junto con el tipo) para marcarla como leída. */
    private String id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    /** Desde cuándo está vigente la notificación. */
    private LocalDate fecha;
    private Long cuotaId;
    private LocalDate fechaVencimiento;
}
