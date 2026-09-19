package mogni.system_backend.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.dto.ActualizarAlumnoRequest;
import mogni.system_backend.dto.AlumnoResponse;
import mogni.system_backend.dto.RegistrarAlumnoRequest;
import mogni.system_backend.entidades.Alumno;
import mogni.system_backend.entidades.Cuenta;
import mogni.system_backend.entidades.Rol;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.repository.AlumnoRepository;
import mogni.system_backend.repository.CuentaRepository;

@Service
public class AlumnoService {

    private static final int EDAD_MINIMA = 16; // ajustar al valor real que definan para Mogni

    private final AlumnoRepository alumnoRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    public AlumnoService(AlumnoRepository alumnoRepository,
                          CuentaRepository cuentaRepository,
                          PasswordEncoder passwordEncoder) {
        this.alumnoRepository = alumnoRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AlumnoResponse registrarAlumno(RegistrarAlumnoRequest request) {

        String dniNormalizado = request.getDni().replaceAll("[^0-9]", "");

        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new RegistroInvalidoException(
                "El alumno debe tener al menos " + EDAD_MINIMA + " años para registrarse");
        }

        if (alumnoRepository.existsByDni(dniNormalizado)) {
            throw new RegistroInvalidoException("Ya existe un alumno registrado con ese DNI");
        }

        if (cuentaRepository.existsByEmail(request.getEmail())) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(request.getEmail());
        cuenta.setPassword(passwordEncoder.encode(dniNormalizado));
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setDebeCambiarPassword(true);
        cuenta.setActiva(true);

        Alumno alumno = new Alumno();
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setDni(dniNormalizado);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.setCuenta(cuenta);

        Alumno alumnoGuardado = alumnoRepository.save(alumno);

        return mapearAResponse(alumnoGuardado);
    }

    public List<AlumnoResponse> listarAlumnos() {
        return alumnoRepository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public AlumnoResponse obtenerAlumno(Long id) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        return mapearAResponse(alumno);
    }

    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, ActualizarAlumnoRequest request) {
        Alumno alumno = buscarAlumnoOLanzarError(id);

        String dniNormalizado = request.getDni().replaceAll("[^0-9]", "");
        boolean dniCambio = !alumno.getDni().equals(dniNormalizado);

        if (dniCambio && alumnoRepository.existsByDniAndIdNot(dniNormalizado, id)) {
            throw new RegistroInvalidoException("Ya existe otro alumno registrado con ese DNI");
        }

        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new RegistroInvalidoException(
                "El alumno debe tener al menos " + EDAD_MINIMA + " años para registrarse");
        }

        String emailActual = alumno.getCuenta().getEmail();
        if (!emailActual.equalsIgnoreCase(request.getEmail())
                && cuentaRepository.existsByEmail(request.getEmail())) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setDni(dniNormalizado);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.getCuenta().setEmail(request.getEmail());

        if (dniCambio && alumno.getCuenta().isDebeCambiarPassword()) {
            alumno.getCuenta().setPassword(passwordEncoder.encode(dniNormalizado));
        }

        Alumno alumnoActualizado = alumnoRepository.save(alumno);
        return mapearAResponse(alumnoActualizado);
    }

    private Alumno buscarAlumnoOLanzarError(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + id));
    }

    private AlumnoResponse mapearAResponse(Alumno alumno) {
        return new AlumnoResponse(
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getDni(),
            alumno.getCuenta().getEmail(),
            alumno.getFechaNacimiento(),
            alumno.getObjetivo(),
            alumno.getCuestionarioSalud(),
            alumno.getCuenta().getRol()
        );
    }
}