package mogni.system_backend.notificacion.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificacionResponse {
    /** ID estable de la notificación; el frontend lo usa junto con el tipo para marcarla como leída. */
    private String id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    /** Desde cuándo está vigente la notificación. */
    private LocalDate fecha;
    private Long cuotaId;
    private LocalDate fechaVencimiento;
    private Long turnoId;
    private LocalDate fechaTurno;

    public NotificacionResponse(String id, TipoNotificacion tipo, String titulo, String mensaje,
                                LocalDate fecha, Long cuotaId, LocalDate fechaVencimiento) {
        this(id, tipo, titulo, mensaje, fecha, cuotaId, fechaVencimiento, null, null);
    }
}
