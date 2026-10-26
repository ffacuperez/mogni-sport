package mogni.system_backend.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que devuelve el navegador al aceptar las notificaciones (PushSubscription), aplanado. */
@Getter
@Setter
@NoArgsConstructor
public class SuscripcionPushRequest {

    @NotBlank
    @Size(max = 1024)
    private String endpoint;

    @NotBlank
    @Size(max = 200)
    private String p256dh;

    @NotBlank
    @Size(max = 100)
    private String auth;
}
