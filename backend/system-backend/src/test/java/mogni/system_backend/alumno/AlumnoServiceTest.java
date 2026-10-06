package mogni.system_backend.alumno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
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
        membresia = new Membresia(7L, "Pro", 40000.0, 5, null);
    }

    private RegistrarAlumnoRequest registro() {
        RegistrarAlumnoRequest request = new RegistrarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12.345.678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(LocalDate.now().minusYears(30));
        request.setTelefono("1122334455");
        request.setSexo("Masculino");
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

    private ActualizarAlumnoRequest actualizacion(Alumno alumno, Long membresiaId) {
        ActualizarAlumnoRequest request = new ActualizarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12345678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(alumno.getFechaNacimiento());
        request.setTelefono("1199887766");
        request.setMembresiaId(membresiaId);
        return request;
    }

    private Alumno guardarYCapturar() {
        ArgumentCaptor<Alumno> captor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(captor.capture());
        return captor.getValue();
    }

    // Pruebas 1 y 2: inscripción válida (con y sin opcionales) crea una Inscripcion ACTIVA
    @Test
    void registrarAlumno_creaInscripcionActivaConLaMembresia() {
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.registrarAlumno(registro());

        assertEquals("12345678", response.getDni());
        assertEquals("1122334455", response.getTelefono());
        assertEquals("Masculino", response.getSexo());
        assertEquals(30, response.getEdad());
        assertEquals("juan@email.com", response.getEmail());
        assertEquals(7L, response.getMembresiaId());
        assertEquals("Pro", response.getMembresiaNombre());
        assertTrue(response.isActivo());

        Alumno guardado = guardarYCapturar();
        assertEquals(1, guardado.getInscripciones().size());
        Inscripcion inscripcion = guardado.getInscripciones().get(0);
        assertEquals(EstadoInscripcion.ACTIVA, inscripcion.getEstado());
        assertEquals(LocalDate.now(), inscripcion.getFechaInicio());
        assertNull(inscripcion.getFechaFin());
        assertEquals(membresia, inscripcion.getMembresia());
        assertEquals(guardado, inscripcion.getAlumno());

        // HU-45: al inscribirse se genera la primera cuota PENDIENTE
        assertEquals(1, inscripcion.getCuotas().size());
        Cuota cuota = inscripcion.getCuotas().get(0);
        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
        assertEquals(LocalDate.now().plusDays(30), cuota.getFechaVencimiento());
        assertEquals(0, BigDecimal.valueOf(40000).compareTo(cuota.getMontoEsperado()));
    }

    @Test
    void registrarAlumno_conCamposOpcionales() {
        RegistrarAlumnoRequest request = registro();
        request.setObjetivo("Ganar fuerza");
        request.setCuestionarioSalud("Asma leve");
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.registrarAlumno(request);

        assertEquals("Ganar fuerza", response.getObjetivo());
        assertEquals("Asma leve", response.getCuestionarioSalud());
    }

    // Prueba 4
    @Test
    void registrarAlumno_rechazaSinMembresia() {
        RegistrarAlumnoRequest request = registro();
        request.setMembresiaId(null);

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void registrarAlumno_rechazaMembresiaInexistente() {
        when(membresiaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(registro()));
        verify(alumnoRepository, never()).save(any());
    }

    // Prueba 5
    @Test
    void registrarAlumno_rechazaMenoresDeEdadMinima() {
        RegistrarAlumnoRequest request = registro();
        request.setFechaNacimiento(LocalDate.now().minusYears(16).plusDays(1));

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void registrarAlumno_aceptaAlumnoQueCumple16Hoy() {
        RegistrarAlumnoRequest request = registro();
        request.setFechaNacimiento(LocalDate.now().minusYears(16));
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(alumnoService.registrarAlumno(request));
    }

    // Prueba 6
    @Test
    void registrarAlumno_rechazaDniDuplicado() {
        when(alumnoRepository.existsByDni("12345678")).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(registro()));
        verify(alumnoRepository, never()).save(any());
    }

    // Prueba 7 (sin distinguir mayúsculas)
    @Test
    void registrarAlumno_rechazaEmailDuplicado() {
        RegistrarAlumnoRequest request = registro();
        request.setEmail("  JUAN@Email.com ");
        when(cuentaRepository.existsByEmailIgnoreCase("juan@email.com")).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
        verify(alumnoRepository, never()).save(any());
    }

    // Pruebas 9 y 10
    @Test
    void registrarAlumno_creaCuentaAlumnoConEmailYDniComoPassword() {
        RegistrarAlumnoRequest request = registro();
        request.setEmail(" Juan@Email.com ");
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(passwordEncoder.encode("12345678")).thenReturn("hash-del-dni");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        alumnoService.registrarAlumno(request);

        Cuenta cuenta = guardarYCapturar().getCuenta();
        assertEquals(Rol.ALUMNO, cuenta.getRol());
        assertEquals("juan@email.com", cuenta.getEmail());
        assertEquals("juan@email.com", guardarYCapturar().getEmail());
        assertEquals("hash-del-dni", cuenta.getPassword());
        assertTrue(cuenta.isActiva());
        assertTrue(cuenta.isDebeCambiarPassword());
    }

    @Test
    void registrarAlumno_rechazaDniSinNumeros() {
        RegistrarAlumnoRequest request = registro();
        request.setDni("abc");

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
    }

    @Test
    void actualizarAlumno_cambiarDniActualizaLaContraseniaSiTodaviaEsLaInicial() {
        Alumno alumno = alumnoExistente(); // debeCambiarPassword = true
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(passwordEncoder.encode("99888777")).thenReturn("hash-nuevo-dni");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlumnoRequest request = actualizacion(alumno, null);
        request.setDni("99.888.777");
        AlumnoResponse response = alumnoService.actualizarAlumno(10L, request);

        assertEquals("99888777", response.getDni());
        assertEquals("hash-nuevo-dni", alumno.getCuenta().getPassword());
    }

    @Test
    void actualizarAlumno_cambiarDniNoTocaLaContraseniaSiYaLaCambio() {
        Alumno alumno = alumnoExistente();
        alumno.getCuenta().setDebeCambiarPassword(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlumnoRequest request = actualizacion(alumno, null);
        request.setDni("99888777");
        AlumnoResponse response = alumnoService.actualizarAlumno(10L, request);

        assertEquals("99888777", response.getDni());
        assertEquals("hash", alumno.getCuenta().getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizarAlumno_rechazaMenorDe16ConMensajeDeEdicion() {
        Alumno alumno = alumnoExistente();
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        ActualizarAlumnoRequest request = actualizacion(alumno, null);
        request.setFechaNacimiento(LocalDate.now().minusYears(15));
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.actualizarAlumno(10L, request));

        assertEquals("El alumno debe tener al menos 16 años", ex.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void actualizarAlumno_asignaMembresiaCreandoInscripcion() {
        Alumno alumno = alumnoExistente();
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.actualizarAlumno(10L, actualizacion(alumno, 7L));

        assertEquals("1199887766", response.getTelefono());
        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(1, alumno.getInscripciones().size());
    }

    @Test
    void actualizarAlumno_cambiarMembresiaCancelaLaActivaYCreaOtra() {
        Alumno alumno = alumnoExistente();
        Membresia anterior = new Membresia(3L, "Basica", 20000.0, 2, null);
        Inscripcion vieja = new Inscripcion(1L, LocalDate.now().minusDays(10), null,
                EstadoInscripcion.ACTIVA, null, anterior);
        alumno.agregarInscripcion(vieja);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.actualizarAlumno(10L, actualizacion(alumno, 7L));

        assertEquals(EstadoInscripcion.CANCELADA, vieja.getEstado());
        assertEquals(LocalDate.now(), vieja.getFechaFin());
        assertEquals(2, alumno.getInscripciones().size());
        Inscripcion nueva = alumno.getInscripciones().get(1);
        assertNull(nueva.getFechaFin());
        assertEquals(1, nueva.getCuotas().size());
        assertEquals(EstadoCuota.PENDIENTE, nueva.getCuotas().get(0).getEstado());
        assertEquals(7L, response.getMembresiaId());
    }

    @Test
    void actualizarAlumno_mismaMembresiaNoCreaOtraInscripcion() {
        Alumno alumno = alumnoExistente();
        Inscripcion actual = new Inscripcion(1L, LocalDate.now(), null,
                EstadoInscripcion.ACTIVA, null, membresia);
        alumno.agregarInscripcion(actual);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        alumnoService.actualizarAlumno(10L, actualizacion(alumno, 7L));

        assertEquals(1, alumno.getInscripciones().size());
        assertEquals(EstadoInscripcion.ACTIVA, actual.getEstado());
    }

    @Test
    void actualizarAlumno_sinMembresiaCancelaLaActiva() {
        Alumno alumno = alumnoExistente();
        Inscripcion actual = new Inscripcion(1L, LocalDate.now(), null,
                EstadoInscripcion.ACTIVA, null, membresia);
        alumno.agregarInscripcion(actual);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.actualizarAlumno(10L, actualizacion(alumno, null));

        assertEquals(EstadoInscripcion.CANCELADA, actual.getEstado());
        assertEquals(LocalDate.now(), actual.getFechaFin());
        assertNull(response.getMembresiaId());
    }

    @Test
    void obtenerAlumnoPorEmail_devuelveLosDatosConLaInscripcionActiva() {
        Alumno alumno = alumnoExistente();
        LocalDate inicio = LocalDate.now().minusDays(5);
        alumno.agregarInscripcion(new Inscripcion(1L, inicio, null, EstadoInscripcion.ACTIVA, null, membresia));
        when(alumnoRepository.findByCuentaEmailIgnoreCase("juan@email.com")).thenReturn(Optional.of(alumno));

        AlumnoResponse response = alumnoService.obtenerAlumnoPorEmail("juan@email.com");

        assertEquals("Juan", response.getNombre());
        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(inicio, response.getFechaInicioInscripcion());
    }

    @Test
    void obtenerAlumnoPorEmail_fallaSiLaCuentaNoEsDeUnAlumno() {
        when(alumnoRepository.findByCuentaEmailIgnoreCase("admin@mogni.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> alumnoService.obtenerAlumnoPorEmail("admin@mogni.com"));
    }

    @Test
    void listarAlumnos_filtraPorEstadoDeLaCuenta() {
        Alumno activo = alumnoExistente();
        Alumno inactivo = alumnoExistente();
        inactivo.setId(11L);
        inactivo.getCuenta().setActiva(false);
        when(alumnoRepository.findByCuentaActiva(true)).thenReturn(List.of(activo));
        when(alumnoRepository.findByCuentaActiva(false)).thenReturn(List.of(inactivo));

        List<AlumnoResponse> activos = alumnoService.listarAlumnos(true);
        List<AlumnoResponse> inactivos = alumnoService.listarAlumnos(false);

        assertEquals(1, activos.size());
        assertTrue(activos.get(0).isActivo());
        assertEquals(1, inactivos.size());
        assertFalse(inactivos.get(0).isActivo());
    }

    @Test
    void obtenerAlumno_inscripcionActualEsLaActivaSiTiene() {
        Alumno alumno = alumnoExistente();
        Membresia anterior = new Membresia(3L, "Basica", 20000.0, 2, null);
        alumno.agregarInscripcion(new Inscripcion(1L, LocalDate.now().minusDays(60), LocalDate.now().minusDays(30), EstadoInscripcion.CANCELADA, null, anterior));
        alumno.agregarInscripcion(new Inscripcion(2L, LocalDate.now(), null, EstadoInscripcion.ACTIVA, null, membresia));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        AlumnoResponse response = alumnoService.obtenerAlumno(10L);

        assertEquals("Pro", response.getInscripcionActual().getMembresiaNombre());
        assertEquals(EstadoInscripcion.ACTIVA, response.getInscripcionActual().getEstado());
    }

    @Test
    void obtenerAlumno_sinActivaMuestraLaUltimaConSuEstado() {
        Alumno alumno = alumnoExistente();
        Membresia anterior = new Membresia(3L, "Basica", 20000.0, 2, null);
        LocalDate inicio = LocalDate.now().minusDays(40);
        alumno.agregarInscripcion(new Inscripcion(1L, LocalDate.now().minusDays(100), LocalDate.now().minusDays(70), EstadoInscripcion.CANCELADA, null, anterior));
        alumno.agregarInscripcion(new Inscripcion(2L, inicio, inicio.plusDays(30), EstadoInscripcion.CANCELADA, null, membresia));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        AlumnoResponse response = alumnoService.obtenerAlumno(10L);

        // Los campos de la inscripción activa quedan vacíos (los usan la edición y cuotas)...
        assertNull(response.getMembresiaId());
        // ...y la última inscripción se informa con su estado
        assertEquals("Pro", response.getInscripcionActual().getMembresiaNombre());
        assertEquals(EstadoInscripcion.CANCELADA, response.getInscripcionActual().getEstado());
        assertEquals(inicio, response.getInscripcionActual().getFechaInicio());
        assertEquals(inicio.plusDays(30), response.getInscripcionActual().getFechaFin());
    }

    @Test
    void obtenerAlumno_sinInscripcionesNoInformaInscripcionActual() {
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumnoExistente()));

        assertNull(alumnoService.obtenerAlumno(10L).getInscripcionActual());
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
