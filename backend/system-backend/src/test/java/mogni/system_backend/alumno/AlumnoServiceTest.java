package mogni.system_backend.alumno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;
import mogni.system_backend.alumno.dto.AlumnoResponse;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.alumno.service.AlumnoService;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;

@ExtendWith(MockitoExtension.class)
class AlumnoServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AlumnoService alumnoService;

    private Membresia membresia;

    @BeforeEach
    void setUp() {
        membresia = new Membresia(7L, "Pro", null, 30, 5, new BigDecimal("40000"));
    }

    private RegistrarAlumnoRequest registro() {
        RegistrarAlumnoRequest request = new RegistrarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12.345.678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(LocalDate.now().minusYears(30));
        request.setCelular("1122334455");
        request.setMembresiaId(7L);
        return request;
    }

    private Alumno alumnoExistente() {
        Cuenta cuenta = new Cuenta(1L, "juan@email.com", "hash", Rol.ALUMNO, true, true);
        Alumno alumno = new Alumno();
        alumno.setId(10L);
        alumno.setNombre("Juan");
        alumno.setApellido("Perez");
        alumno.setDni("12345678");
        alumno.setFechaNacimiento(LocalDate.now().minusYears(30));
        alumno.setCuenta(cuenta);
        return alumno;
    }

    @Test
    void registrarAlumno_asignaMembresiaCelularYFechaIngreso() {
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.registrarAlumno(registro());

        assertEquals("12345678", response.getDni());
        assertEquals("1122334455", response.getCelular());
        assertEquals(7L, response.getMembresiaId());
        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(LocalDate.now(), response.getFechaIngreso());
        assertTrue(response.isActivo());
    }

    @Test
    void registrarAlumno_sinMembresiaEsValido() {
        RegistrarAlumnoRequest request = registro();
        request.setMembresiaId(null);
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.registrarAlumno(request);

        assertEquals(null, response.getMembresiaId());
    }

    @Test
    void registrarAlumno_rechazaMembresiaInexistente() {
        when(membresiaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(registro()));
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void registrarAlumno_rechazaMenoresDeEdadMinima() {
        RegistrarAlumnoRequest request = registro();
        request.setFechaNacimiento(LocalDate.now().minusYears(15));

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
    }

    @Test
    void actualizarAlumno_cambiaMembresiaYCelular() {
        Alumno alumno = alumnoExistente();
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlumnoRequest request = new ActualizarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12345678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(alumno.getFechaNacimiento());
        request.setCelular("1199887766");
        request.setMembresiaId(7L);

        AlumnoResponse response = alumnoService.actualizarAlumno(10L, request);

        assertEquals("1199887766", response.getCelular());
        assertEquals("Pro", response.getMembresiaNombre());
    }

    @Test
    void darDeBaja_desactivaLaCuentaYReactivarLaActiva() {
        Alumno alumno = alumnoExistente();
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse baja = alumnoService.darDeBajaAlumno(10L);
        assertFalse(baja.isActivo());
        assertFalse(alumno.getCuenta().isActiva());

        AlumnoResponse alta = alumnoService.reactivarAlumno(10L);
        assertTrue(alta.isActivo());
        assertNotNull(alta.getId());
    }
}
