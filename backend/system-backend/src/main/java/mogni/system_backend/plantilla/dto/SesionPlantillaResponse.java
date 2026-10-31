package mogni.system_backend.plantilla.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SesionPlantillaResponse {
    private Long id;
    private int numero;
    private String nombre;
    private List<EjercicioSesionResponse> ejercicios;
}
