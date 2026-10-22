package mogni.system_backend.alumno.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.alumno.entity.Modalidad;
import mogni.system_backend.pago.entity.MedioDePago;

@Getter
@Setter
public class RegistrarAlumnoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(regexp = "^[^@ ]+@[^@ ]+[.][^@ ]+$", message = "El email no tiene un formato válido")
    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[-0-9+ ]{6,20}$", message = "El teléfono no tiene un formato válido")
    private String telefono;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotNull(message = "La modalidad es obligatoria")
    private Modalidad modalidad;

    @NotNull(message = "Debe asignarse una membresía al inscribir al alumno")
    private Long membresiaId;

    /** Medio con el que paga la primera cuota: el alta siempre se hace con la primera cuota pagada (MOGNI-73). */
    @NotNull(message = "Debe seleccionarse el medio de pago de la primera cuota")
    private MedioDePago medioDePago;

    private String objetivo;

    private String cuestionarioSalud;

}