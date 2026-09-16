package mogni.system_backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.entidades.Rol;

@Getter
@AllArgsConstructor
public class AlumnoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private LocalDate fechaNacimiento;
    private String objetivo;
    private String cuestionarioSalud;
    private Rol rol;
}