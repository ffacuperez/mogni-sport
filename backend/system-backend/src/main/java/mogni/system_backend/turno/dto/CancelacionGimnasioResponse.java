package mogni.system_backend.turno.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelacionGimnasioResponse {
    private int turnosCancelados;
    private int reservasCanceladas;
    private int clasesExtraOtorgadas;
}
