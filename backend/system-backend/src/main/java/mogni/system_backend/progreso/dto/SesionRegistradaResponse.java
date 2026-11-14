package mogni.system_backend.progreso.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** HU-270: una sesión en el listado de sesiones registradas de un plan. */
@Getter
@AllArgsConstructor
public class SesionRegistradaResponse {
    private Long id;
    private int semana;
    private int numeroSesion;
    /** El nombre de la sesión en el plan (p. ej. "Tren inferior A"); null si no tiene. */
    private String nombreSesion;
    private LocalDate fecha;
    /** Los ejercicios hechos de la sesión. */
    private int ejerciciosRegistrados;
    /** Todos los ejercicios que tenía indicados la sesión, según la copia que se guardó al empezarla. */
    private int ejerciciosTotales;
}
