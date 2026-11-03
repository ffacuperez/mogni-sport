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
    private LocalDate fecha;
    private int ejerciciosRegistrados;
}
