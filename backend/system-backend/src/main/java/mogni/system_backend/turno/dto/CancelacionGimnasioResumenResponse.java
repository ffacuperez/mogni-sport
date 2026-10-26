package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelacionGimnasioResumenResponse {
    private int turnosAfectados;
    private int reservasAfectadas;
}
