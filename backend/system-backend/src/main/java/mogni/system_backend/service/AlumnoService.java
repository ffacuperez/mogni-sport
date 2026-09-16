package mogni.system_backend.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.dto.AlumnoResponse;
import mogni.system_backend.dto.RegistrarAlumnoRequest;
import mogni.system_backend.entidades.Alumno;
import mogni.system_backend.entidades.Cuenta;
import mogni.system_backend.entidades.Rol;
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

        return new AlumnoResponse(
            alumnoGuardado.getId(),
            alumnoGuardado.getNombre(),
            alumnoGuardado.getApellido(),
            alumnoGuardado.getDni(),
            alumnoGuardado.getCuenta().getEmail(),
            alumnoGuardado.getFechaNacimiento(),
            alumnoGuardado.getObjetivo(),
            alumnoGuardado.getCuestionarioSalud(),
            alumnoGuardado.getCuenta().getRol()
        );
    }
}