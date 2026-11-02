package mogni.system_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recuperar contraseña, paso 1: { "email": "..." }. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitarRecuperacionRequest {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;
}
