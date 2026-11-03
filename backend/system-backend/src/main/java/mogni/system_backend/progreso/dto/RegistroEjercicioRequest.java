package mogni.system_backend.progreso.dto;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroEjercicioRequest {
    @Valid
    private List<RegistroSerieRequest> series;
}
