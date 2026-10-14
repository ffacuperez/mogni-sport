package mogni.system_backend.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DesuscripcionPushRequest {

    @NotBlank
    private String endpoint;
}
