package mogni.system_backend.ejercicio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EjercicioResponse {
    private Long id;
    private String nombre;
    private String grupoMuscular;
    private String descripcion;
    private boolean activo;
}