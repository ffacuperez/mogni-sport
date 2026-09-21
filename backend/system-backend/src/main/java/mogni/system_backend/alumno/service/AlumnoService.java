package mogni.system_backend.alumno.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;
import mogni.system_backend.alumno.dto.AlumnoResponse;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.repository.CuentaRepository;

@Service
public class AlumnoService {

    private static final int EDAD_MINIMA = 16; // ajustar al valor real que definan para Mogni

    private final AlumnoRepository alumnoRepository;
    private final CuentaRepository cuentaRepository;
    private final MembresiaRepository membresiaRepository;
    private final PasswordEncoder passwordEncoder;

    public AlumnoService(AlumnoRepository alumnoRepository,
                          CuentaRepository cuentaRepository,
                          MembresiaRepository membresiaRepository,
                          PasswordEncoder passwordEncoder) {
        this.alumnoRepository = alumnoRepository;
        this.cuentaRepository = cuentaRepository;
        this.membresiaRepository = membresiaRepository;
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
        alumno.setCelular(request.getCelular());
        alumno.setFechaIngreso(request.getFechaIngreso() != null ? request.getFechaIngreso() : LocalDate.now());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.setMembresia(buscarMembresiaOLanzarError(request.getMembresiaId()));
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
        alumno.setCelular(request.getCelular());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.setMembresia(buscarMembresiaOLanzarError(request.getMembresiaId()));
        alumno.getCuenta().setEmail(request.getEmail());

        if (dniCambio && alumno.getCuenta().isDebeCambiarPassword()) {
            alumno.getCuenta().setPassword(passwordEncoder.encode(dniNormalizado));
        }

        Alumno alumnoActualizado = alumnoRepository.save(alumno);
        return mapearAResponse(alumnoActualizado);
    }

    /**
     * Baja lógica: el alumno no se borra, su cuenta pasa a inactiva (y por lo tanto no puede iniciar sesión).
     */
    @Transactional
    public AlumnoResponse darDeBajaAlumno(Long id) {
        return cambiarEstado(id, false);
    }

    @Transactional
    public AlumnoResponse reactivarAlumno(Long id) {
        return cambiarEstado(id, true);
    }

    private AlumnoResponse cambiarEstado(Long id, boolean activo) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        alumno.getCuenta().setActiva(activo);
        return mapearAResponse(alumnoRepository.save(alumno));
    }

    private Alumno buscarAlumnoOLanzarError(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + id));
    }

    private Membresia buscarMembresiaOLanzarError(Long membresiaId) {
        if (membresiaId == null) {
            return null;
        }
        return membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new RegistroInvalidoException("La membresía indicada no existe"));
    }

    private AlumnoResponse mapearAResponse(Alumno alumno) {
        Membresia membresia = alumno.getMembresia();
        return new AlumnoResponse(
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getDni(),
            alumno.getCuenta().getEmail(),
            alumno.getFechaNacimiento(),
            alumno.getCelular(),
            alumno.getFechaIngreso(),
            alumno.getObjetivo(),
            alumno.getCuestionarioSalud(),
            alumno.getCuenta().getRol(),
            alumno.getCuenta().isActiva(),
            membresia != null ? membresia.getId() : null,
            membresia != null ? membresia.getNombre() : null
        );
    }
}
