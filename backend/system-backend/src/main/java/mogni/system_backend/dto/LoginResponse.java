package mogni.system_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.entidades.Rol;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Rol rol;
    private boolean debeCambiarPassword;
}