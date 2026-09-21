package mogni.system_backend.membresia.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MembresiaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer duracionDias;
    private Integer diasPorSemana;
    private BigDecimal precio;
}
