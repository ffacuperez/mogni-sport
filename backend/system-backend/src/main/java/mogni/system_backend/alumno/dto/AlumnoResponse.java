package mogni.system_backend.alumno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.inscripcion.dto.InscripcionResponse;

@Getter
@AllArgsConstructor
public class AlumnoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private int edad;
    private String telefono;
    private String sexo;
    private String dni;
    private String email;
    private LocalDate fechaNacimiento;
    private String objetivo;
    private String cuestionarioSalud;
    private Rol rol;
    private boolean activo;
    private LocalDate fechaBaja;
    private Long membresiaId;
    private String membresiaNombre;
    /** Inicio de la inscripción activa (null si no tiene). */
    private LocalDate fechaInicioInscripcion;
    /** Inscripción activa o, si no tiene, la última (con su estado). Null si nunca tuvo. */
    private InscripcionResponse inscripcionActual;
}