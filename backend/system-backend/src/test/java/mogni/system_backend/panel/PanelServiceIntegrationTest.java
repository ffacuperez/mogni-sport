package mogni.system_backend.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import mogni.system_backend.cuota.CuotasDePrueba;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.alumno.service.AlumnoService;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.panel.dto.PanelResponse;
import mogni.system_backend.panel.service.PanelService;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * Panel del propietario: el servicio contra un PostgreSQL embebido (nunca la base compartida), con datos conocidos.
 * Cada prueba empieza con la base vacía y usa un "ahora" fijo, salvo la de mora, que se compara con la lista de
 * Alumnos y por eso usa la fecha real. La propiedad de abajo le da a esta clase su propio contexto (y su propia base),
 * así vaciar las tablas no afecta a otras pruebas.
 */
@SpringBootTest(properties = "mogni.test.contexto=panel")
@ActiveProfiles("test")
class PanelServiceIntegrationTest {

    /** Lunes 15/06/2026 al mediodía. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 6, 15, 12, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final AtomicInteger SECUENCIA = new AtomicInteger(5000);

    @Autowired
    private PanelService panelService;

    @Autowired
    private AlumnoService alumnoService;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private MembresiaRepository membresiaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciarBase() {
        jdbc.execute("TRUNCATE TABLE alumnos, cuentas, membresias, turnos RESTART IDENTITY CASCADE");
    }

    // --- Datos de prueba ---

    private Membresia membresia(String nombre, double precio) {
        return membresiaRepository.save(new Membresia(null, nombre + " " + SECUENCIA.incrementAndGet(), precio, 3, null));
    }

    private Alumno alumnoSinInscripcion(boolean activo, LocalDate fechaBaja) {
        int n = SECUENCIA.incrementAndGet();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail("panel" + n + "@test.com");
        cuenta.setPassword("x");
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setActiva(activo);
        cuenta.setFechaBaja(fechaBaja);
        Alumno alumno = new Alumno();
        alumno.setNombre("Alumno" + n);
        alumno.setApellido("Panel");
        alumno.setEdad(30);
        alumno.setDni(String.valueOf(42_000_000 + n));
        alumno.setEmail(cuenta.getEmail());
        alumno.setCuenta(cuenta);
        return alumno;
    }

    /** Alumno con una inscripción en ese estado desde {@code inicio}, sin cuotas. */
    private Alumno alumno(boolean activo, Membresia membresia, LocalDate inicio, EstadoInscripcion estado) {
        Alumno alumno = alumnoSinInscripcion(activo, null);
        Inscripcion.nueva(alumno, membresia, inicio, estado);
        return alumnoRepository.save(alumno);
    }

    /** Alumno activo con inscripción activa y una cuota que vence en {@code diasHastaVencimiento} desde {@code hoy}. */
    private Alumno alumnoConCuota(boolean activo, Membresia membresia, LocalDate hoy, int diasHastaVencimiento) {
        Alumno alumno = alumnoSinInscripcion(activo, null);
        Inscripcion inscripcion = Inscripcion.nueva(alumno, membresia, hoy.minusDays(90), EstadoInscripcion.ACTIVA);
        CuotasDePrueba.queVence(inscripcion, hoy.plusDays(diasHastaVencimiento));
        return alumnoRepository.save(alumno);
    }

    /**
     * Un pago de {@code precio} + {@code recargo} en esa fecha. Cada pago necesita su cuota: la cuelga de un alumno
     * inactivo, sin fecha de baja y con inscripción de 2020, así no cambia ningún otro indicador.
     */
    private void pago(LocalDate fecha, double precio, String recargo, MedioDePago medio) {
        Alumno alumno = alumnoSinInscripcion(false, null);
        Inscripcion inscripcion = Inscripcion.nueva(alumno, membresia("Pago", precio), LocalDate.of(2020, 1, 1),
                EstadoInscripcion.CANCELADA);
        Cuota cuota = inscripcion.agregarCuota(fecha);
        alumnoRepository.save(alumno);
        pagoRepository.save(Pago.de(cuota, new BigDecimal(recargo), medio, fecha));
    }

    /** Pago de la diferencia por un cambio de membresía (HU-274): una cuota con precio = diferencia. */
    private void pagoDiferenciaPorCambio(LocalDate fecha, String diferencia, MedioDePago medio) {
        Alumno alumno = alumnoSinInscripcion(false, null);
        Inscripcion inscripcion = Inscripcion.nueva(alumno, membresia("Cambio", 30000), LocalDate.of(2020, 1, 1),
                EstadoInscripcion.CANCELADA);
        Cuota cuota = inscripcion.agregarCuotaPorCambio(fecha, fecha.plusDays(10), new BigDecimal(diferencia));
        alumnoRepository.save(alumno);
        pagoRepository.save(Pago.de(cuota, BigDecimal.ZERO, medio, fecha));
    }

    private Turno turno(LocalDate fecha, String hora, int cupo, EstadoTurno estado) {
        LocalTime inicio = LocalTime.parse(hora);
        return turnoRepository.save(new Turno(null, fecha, inicio, inicio.plusHours(1), cupo, estado));
    }

    private void reserva(Turno turno, EstadoReserva estado, Asistencia asistencia) {
        Alumno alumno = alumnoRepository.save(alumnoSinInscripcion(true, null));
        Reserva reserva = new Reserva(null, alumno, turno, turno.getFecha().atStartOfDay(), estado);
        reserva.setAsistencia(asistencia);
        reservaRepository.save(reserva);
    }

    private static BigDecimal pesos(String monto) {
        return new BigDecimal(monto).setScale(2);
    }

    // --- Ingresos ---

    @Test
    void ingresosDelMesIncluyenRecargoYDiferenciaPorCambioConDesgloseYVariacion() {
        pago(HOY.withDayOfMonth(1), 20000, "0", MedioDePago.EFECTIVO);
        pago(HOY, 20000, "2000", MedioDePago.TRANSFERENCIA);           // renovación en mora
        pagoDiferenciaPorCambio(HOY.minusDays(3), "5000", MedioDePago.EFECTIVO);
        pago(HOY.minusMonths(1), 40000, "0", MedioDePago.EFECTIVO);       // mayo
        pago(HOY.plusMonths(1), 99999, "0", MedioDePago.EFECTIVO);        // julio: no cuenta

        PanelResponse.Ingresos ingresos = panelService.consultar(AHORA).ingresos();

        assertEquals(pesos("47000"), ingresos.mes());
        assertEquals(pesos("40000"), ingresos.mesAnterior());
        assertEquals(new BigDecimal("17.5"), ingresos.variacionPorcentaje());
        assertEquals(pesos("25000"), ingresos.efectivo());
        assertEquals(pesos("22000"), ingresos.transferencia());
        assertEquals(pesos("2000"), ingresos.recargos());
        // Efectivo + transferencia = total del mes
        assertEquals(ingresos.mes(), ingresos.efectivo().add(ingresos.transferencia()));
    }

    @Test
    void sinPagosElMesAnteriorLaVariacionEsNula() {
        pago(HOY, 15000, "0", MedioDePago.EFECTIVO);

        PanelResponse.Ingresos ingresos = panelService.consultar(AHORA).ingresos();

        assertEquals(pesos("15000"), ingresos.mes());
        assertEquals(pesos("0"), ingresos.mesAnterior());
        assertNull(ingresos.variacionPorcentaje());
    }

    @Test
    void variacionNegativaContraElMesAnterior() {
        pago(HOY, 30000, "0", MedioDePago.EFECTIVO);
        pago(HOY.minusMonths(1), 40000, "0", MedioDePago.TRANSFERENCIA);

        assertEquals(new BigDecimal("-25.0"), panelService.consultar(AHORA).ingresos().variacionPorcentaje());
    }

    @Test
    void elGraficoTieneLosUltimosMesesConLosMesesSinPagosEnCero() {
        pago(LocalDate.of(2026, 3, 10), 10000, "0", MedioDePago.EFECTIVO);
        pago(LocalDate.of(2026, 5, 31), 40000, "0", MedioDePago.EFECTIVO);
        pago(LocalDate.of(2026, 6, 1), 20000, "0", MedioDePago.EFECTIVO);
        pago(LocalDate.of(2025, 12, 31), 50000, "0", MedioDePago.EFECTIVO); // fuera de los 6 meses

        List<PanelResponse.IngresoMes> meses = panelService.consultar(AHORA).ingresosPorMes();

        assertEquals(PanelService.MESES_GRAFICO, meses.size());
        assertEquals(List.of(
                new PanelResponse.IngresoMes(2026, 1, pesos("0")),
                new PanelResponse.IngresoMes(2026, 2, pesos("0")),
                new PanelResponse.IngresoMes(2026, 3, pesos("10000")),
                new PanelResponse.IngresoMes(2026, 4, pesos("0")),
                new PanelResponse.IngresoMes(2026, 5, pesos("40000")),
                new PanelResponse.IngresoMes(2026, 6, pesos("20000"))), meses);
    }

    // --- Alumnos ---

    @Test
    void enMoraCoincideConLaListaDeAlumnos() {
        LocalDate hoy = ZonaHoraria.hoy();
        Membresia m = membresia("x3", 20000);
        alumnoConCuota(true, m, hoy, 20);   // al día
        alumnoConCuota(true, m, hoy, 3);    // próxima a vencer
        alumnoConCuota(true, m, hoy, -1);   // vencida
        alumnoConCuota(true, m, hoy, -2);   // en mora
        alumnoConCuota(true, m, hoy, -15);  // en mora
        alumnoConCuota(false, m, hoy, -15); // dado de baja: no cuenta

        long enMoraEnLaLista = alumnoService.listarAlumnos(true, null).stream()
                .filter(a -> a.getCuotaActual().getSituacion() == SituacionPago.EN_MORA)
                .count();

        assertEquals(2, enMoraEnLaLista);
        assertEquals(enMoraEnLaLista, panelService.consultar(ZonaHoraria.ahora()).alumnos().enMora());
    }

    @Test
    void activosPorMembresiaSumanElTotalDeActivos() {
        Membresia x3 = membresia("x3", 20000);
        Membresia x5 = membresia("x5", 30000);
        alumno(true, x3, HOY.minusMonths(3), EstadoInscripcion.ACTIVA);
        alumno(true, x3, HOY.minusMonths(2), EstadoInscripcion.ACTIVA);
        alumno(true, x3, HOY.minusMonths(1), EstadoInscripcion.ACTIVA);
        alumno(true, x5, HOY.minusMonths(1), EstadoInscripcion.ACTIVA);
        alumno(false, x3, HOY.minusMonths(1), EstadoInscripcion.CANCELADA); // dado de baja
        alumno(true, x5, HOY.minusMonths(1), EstadoInscripcion.CANCELADA);  // activo sin inscripción activa

        PanelResponse panel = panelService.consultar(AHORA);

        assertEquals(5, panel.alumnos().activos());
        assertEquals(List.of(
                new PanelResponse.ActivosMembresia(x3.getNombre(), 3),
                new PanelResponse.ActivosMembresia(x5.getNombre(), 1),
                new PanelResponse.ActivosMembresia(null, 1)), panel.activosPorMembresia());
        assertEquals(panel.alumnos().activos(),
                panel.activosPorMembresia().stream().mapToLong(PanelResponse.ActivosMembresia::cantidad).sum());
    }

    @Test
    void nuevosSonLosQueEmpiezanSuPrimeraInscripcionEnElMesYBajasPorFechaDeBaja() {
        Membresia m = membresia("x4", 25000);
        alumno(true, m, HOY.withDayOfMonth(3), EstadoInscripcion.ACTIVA);           // nuevo
        // Cambio de membresía este mes: su primera inscripción es de enero, no es nuevo
        Alumno conCambio = alumnoSinInscripcion(true, null);
        Inscripcion.nueva(conCambio, m, LocalDate.of(2026, 1, 10), EstadoInscripcion.CANCELADA);
        Inscripcion.nueva(conCambio, m, HOY.withDayOfMonth(10), EstadoInscripcion.ACTIVA);
        alumnoRepository.save(conCambio);
        // Bajas: una este mes, una el mes pasado
        Alumno bajaDelMes = alumnoSinInscripcion(false, HOY.withDayOfMonth(5));
        Inscripcion.nueva(bajaDelMes, m, LocalDate.of(2026, 2, 1), EstadoInscripcion.CANCELADA);
        alumnoRepository.save(bajaDelMes);
        Alumno bajaAnterior = alumnoSinInscripcion(false, HOY.minusMonths(1).withDayOfMonth(30));
        Inscripcion.nueva(bajaAnterior, m, LocalDate.of(2026, 2, 1), EstadoInscripcion.CANCELADA);
        alumnoRepository.save(bajaAnterior);

        PanelResponse.Alumnos alumnos = panelService.consultar(AHORA).alumnos();

        assertEquals(1, alumnos.nuevosDelMes());
        assertEquals(1, alumnos.bajasDelMes());
        assertEquals(2, alumnos.activos());
    }

    // --- Turnos ---

    @Test
    void ocupacionYAsistenciaDeLosTurnosDelMes() {
        // 02/06 08:00, cupo 10: 3 reservas activas (2 presentes, 1 ausente) y 1 cancelada por el alumno
        Turno t1 = turno(LocalDate.of(2026, 6, 2), "08:00", 10, EstadoTurno.DISPONIBLE);
        reserva(t1, EstadoReserva.RESERVADA, Asistencia.PRESENTE);
        reserva(t1, EstadoReserva.RESERVADA, Asistencia.PRESENTE);
        reserva(t1, EstadoReserva.RESERVADA, Asistencia.AUSENTE);
        reserva(t1, EstadoReserva.CANCELADA, null);
        // 10/06 18:00, cupo 10: 1 reserva sin marcar
        reserva(turno(LocalDate.of(2026, 6, 10), "18:00", 10, EstadoTurno.DISPONIBLE), EstadoReserva.RESERVADA, null);
        // Cancelado por el gimnasio: no cuenta ni su cupo ni sus reservas
        Turno cancelado = turno(LocalDate.of(2026, 6, 5), "09:00", 10, EstadoTurno.CANCELADO);
        reserva(cancelado, EstadoReserva.CANCELADA_POR_GIMNASIO, null);
        // Hoy 08:00 (ya empezó), cupo 5: 1 presente
        reserva(turno(HOY, "08:00", 5, EstadoTurno.DISPONIBLE), EstadoReserva.RESERVADA, Asistencia.PRESENTE);
        // Hoy 18:00 (todavía no empezó) y el 20/06: fuera de la ocupación
        reserva(turno(HOY, "18:00", 5, EstadoTurno.DISPONIBLE), EstadoReserva.RESERVADA, null);
        reserva(turno(LocalDate.of(2026, 6, 20), "08:00", 5, EstadoTurno.DISPONIBLE), EstadoReserva.RESERVADA, null);
        // Mes anterior: no cuenta
        reserva(turno(LocalDate.of(2026, 5, 31), "08:00", 10, EstadoTurno.DISPONIBLE), EstadoReserva.RESERVADA,
                Asistencia.PRESENTE);

        PanelResponse panel = panelService.consultar(AHORA);

        assertEquals(new PanelResponse.Ocupacion(5, 25, new BigDecimal("20.0")), panel.ocupacion());
        assertEquals(new PanelResponse.Asistencia(3, 1, new BigDecimal("75.0")), panel.asistencia());
    }

    // --- Sin datos ---

    @Test
    void sinDatosTodoEnCeroYLosPorcentajesNulos() {
        PanelResponse panel = panelService.consultar(AHORA);

        assertEquals(HOY, panel.fecha());
        assertEquals(new PanelResponse.Ingresos(pesos("0"), pesos("0"), null, pesos("0"), pesos("0"), pesos("0")),
                panel.ingresos());
        assertEquals(PanelService.MESES_GRAFICO, panel.ingresosPorMes().size());
        assertTrue(panel.ingresosPorMes().stream().allMatch(m -> m.total().signum() == 0));
        assertEquals(new PanelResponse.Alumnos(0, 0, 0, 0), panel.alumnos());
        assertTrue(panel.activosPorMembresia().isEmpty());
        assertEquals(new PanelResponse.Ocupacion(0, 0, null), panel.ocupacion());
        assertEquals(new PanelResponse.Asistencia(0, 0, null), panel.asistencia());
    }
}
