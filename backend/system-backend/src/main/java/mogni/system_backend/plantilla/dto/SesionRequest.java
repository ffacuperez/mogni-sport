package mogni.system_backend.plantilla.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sesión; su número sale del orden en que llega en la lista. */
@Getter
@Setter
@NoArgsConstructor
public class SesionRequest {

    /** Opcional, por ejemplo "Full body A". */
    @Size(max = 100, message = "El nombre de la sesión no puede superar los 100 caracteres")
    private String nombre;

    @NotEmpty(message = "Cada sesión tiene que tener al menos un ejercicio")
    private List<@Valid EjercicioSesionRequest> ejercicios;
}
