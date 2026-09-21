package mogni.system_backend.alumno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.auth.entity.Rol;

@Getter
@AllArgsConstructor
public class AlumnoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private LocalDate fechaNacimiento;
    private String celular;
    private LocalDate fechaIngreso;
    private String objetivo;
    private String cuestionarioSalud;
    private Rol rol;
    private boolean activo;
    private Long membresiaId;
    private String membresiaNombre;
}