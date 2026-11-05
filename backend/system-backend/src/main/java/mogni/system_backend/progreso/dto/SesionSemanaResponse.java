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
    /** El nombre de la sesión en el plan (p. ej. "Tren inferior A"); null si no tiene. */
    private String nombre;
    private EstadoSesion estado;
    /** El día en que se empezó; null si no está registrada ni en curso. */
    private LocalDate fechaRegistro;
    /** HU-269: el registro de la sesión; null si no está registrada ni en curso. */
    private Long registroId;
    /** HU-269: si el alumno puede cargar o corregir ejercicios (plan activo, semana actual y sin finalizar). */
    private boolean registrable;
    private List<EjercicioSesionResponse> ejercicios;
}
