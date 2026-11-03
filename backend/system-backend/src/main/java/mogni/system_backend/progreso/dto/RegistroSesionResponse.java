package mogni.system_backend.progreso.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistroSesionResponse {
    private Long id;
    private int semana;
    private int numeroSesion;
    private LocalDate fecha;
    private boolean completada;
}
