package mogni.system_backend.alumno.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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
import mogni.system_backend.inscripcion.dto.InscripcionResponse;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.repository.CuentaRepository;

@Service
public class AlumnoService {

    private static final int EDAD_MINIMA = 16;
    private static final int DURACION_INSCRIPCION_DIAS = 30;

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

        String dniNormalizado = normalizarDni(request.getDni());
        String email = normalizarEmail(request.getEmail());

        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new RegistroInvalidoException(
                "El alumno debe tener al menos " + EDAD_MINIMA + " años para registrarse");
        }

        if (alumnoRepository.existsByDni(dniNormalizado)) {
            throw new RegistroInvalidoException("Ya existe un alumno registrado con ese DNI");
        }

        if (cuentaRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        Membresia membresia = buscarMembresiaOLanzarError(request.getMembresiaId());

        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(email);
        cuenta.setPassword(passwordEncoder.encode(dniNormalizado));
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setDebeCambiarPassword(true);
        cuenta.setActiva(true);

        Alumno alumno = new Alumno();
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEdad(edad);
        alumno.setTelefono(request.getTelefono());
        alumno.setSexo(request.getSexo());
        alumno.setDni(dniNormalizado);
        alumno.setEmail(email);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.setCuenta(cuenta);
        alumno.agregarInscripcion(nuevaInscripcion(membresia));

        Alumno alumnoGuardado = alumnoRepository.save(alumno);

        return mapearAResponse(alumnoGuardado);
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarAlumnos(boolean activos) {
        return alumnoRepository.findByCuentaActiva(activos)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlumnoResponse obtenerAlumno(Long id) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        return mapearAResponse(alumno);
    }

    /** Datos del alumno dueño de la cuenta con ese email (el usuario logueado). */
    @Transactional(readOnly = true)
    public AlumnoResponse obtenerAlumnoPorEmail(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        return mapearAResponse(alumno);
    }

    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, ActualizarAlumnoRequest request) {
        Alumno alumno = buscarAlumnoOLanzarError(id);

        String dniNormalizado = normalizarDni(request.getDni());
        String email = normalizarEmail(request.getEmail());
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
        if (!emailActual.equalsIgnoreCase(email)
                && cuentaRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEdad(edad);
        alumno.setTelefono(request.getTelefono());
        alumno.setSexo(request.getSexo());
        alumno.setDni(dniNormalizado);
        alumno.setEmail(email);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.getCuenta().setEmail(email);
        cambiarMembresia(alumno, request.getMembresiaId());

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

    /**
     * Si la membresía elegida es distinta a la de la inscripción activa, cancela la activa
     * y crea una inscripción nueva. Null deja al alumno sin inscripción activa.
     */
    private void cambiarMembresia(Alumno alumno, Long membresiaId) {
        Optional<Inscripcion> activa = alumno.buscarInscripcionActiva();
        Long membresiaActualId = activa.map(i -> i.getMembresia().getId()).orElse(null);
        if (Objects.equals(membresiaActualId, membresiaId)) {
            return;
        }
        activa.ifPresent(i -> i.setEstado(EstadoInscripcion.CANCELADA));
        if (membresiaId != null) {
            alumno.agregarInscripcion(nuevaInscripcion(buscarMembresiaOLanzarError(membresiaId)));
        }
    }

    private Inscripcion nuevaInscripcion(Membresia membresia) {
        LocalDate hoy = LocalDate.now();
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setFechaInicio(hoy);
        inscripcion.setFechaFin(hoy.plusDays(DURACION_INSCRIPCION_DIAS));
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setMembresia(membresia);
        return inscripcion;
    }

    private String normalizarDni(String dni) {
        String dniNormalizado = dni.replaceAll("[^0-9]", "");
        if (dniNormalizado.isEmpty()) {
            throw new RegistroInvalidoException("El DNI debe contener números");
        }
        return dniNormalizado;
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Membresia buscarMembresiaOLanzarError(Long membresiaId) {
        if (membresiaId == null) {
            throw new RegistroInvalidoException("Debe asignarse una membresía");
        }
        return membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new RegistroInvalidoException("La membresía indicada no existe"));
    }

    private AlumnoResponse mapearAResponse(Alumno alumno) {
        Optional<Inscripcion> activa = alumno.buscarInscripcionActiva();
        Membresia membresia = activa.map(Inscripcion::getMembresia).orElse(null);
        return new AlumnoResponse(
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getEdad(),
            alumno.getTelefono(),
            alumno.getSexo(),
            alumno.getDni(),
            alumno.getEmail(),
            alumno.getFechaNacimiento(),
            alumno.getObjetivo(),
            alumno.getCuestionarioSalud(),
            alumno.getCuenta().getRol(),
            alumno.getCuenta().isActiva(),
            membresia != null ? membresia.getId() : null,
            membresia != null ? membresia.getNombre() : null,
            activa.map(Inscripcion::getFechaInicio).orElse(null),
            activa.map(Inscripcion::getFechaFin).orElse(null),
            alumno.buscarInscripcionActual().map(this::mapearInscripcion).orElse(null)
        );
    }

    private InscripcionResponse mapearInscripcion(Inscripcion inscripcion) {
        return new InscripcionResponse(
            inscripcion.getMembresia().getId(),
            inscripcion.getMembresia().getNombre(),
            inscripcion.getEstado(),
            inscripcion.getFechaInicio(),
            inscripcion.getFechaFin()
        );
    }
}
