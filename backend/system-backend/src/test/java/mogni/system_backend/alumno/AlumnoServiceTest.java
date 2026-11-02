package mogni.system_backend.alumno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;
import mogni.system_backend.alumno.dto.AlumnoResponse;
import mogni.system_backend.alumno.dto.CambiarMembresiaRequest;
import mogni.system_backend.alumno.dto.CambioMembresiaResponse;
import mogni.system_backend.alumno.dto.FiltroEstadoPlan;
import mogni.system_backend.alumno.dto.ReactivarAlumnoRequest;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.alumno.service.AlumnoService;
import mogni.system_backend.alumno.service.BajaAlumnoService;
import mogni.system_backend.plan.service.PlanAsignadoService;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

@ExtendWith(MockitoExtension.class)
class AlumnoServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CancelacionPorSistemaService cancelacionPorSistemaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PlanAsignadoRepository planAsignadoRepository;

    @Mock
    private PlanAsignadoService planAsignadoService;

    private AlumnoService alumnoService;

    private Membresia membresia;
    private final LocalDate hoy = ZonaHoraria.hoy();

    @BeforeEach
    void setUp() {
        membresia = new Membresia(7L, "Pro", 40000.0, 5, null);
        // La baja real (no un mock), así se prueba que cancela la inscripción y pide cancelar los turnos
        alumnoService = new AlumnoService(alumnoRepository, cuentaRepository, membresiaRepository, pagoRepository,
                new BajaAlumnoService(cancelacionPorSistemaService, planAsignadoService), passwordEncoder, planAsignadoRepository);
    }

    private RegistrarAlumnoRequest registro() {
        RegistrarAlumnoRequest request = new RegistrarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12.345.678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(ZonaHoraria.hoy().minusYears(30));
        request.setTelefono("1122334455");
        request.setSexo("Masculino");
        request.setMembresiaId(7L);
        request.setMedioDePago(MedioDePago.EFECTIVO);
        return request;
    }

    private Alumno alumnoExistente() {
        Cuenta cuenta = new Cuenta(1L, "juan@email.com", "hash", Rol.ALUMNO, true, true, null);
        Alumno alumno = new Alumno();
        alumno.setId(10L);
        alumno.setNombre("Juan");
        alumno.setApellido("Perez");
        alumno.setDni("12345678");
        alumno.setFechaNacimiento(ZonaHoraria.hoy().minusYears(30));
        alumno.setCuenta(cuenta);
        return alumno;
    }

    /**
     * Alumno activo con una inscripción ACTIVA de la membresía dada y una cuota pagada que vence dentro de
     * {@code diasHastaVencimiento} días, con su pago (recargo opcional).
     */
    private Alumno alumnoConCuota(Membresia actual, int diasHastaVencimiento, BigDecimal recargo) {
        Alumno alumno = alumnoExistente();
        Inscripcion inscripcion = new Inscripcion(1L, hoy.minusDays(60), null, EstadoInscripcion.ACTIVA, null, actual);
        alumno.agregarInscripcion(inscripcion);
        Cuota cuota = inscripcion.agregarCuota(hoy.plusDays(diasHastaVencimiento).minusDays(30));
        cuota.setId(100L);
        if (recargo != null) {
            Pago pago = Pago.de(cuota, recargo, MedioDePago.TRANSFERENCIA, hoy.minusDays(20));
            lenientPago(pago);
        }
        return alumno;
    }

    private Pago pagoGuardado;

    private void lenientPago(Pago pago) {
        pagoGuardado = pago;
        org.mockito.Mockito.lenient().when(pagoRepository.findFirstByCuotaIdOrderByIdAsc(100L))
                .thenReturn(Optional.of(pago));
    }

    private ActualizarAlumnoRequest actualizacion(Alumno alumno) {
        ActualizarAlumnoRequest request = new ActualizarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12345678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(alumno.getFechaNacimiento());
        request.setTelefono("1199887766");
        return request;
    }

    private Alumno guardarYCapturar() {
        ArgumentCaptor<Alumno> captor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(captor.capture());
        return captor.getValue();
    }

    private Pago pagoCapturado() {
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        return captor.getValue();
    }

    // --- Alta (MOGNI-73) ---

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
        assertEquals(ZonaHoraria.hoy(), inscripcion.getFechaInicio());
        assertNull(inscripcion.getFechaFin());
        assertEquals(membresia, inscripcion.getMembresia());
        assertEquals(guardado, inscripcion.getAlumno());
    }

    @Test
    void registrarAlumno_creaLaPrimeraCuotaDeHoyA30DiasConSuPago() {
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        alumnoService.registrarAlumno(registro());

        Inscripcion inscripcion = guardarYCapturar().getInscripciones().get(0);
        assertEquals(1, inscripcion.getCuotas().size());
        Cuota cuota = inscripcion.getCuotas().get(0);
        assertEquals(hoy, cuota.getFechaInicio());
        assertEquals(hoy.plusDays(30), cuota.getFechaVencimiento());
        assertEquals(new BigDecimal("40000.00"), cuota.getPrecio());
        assertSame(membresia, cuota.membresiaCubierta());

        Pago pago = pagoCapturado();
        assertSame(cuota, pago.getCuota());
        assertEquals(new BigDecimal("40000.00"), pago.getMonto());
        assertEquals(new BigDecimal("0.00"), pago.getRecargo());
        assertEquals(MedioDePago.EFECTIVO, pago.getMedioDePago());
        assertEquals(hoy, pago.getFechaPago());
    }

    @Test
    void registrarAlumno_rechazaSinMedioDePago() {
        RegistrarAlumnoRequest request = registro();
        request.setMedioDePago(null);
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
        verify(alumnoRepository, never()).save(any());
        verify(pagoRepository, never()).save(any());
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
        request.setFechaNacimiento(ZonaHoraria.hoy().minusYears(16).plusDays(1));

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.registrarAlumno(request));
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void registrarAlumno_aceptaAlumnoQueCumple16Hoy() {
        RegistrarAlumnoRequest request = registro();
        request.setFechaNacimiento(ZonaHoraria.hoy().minusYears(16));
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

    // --- Edición de datos ---

    @Test
    void actualizarAlumno_cambiarDniActualizaLaContraseniaSiTodaviaEsLaInicial() {
        Alumno alumno = alumnoExistente(); // debeCambiarPassword = true
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(passwordEncoder.encode("99888777")).thenReturn("hash-nuevo-dni");
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlumnoRequest request = actualizacion(alumno);
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

        ActualizarAlumnoRequest request = actualizacion(alumno);
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

        ActualizarAlumnoRequest request = actualizacion(alumno);
        request.setFechaNacimiento(ZonaHoraria.hoy().minusYears(15));
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.actualizarAlumno(10L, request));

        assertEquals("El alumno debe tener al menos 16 años", ex.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    // --- Cambio de membresía (reglas M1 a M9, endpoint propio) ---

    private CambiarMembresiaRequest cambioA(Long membresiaId, MedioDePago medio) {
        CambiarMembresiaRequest request = new CambiarMembresiaRequest();
        request.setMembresiaId(membresiaId);
        request.setMedioDePago(medio);
        return request;
    }

    private void prepararCambio(Alumno alumno, Membresia nueva) {
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(nueva.getId())).thenReturn(Optional.of(nueva));
    }

    private void prepararCambioQueSeGuarda(Alumno alumno, Membresia nueva) {
        prepararCambio(alumno, nueva);
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void actualizarAlumno_noTocaLaMembresia() {
        Alumno alumno = alumnoConCuota(membresia, 20, BigDecimal.ZERO);
        Inscripcion actual = alumno.getInscripciones().get(0);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = alumnoService.actualizarAlumno(10L, actualizacion(alumno));

        assertEquals(EstadoInscripcion.ACTIVA, actual.getEstado());
        assertEquals(1, alumno.getInscripciones().size());
        assertEquals(7L, response.getMembresiaId());
    }

    @Test
    void cambioDeMembresia_subeDe3a5DiasCreaUnaCuotaYUnPagoPorLaDiferencia() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Alumno alumno = alumnoConCuota(tresDias, 20, BigDecimal.ZERO);
        Inscripcion vieja = alumno.getInscripciones().get(0);
        Cuota original = vieja.getCuotas().get(0);
        prepararCambioQueSeGuarda(alumno, cincoDias);

        AlumnoResponse response = alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO));

        // M4: la inscripción vieja termina hoy y su cuota queda tal cual se pagó
        assertEquals(EstadoInscripcion.CANCELADA, vieja.getEstado());
        assertEquals(hoy, vieja.getFechaFin());
        assertEquals(List.of(original), vieja.getCuotas());
        assertEquals(new BigDecimal("20000.00"), original.getPrecio());
        assertEquals(new BigDecimal("20000.00"), pagoGuardado.getMonto());
        // Inscripción nueva desde hoy con una cuota de hoy al mismo vencimiento, por la diferencia
        Inscripcion nueva = alumno.getInscripciones().get(1);
        assertEquals(EstadoInscripcion.ACTIVA, nueva.getEstado());
        assertEquals(hoy, nueva.getFechaInicio());
        assertSame(cincoDias, nueva.getMembresia());
        Cuota cuotaNueva = nueva.getCuotas().get(0);
        assertEquals(hoy, cuotaNueva.getFechaInicio());
        assertEquals(original.getFechaVencimiento(), cuotaNueva.getFechaVencimiento());
        assertEquals(new BigDecimal("5000.00"), cuotaNueva.getPrecio());
        // M6: el pago de la diferencia tiene su fecha y su medio
        Pago pago = pagoCapturado();
        assertSame(cuotaNueva, pago.getCuota());
        assertEquals(new BigDecimal("5000.00"), pago.getMonto());
        assertEquals(new BigDecimal("0.00"), pago.getRecargo());
        assertEquals(MedioDePago.EFECTIVO, pago.getMedioDePago());
        assertEquals(hoy, pago.getFechaPago());
        assertEquals(5L, response.getMembresiaId());
    }

    @Test
    void cambioDeMembresia_bajaDe5a3DiasCreaCuotaYPagoDeCeroConElMedioAnterior() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Alumno alumno = alumnoConCuota(cincoDias, 20, BigDecimal.ZERO);
        prepararCambioQueSeGuarda(alumno, tresDias);

        alumnoService.cambiarMembresia(10L, cambioA(3L, null));

        Cuota cuotaNueva = alumno.getInscripciones().get(1).getCuotas().get(0);
        assertEquals(new BigDecimal("0.00"), cuotaNueva.getPrecio());
        Pago pago = pagoCapturado();
        assertEquals(new BigDecimal("0.00"), pago.getMonto());
        // Sin cobro: se copia el medio del pago anterior (es irrelevante)
        assertEquals(MedioDePago.TRANSFERENCIA, pago.getMedioDePago());
        assertEquals(new BigDecimal("25000.00"), pagoGuardado.getMonto());
    }

    @Test
    void cambioDeMembresia_conRecargoElPagoOriginalQuedaIgualYLaDiferenciaEsSinRecargo() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Alumno alumno = alumnoConCuota(tresDias, 20, new BigDecimal("2000.00"));
        prepararCambioQueSeGuarda(alumno, cincoDias);

        alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO));

        assertEquals(new BigDecimal("22000.00"), pagoGuardado.getMonto());
        assertEquals(new BigDecimal("2000.00"), pagoGuardado.getRecargo());
        assertEquals(new BigDecimal("5000.00"), pagoCapturado().getMonto());
    }

    @Test
    void cambioDeMembresia_dosCambiosEnElMismoPeriodoNoCobranDosVecesLoMismo() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Membresia full = new Membresia(6L, "Full", 30000.0, 6, null);
        Alumno alumno = alumnoConCuota(tresDias, 20, BigDecimal.ZERO);
        prepararCambioQueSeGuarda(alumno, cincoDias);
        when(membresiaRepository.findById(6L)).thenReturn(Optional.of(full));
        alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO));
        alumno.getInscripciones().get(1).getCuotas().get(0).setId(101L);

        alumnoService.cambiarMembresia(10L, cambioA(6L, MedioDePago.EFECTIVO));

        // Ya pagó $20.000 + $5.000 por este período: a $30.000 le faltan $5.000
        assertEquals(new BigDecimal("5000.00"), alumno.getInscripciones().get(2).getCuotas().get(0).getPrecio());
    }

    @Test
    void cambioDeMembresia_aUnaMasCaraSinMedioDePagoFalla() {
        Alumno alumno = alumnoConCuota(new Membresia(3L, "3 días", 20000.0, 3, null), 20, BigDecimal.ZERO);
        prepararCambio(alumno, new Membresia(5L, "5 días", 25000.0, 5, null));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, null)));

        assertTrue(ex.getMessage().contains("medio"));
        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(0).getEstado());
        verify(alumnoRepository, never()).save(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void cambioDeMembresia_rechazaConLaCuotaVencida() {
        Alumno alumno = alumnoConCuota(membresia, -1, null);
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)));

        assertTrue(ex.getMessage().contains("vencida"));
        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(0).getEstado());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void cambioDeMembresia_rechazaConLaCuotaEnMora() {
        Alumno alumno = alumnoConCuota(membresia, -4, null);
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)));

        assertTrue(ex.getMessage().contains("en mora"));
    }

    @Test
    void cambioDeMembresia_rechazaSiYaPagoElMesSiguiente() {
        // M2: renovó por adelantado: su última cuota empieza en 3 días
        Alumno alumno = alumnoConCuota(membresia, 33, null);
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)));

        assertTrue(ex.getMessage().contains("ya pagó el período que empieza"));
    }

    @Test
    void cambioDeMembresia_seAceptaElDiaQueEmpiezaElPeriodoRenovado() {
        Alumno alumno = alumnoConCuota(membresia, 30, null); // la cuota empieza hoy
        prepararCambioQueSeGuarda(alumno, new Membresia(5L, "Otra", 50000.0, 1, null));

        assertEquals(5L, alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)).getMembresiaId());
    }

    @Test
    void cambioDeMembresia_rechazaLaMismaMembresia() {
        Alumno alumno = alumnoConCuota(membresia, 20, null);
        prepararCambio(alumno, membresia);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(7L, MedioDePago.EFECTIVO)));

        assertTrue(ex.getMessage().contains("distinta"));
    }

    @Test
    void cambioDeMembresia_sinCuotasCreaLaInscripcionSinCuotaNiPago() {
        Alumno alumno = alumnoExistente();
        prepararCambioQueSeGuarda(alumno, membresia);

        AlumnoResponse response = alumnoService.cambiarMembresia(10L, cambioA(7L, null));

        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(1, alumno.getInscripciones().size());
        assertTrue(alumno.getInscripciones().get(0).getCuotas().isEmpty());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void cambioDeMembresia_rechazaAUnAlumnoInactivo() {
        Alumno alumno = alumnoConCuota(membresia, 20, null);
        alumno.getCuenta().setActiva(false);
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)));

        assertTrue(ex.getMessage().contains("inactivo"));
    }

    @Test
    void vistaPreviaDelCambio_aUnaMasCaraMuestraLaDiferenciaYPideMedio() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Alumno alumno = alumnoConCuota(tresDias, 20, null);
        prepararCambio(alumno, cincoDias);

        CambioMembresiaResponse vista = alumnoService.previsualizarCambioMembresia(10L, 5L);

        assertTrue(vista.isPermitido());
        assertEquals("3 días", vista.getMembresiaActual());
        assertEquals("5 días", vista.getMembresiaNueva());
        assertEquals(hoy, vista.getDesde());
        assertEquals(hoy.plusDays(20), vista.getHasta());
        assertEquals(new BigDecimal("20000.00"), vista.getPrecioPagado());
        assertEquals(new BigDecimal("25000.00"), vista.getPrecioNuevo());
        assertEquals(new BigDecimal("5000.00"), vista.getDiferenciaACobrar());
        assertTrue(vista.isRequiereMedioDePago());
        assertFalse(vista.isSinReembolso());
    }

    @Test
    void vistaPreviaDelCambio_aUnaMasBarataEsSinReembolsoYSinMedio() {
        Membresia tresDias = new Membresia(3L, "3 días", 20000.0, 3, null);
        Membresia cincoDias = new Membresia(5L, "5 días", 25000.0, 5, null);
        Alumno alumno = alumnoConCuota(cincoDias, 20, null);
        prepararCambio(alumno, tresDias);

        CambioMembresiaResponse vista = alumnoService.previsualizarCambioMembresia(10L, 3L);

        assertTrue(vista.isPermitido());
        assertEquals(new BigDecimal("0.00"), vista.getDiferenciaACobrar());
        assertTrue(vista.isSinReembolso());
        assertFalse(vista.isRequiereMedioDePago());
    }

    @Test
    void vistaPreviaDelCambio_enMoraNoSePermiteYDicePorQue() {
        Alumno alumno = alumnoConCuota(membresia, -3, null);
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        CambioMembresiaResponse vista = alumnoService.previsualizarCambioMembresia(10L, 5L);

        assertFalse(vista.isPermitido());
        assertTrue(vista.getMotivo().contains("en mora"));
    }

    // --- Consultas ---

    @Test
    void obtenerAlumnoPorEmail_devuelveLosDatosConLaInscripcionActiva() {
        Alumno alumno = alumnoExistente();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(5);
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

        List<AlumnoResponse> activos = alumnoService.listarAlumnos(true, null);
        List<AlumnoResponse> inactivos = alumnoService.listarAlumnos(false, null);

        assertEquals(1, activos.size());
        assertTrue(activos.get(0).isActivo());
        assertNull(activos.get(0).getReactivacionRequierePago());
        assertEquals(1, inactivos.size());
        assertFalse(inactivos.get(0).isActivo());
        assertTrue(inactivos.get(0).getReactivacionRequierePago());
    }

    private Alumno alumnoConId(Long id) {
        Alumno alumno = alumnoExistente();
        alumno.setId(id);
        return alumno;
    }

    private PlanAsignado planActivo(Long id, Alumno alumno, String nombre) {
        PlanAsignado plan = new PlanAsignado(alumno, hoy);
        plan.setId(id);
        plan.setNombre(nombre);
        return plan;
    }

    @Test
    void listarAlumnos_conPlanActivoMuestraElNombreDelPlan() {
        Alumno conPlan = alumnoConId(10L);
        when(alumnoRepository.findByCuentaActiva(true)).thenReturn(List.of(conPlan));
        when(planAsignadoRepository.findByEstado(EstadoPlanAsignado.ACTIVO))
                .thenReturn(List.of(planActivo(5L, conPlan, "Fuerza intermedio")));

        AlumnoResponse response = alumnoService.listarAlumnos(true, null).get(0);

        assertEquals(5L, response.getPlanActivo().getId());
        assertEquals("Fuerza intermedio", response.getPlanActivo().getNombre());
    }

    @Test
    void listarAlumnos_sinPlanActivoDevuelvePlanNull() {
        when(alumnoRepository.findByCuentaActiva(true)).thenReturn(List.of(alumnoConId(10L)));
        when(planAsignadoRepository.findByEstado(EstadoPlanAsignado.ACTIVO)).thenReturn(List.of());

        assertNull(alumnoService.listarAlumnos(true, null).get(0).getPlanActivo());
    }

    @Test
    void listarAlumnos_filtraPorEstadoDelPlan() {
        Alumno conPlan = alumnoConId(10L);
        Alumno sinPlan = alumnoConId(11L);
        when(alumnoRepository.findByCuentaActiva(true)).thenReturn(List.of(conPlan, sinPlan));
        when(planAsignadoRepository.findByEstado(EstadoPlanAsignado.ACTIVO))
                .thenReturn(List.of(planActivo(5L, conPlan, "Fuerza intermedio")));

        List<AlumnoResponse> todos = alumnoService.listarAlumnos(true, null);
        List<AlumnoResponse> soloConPlan = alumnoService.listarAlumnos(true, FiltroEstadoPlan.ACTIVO);
        List<AlumnoResponse> soloSinPlan = alumnoService.listarAlumnos(true, FiltroEstadoPlan.SIN_PLAN);

        assertEquals(2, todos.size());
        assertEquals(List.of(10L), soloConPlan.stream().map(AlumnoResponse::getId).toList());
        assertEquals(List.of(11L), soloSinPlan.stream().map(AlumnoResponse::getId).toList());
    }

    @Test
    void obtenerAlumno_incluyeSuPlanActivo() {
        Alumno alumno = alumnoConId(10L);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(planAsignadoRepository.findFirstByAlumnoIdAndEstado(10L, EstadoPlanAsignado.ACTIVO))
                .thenReturn(Optional.of(planActivo(5L, alumno, "Fuerza intermedio")));

        assertEquals("Fuerza intermedio", alumnoService.obtenerAlumno(10L).getPlanActivo().getNombre());
    }

    @Test
    void obtenerAlumno_inscripcionActualEsLaActivaSiTiene() {
        Alumno alumno = alumnoExistente();
        Membresia anterior = new Membresia(3L, "Basica", 20000.0, 2, null);
        alumno.agregarInscripcion(new Inscripcion(1L, ZonaHoraria.hoy().minusDays(60), ZonaHoraria.hoy().minusDays(30), EstadoInscripcion.CANCELADA, null, anterior));
        alumno.agregarInscripcion(new Inscripcion(2L, ZonaHoraria.hoy(), null, EstadoInscripcion.ACTIVA, null, membresia));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        AlumnoResponse response = alumnoService.obtenerAlumno(10L);

        assertEquals("Pro", response.getInscripcionActual().getMembresiaNombre());
        assertEquals(EstadoInscripcion.ACTIVA, response.getInscripcionActual().getEstado());
    }

    @Test
    void obtenerAlumno_sinActivaMuestraLaUltimaConSuEstado() {
        Alumno alumno = alumnoExistente();
        Membresia anterior = new Membresia(3L, "Basica", 20000.0, 2, null);
        LocalDate inicio = ZonaHoraria.hoy().minusDays(40);
        alumno.agregarInscripcion(new Inscripcion(1L, ZonaHoraria.hoy().minusDays(100), ZonaHoraria.hoy().minusDays(70), EstadoInscripcion.CANCELADA, null, anterior));
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

    // --- Baja y reactivación (MOGNI-246) ---

    @Test
    void darDeBaja_desactivaLaCuentaCancelaLaInscripcionYLosTurnos() {
        Alumno alumno = alumnoConCuota(membresia, 20, null);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse baja = alumnoService.darDeBajaAlumno(10L);

        assertFalse(baja.isActivo());
        assertFalse(alumno.getCuenta().isActiva());
        assertEquals(hoy, baja.getFechaBaja());
        Inscripcion inscripcion = alumno.getInscripciones().get(0);
        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        assertEquals(hoy, inscripcion.getFechaFin());
        verify(cancelacionPorSistemaService).cancelarReservasFuturas(eq(10L), any());
        verify(planAsignadoService).finalizarPlanActivoDe(10L);
        // Todavía le quedan 20 días pagos: reactivarlo no requiere pago
        assertFalse(baja.getReactivacionRequierePago());
    }

    @Test
    void reactivar_conDiasPagosReactivaLaMismaInscripcionSinCobrar() {
        Alumno alumno = alumnoConCuota(membresia, 10, null);
        Inscripcion inscripcion = alumno.getInscripciones().get(0);
        inscripcion.cancelar(hoy.minusDays(2));
        alumno.getCuenta().setActiva(false);
        alumno.getCuenta().setFechaBaja(hoy.minusDays(2));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse alta = alumnoService.reactivarAlumno(10L, null);

        assertTrue(alta.isActivo());
        assertNull(alta.getFechaBaja());
        assertEquals(1, alumno.getInscripciones().size());
        assertEquals(EstadoInscripcion.ACTIVA, inscripcion.getEstado());
        assertNull(inscripcion.getFechaFin());
        assertEquals(1, inscripcion.getCuotas().size());
        assertEquals(hoy.plusDays(10), inscripcion.getCuotas().get(0).getFechaVencimiento());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reactivar_elDiaDelVencimientoTodaviaTieneDiasPagos() {
        Alumno alumno = alumnoConCuota(membresia, 0, null);
        alumno.getInscripciones().get(0).cancelar(hoy.minusDays(2));
        alumno.getCuenta().setActiva(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        alumnoService.reactivarAlumno(10L, null);

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reactivar_sinDiasPagosCreaInscripcionNuevaConPrimeraCuotaDesdeHoySinRecargo() {
        Alumno alumno = alumnoConCuota(new Membresia(3L, "Basica", 20000.0, 2, null), -12, null);
        Inscripcion vieja = alumno.getInscripciones().get(0);
        vieja.cancelar(hoy.minusDays(3));
        alumno.getCuenta().setActiva(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(membresiaRepository.findById(7L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));
        ReactivarAlumnoRequest request = new ReactivarAlumnoRequest();
        request.setMembresiaId(7L);
        request.setMedioDePago(MedioDePago.TRANSFERENCIA);

        AlumnoResponse alta = alumnoService.reactivarAlumno(10L, request);

        assertTrue(alta.isActivo());
        assertEquals(7L, alta.getMembresiaId());
        assertEquals(EstadoInscripcion.CANCELADA, vieja.getEstado());
        Inscripcion nueva = alumno.getInscripciones().get(1);
        assertEquals(hoy, nueva.getFechaInicio());
        Cuota cuota = nueva.getCuotas().get(0);
        assertEquals(hoy, cuota.getFechaInicio());
        assertEquals(hoy.plusDays(30), cuota.getFechaVencimiento());
        Pago pago = pagoCapturado();
        assertEquals(new BigDecimal("40000.00"), pago.getMonto());
        assertEquals(new BigDecimal("0.00"), pago.getRecargo());
        assertEquals(MedioDePago.TRANSFERENCIA, pago.getMedioDePago());
    }

    @Test
    void reactivar_sinDiasPagosYSinMedioDePagoFalla() {
        Alumno alumno = alumnoConCuota(membresia, -12, null);
        alumno.getInscripciones().get(0).cancelar(hoy.minusDays(3));
        alumno.getCuenta().setActiva(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        ReactivarAlumnoRequest request = new ReactivarAlumnoRequest();
        request.setMembresiaId(7L);

        assertThrows(RegistroInvalidoException.class, () -> alumnoService.reactivarAlumno(10L, request));
        assertFalse(alumno.getCuenta().isActiva());
        verify(alumnoRepository, never()).save(any());
    }

    // --- Con un cambio de membresía programado ---

    private final Membresia full = new Membresia(8L, "Full", 38000.0, 5, null);

    /** Le agrega al alumno un cambio a "Full" ya pagado para el período que empieza en {@code inicio}. */
    private Inscripcion programadaDesde(Alumno alumno, LocalDate inicio) {
        Inscripcion programada = Inscripcion.nueva(alumno, full, inicio, EstadoInscripcion.PROGRAMADA);
        programada.agregarCuota(inicio).setId(101L);
        return programada;
    }

    @Test
    void cambioDeMembresia_conUnCambioProgramadoPagadoSeBloquea() {
        Alumno alumno = alumnoConCuota(membresia, 4, null);
        programadaDesde(alumno, hoy.plusDays(4));
        prepararCambio(alumno, new Membresia(5L, "Otra", 1.0, 1, null));

        CambioMembresiaResponse vista = alumnoService.previsualizarCambioMembresia(10L, 5L);

        assertFalse(vista.isPermitido());
        assertTrue(vista.getMotivo().contains("tiene un cambio a Full desde el"), vista.getMotivo());
        assertThrows(RegistroInvalidoException.class,
                () -> alumnoService.cambiarMembresia(10L, cambioA(5L, MedioDePago.EFECTIVO)));
    }

    @Test
    void vistaPreviaDelCambio_informaLosDiasQueLeQuedanALaCuota() {
        Alumno alumno = alumnoConCuota(membresia, 7, null);
        prepararCambio(alumno, full);

        assertEquals(7L, alumnoService.previsualizarCambioMembresia(10L, 8L).getDiasRestantes());
    }

    @Test
    void obtenerAlumno_conUnCambioProgramadoInformaLaMembresiaYDesdeCuando() {
        Alumno alumno = alumnoConCuota(membresia, 4, null);
        programadaDesde(alumno, hoy.plusDays(4));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        AlumnoResponse response = alumnoService.obtenerAlumno(10L);

        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals("Full", response.getMembresiaProgramada());
        assertEquals(hoy.plusDays(4), response.getProgramadaDesde());
    }

    @Test
    void darDeBaja_conUnCambioProgramadoCancelaLasDosInscripciones() {
        Alumno alumno = alumnoConCuota(membresia, 4, null);
        Inscripcion programada = programadaDesde(alumno, hoy.plusDays(4));
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse baja = alumnoService.darDeBajaAlumno(10L);

        assertEquals(EstadoInscripcion.CANCELADA, alumno.getInscripciones().get(0).getEstado());
        assertEquals(EstadoInscripcion.CANCELADA, programada.getEstado());
        assertNull(baja.getMembresiaProgramada());
        verify(pagoRepository, never()).save(any());
    }

    /** Alumno dado de baja hace 2 días que tenía un cambio programado desde {@code inicio}. */
    private Alumno dadoDeBajaConProgramada(int diasHastaVencimiento) {
        Alumno alumno = alumnoConCuota(membresia, diasHastaVencimiento, null);
        programadaDesde(alumno, hoy.plusDays(diasHastaVencimiento));
        alumno.getInscripciones().forEach(i -> i.cancelar(hoy.minusDays(2)));
        alumno.getCuenta().setActiva(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));
        return alumno;
    }

    @Test
    void reactivar_antesDeQueEmpieceLaProgramadaVuelveLaViejaYLaNuevaSigueProgramada() {
        Alumno alumno = dadoDeBajaConProgramada(4);

        AlumnoResponse alta = alumnoService.reactivarAlumno(10L, null);

        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(0).getEstado());
        assertEquals(EstadoInscripcion.PROGRAMADA, alumno.getInscripciones().get(1).getEstado());
        assertNull(alumno.getInscripciones().get(1).getFechaFin());
        assertEquals("Pro", alta.getMembresiaNombre());
        assertEquals("Full", alta.getMembresiaProgramada());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reactivar_duranteElPeriodoDeLaProgramadaQuedaActivaLaNueva() {
        // La cuota vieja venció hace 3 días y la programada empezó ese día (lo dieron de baja antes de activarla)
        Alumno alumno = dadoDeBajaConProgramada(-3);

        AlumnoResponse alta = alumnoService.reactivarAlumno(10L, null);

        assertEquals(EstadoInscripcion.CANCELADA, alumno.getInscripciones().get(0).getEstado());
        assertEquals(EstadoInscripcion.ACTIVA, alumno.getInscripciones().get(1).getEstado());
        assertEquals("Full", alta.getMembresiaNombre());
        assertNull(alta.getMembresiaProgramada());
    }

    @Test
    void reactivar_conElMesSiguientePagadoConLaMismaMembresiaReactivaEsaInscripcion() {
        // Renovó por adelantado con la misma membresía: las dos cuotas están en la misma inscripción
        Alumno alumno = alumnoConCuota(membresia, 4, null);
        Inscripcion inscripcion = alumno.getInscripciones().get(0);
        inscripcion.agregarCuota(hoy.plusDays(4));
        inscripcion.cancelar(hoy.minusDays(2));
        alumno.getCuenta().setActiva(false);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));

        alumnoService.reactivarAlumno(10L, null);

        assertEquals(EstadoInscripcion.ACTIVA, inscripcion.getEstado());
        assertTrue(alumno.buscarInscripcionProgramada().isEmpty());
    }

    @Test
    void reactivar_unAlumnoActivoNoHaceNada() {
        Alumno alumno = alumnoConCuota(membresia, 20, null);
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumno));

        assertTrue(alumnoService.reactivarAlumno(10L, null).isActivo());
        verify(alumnoRepository, never()).save(any());
    }
}
