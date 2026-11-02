package mogni.system_backend.progreso.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Una sesión del plan en una semana determinada. */
@Getter
@AllArgsConstructor
public class SesionSemanaResponse {
    private int numero;
    private EstadoSesion estado;
    /** El día en que se hizo; null si no está registrada. */
    private LocalDate fechaRegistro;
    private List<EjercicioIndicadoResponse> ejercicios;
}
