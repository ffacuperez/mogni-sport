package mogni.system_backend.membresia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MembresiaResponse {
    private Long id;
    private String nombre;
    private double precioActual;
    private int frecuenciaSemanal;
    private String descripcion;
}
