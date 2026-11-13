package mogni.system_backend.inicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import mogni.system_backend.cuota.CuotasDePrueba;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.plan.entity.MetodoPlan;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.service.ReglasReserva;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * MOGNI-281: pruebas de punta a punta del inicio del alumno (HTTP, seguridad, JPA y base), una por prueba de
 * aceptación. Corren contra un PostgreSQL embebido que se crea y se descarta en cada ejecución: nunca tocan una base
 * compartida. Las fechas son relativas a hoy; los turnos de prueba se crean mañana para no depender de la hora.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MiInicioIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar el PostgreSQL embebido", e);
        }
    }

    @DynamicPropertySource
    static void baseEmbebida(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("jwt.secret", () -> "clave-solo-para-las-pruebas-automatizadas-de-mogni");
        registry.add("jwt.expiration-ms", () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private MembresiaRepository membresiaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ClaseExtraRepository claseExtraRepository;

    @Autowired
    private PlanAsignadoRepository planRepository;

    private final ObjectMapper json = JsonMapper.builder().build();

    private record Respuesta(int status, JsonNode cuerpo) {
    }

    // --- Datos de prueba ---

    private static LocalDate hoy() {
        return ZonaHoraria.hoy();
    }

    private Alumno alumnoSinInscripcion() {
        int n = SECUENCIA.incrementAndGet();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail("inicio" + n + "@test.com");
        cuenta.setPassword("x");
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setActiva(true);
        Alumno alumno = new Alumno();
        alumno.setNombre("Martín" + n);
        alumno.setApellido("Prueba");
        alumno.setEdad(30);
        alumno.setDni(String.valueOf(41_000_000 + n));
        alumno.setEmail(cuenta.getEmail());
        alumno.setCuenta(cuenta);
        return alumno;
    }

    private Membresia membresia(int diasPorSemana) {
        return membresiaRepository.save(new Membresia(null, "HIIT " + SECUENCIA.incrementAndGet(), 25000.0, diasPorSemana, null));
    }

    /** Alumno con inscripción activa y una cuota pagada que vence en {@code diasHastaVencimiento} (negativo: ya venció). */
    private Alumno alumno(int diasPorSemana, int diasHastaVencimiento) {
        Alumno alumno = alumnoSinInscripcion();
        Inscripcion inscripcion = Inscripcion.nueva(alumno, membresia(diasPorSemana), hoy().minusDays(60), EstadoInscripcion.ACTIVA);
        CuotasDePrueba.queVence(inscripcion, hoy().plusDays(diasHastaVencimiento));
        return alumnoRepository.save(alumno);
    }

    /** Al día: vence en 20 días. */
    private Alumno alumnoAlDia(int diasPorSemana) {
        return alumno(diasPorSemana, 20);
    }

    /** Cada turno con una hora distinta: la base no admite dos turnos el mismo día a la misma hora. */
    private Turno turno(LocalDate fecha) {
        LocalTime inicio = LocalTime.of(6, 0).plusMinutes(SECUENCIA.incrementAndGet() % 900);
        return turnoRepository.save(new Turno(null, fecha, inicio, inicio.plusHours(1), 10, EstadoTurno.DISPONIBLE));
    }

    private Reserva reservar(Alumno alumno, LocalDate fecha) {
        return reservaRepository.save(new Reserva(null, alumno, turno(fecha), LocalDateTime.now(), EstadoReserva.RESERVADA));
    }

    /**
     * Usa todos los turnos de la semana de hoy y, si mañana ya es otra semana (hoy domingo), también los de esa: así la
     * prueba vale cualquier día. Las reservas van en días distintos de hoy y mañana, como mandan las reglas.
     */
    private void agotarCupo(Alumno alumno, int diasPorSemana) {
        for (LocalDate dia : List.of(hoy(), hoy().plusDays(1))) {
            LocalDate lunes = ReglasReserva.lunesDe(dia);
            long yaUsados = ReglasReserva.reservasDeLaSemana(reservaRepository, alumno.getId(), dia).size();
            for (int i = 0; yaUsados < diasPorSemana && i < 7; i++) {
                LocalDate fecha = lunes.plusDays(i);
                if (!fecha.equals(hoy()) && !fecha.equals(hoy().plusDays(1))) {
                    reservar(alumno, fecha);
                    yaUsados++;
                }
            }
        }
    }

    private PlanAsignado planActivo(Alumno alumno) {
        PlanAsignado plan = new PlanAsignado(alumno, hoy().minusDays(8));
        plan.setNombre("Hipertrofia - Progresivo");
        plan.setNivel(NivelPlantilla.INTERMEDIO);
        plan.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        plan.setMetodo(MetodoPlan.ULTRA_MASS);
        plan.setMesociclo("Hipertrofia");
        plan.setCantidadSemanas(5);
        return planRepository.save(plan);
    }

    // --- Pedidos ---

    private Respuesta inicioDe(Alumno alumno) throws Exception {
        return ejecutar(get("/mi-inicio").with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    private Respuesta ejecutar(MockHttpServletRequestBuilder pedido) throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(pedido).andReturn().getResponse();
        String texto = respuesta.getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(respuesta.getStatus(), texto.isEmpty() ? json.createObjectNode() : json.readTree(texto));
    }

    // --- Pruebas de aceptación ---

    @Test
    void al_dia_con_turno_agendado_y_plan_activo_muestra_todo_sin_aviso_de_cuota() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        Turno turno = reservar(alumno, hoy().plusDays(1)).getTurno();
        PlanAsignado plan = planActivo(alumno);

        Respuesta respuesta = inicioDe(alumno);

        assertEquals(200, respuesta.status());
        JsonNode cuerpo = respuesta.cuerpo();
        assertEquals(hoy().toString(), cuerpo.path("hoy").asString());
        assertEquals(alumno.getNombre(), cuerpo.path("nombre").asString());
        assertTrue(cuerpo.path("membresia").asString().startsWith("HIIT"));
        assertEquals(3, cuerpo.path("diasPorSemana").asInt());
        assertTrue(cuerpo.path("activo").asBoolean());
        assertEquals("AL_DIA", cuerpo.path("cuota").path("situacion").asString());
        assertEquals(20, cuerpo.path("cuota").path("diasParaVencer").asInt());
        assertEquals(turno.getFecha().toString(), cuerpo.path("proximoTurno").path("fecha").asString());
        assertEquals(turno.getHoraInicio().toString(), cuerpo.path("proximoTurno").path("horaInicio").asString());
        assertEquals(turno.getHoraFin().toString(), cuerpo.path("proximoTurno").path("horaFin").asString());
        assertEquals(3, cuerpo.path("turnosSemana").path("total").asInt());
        assertTrue(cuerpo.path("puedeReservar").asBoolean());
        assertTrue(cuerpo.path("motivoNoReserva").isNull());
        assertEquals(plan.getId(), cuerpo.path("planActivo").path("id").asLong());
        assertEquals("Hipertrofia - Progresivo", cuerpo.path("planActivo").path("nombre").asString());
        assertEquals(2, cuerpo.path("planActivo").path("semanaActual").asInt());
        assertEquals(5, cuerpo.path("planActivo").path("cantidadSemanas").asInt());
    }

    @Test
    void con_la_cuota_a_tres_dias_de_vencer_esta_proxima_a_vencer() throws Exception {
        JsonNode cuota = inicioDe(alumno(3, 3)).cuerpo().path("cuota");

        assertEquals("PROXIMA_A_VENCER", cuota.path("situacion").asString());
        assertEquals(3, cuota.path("diasParaVencer").asInt());
        assertEquals(hoy().plusDays(3).toString(), cuota.path("fechaVencimiento").asString());
    }

    @Test
    void en_mora_avisa_y_no_puede_reservar() throws Exception {
        JsonNode cuerpo = inicioDe(alumno(3, -3)).cuerpo();

        assertEquals("EN_MORA", cuerpo.path("cuota").path("situacion").asString());
        assertFalse(cuerpo.path("puedeReservar").asBoolean());
        assertEquals("EN_MORA", cuerpo.path("motivoNoReserva").asString());
    }

    @Test
    void vencida_ayer_avisa_pero_todavia_puede_reservar() throws Exception {
        JsonNode cuerpo = inicioDe(alumno(3, -1)).cuerpo();

        assertEquals("VENCIDA", cuerpo.path("cuota").path("situacion").asString());
        assertTrue(cuerpo.path("puedeReservar").asBoolean());
    }

    @Test
    void sin_turnos_agendados_el_proximo_turno_es_null() throws Exception {
        assertTrue(inicioDe(alumnoAlDia(3)).cuerpo().path("proximoTurno").isNull());
    }

    @Test
    void con_los_tres_turnos_de_la_semana_usados_no_puede_reservar_por_cupo() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        agotarCupo(alumno, 3);

        JsonNode cuerpo = inicioDe(alumno).cuerpo();

        assertEquals(3, cuerpo.path("turnosSemana").path("usados").asInt());
        assertEquals(3, cuerpo.path("turnosSemana").path("total").asInt());
        assertFalse(cuerpo.path("puedeReservar").asBoolean());
        assertEquals("CUPO_SEMANAL_AGOTADO", cuerpo.path("motivoNoReserva").asString());
    }

    @Test
    void sin_plan_activo_el_plan_es_null() throws Exception {
        assertTrue(inicioDe(alumnoAlDia(3)).cuerpo().path("planActivo").isNull());
    }

    // --- Clases de la cuota ---

    private void asistio(Alumno alumno, LocalDate fecha, Asistencia asistencia) {
        Reserva reserva = reservar(alumno, fecha);
        reserva.setAsistencia(asistencia);
        reservaRepository.save(reserva);
    }

    @Test
    void cuenta_las_clases_presentes_del_periodo_de_la_cuota() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        Cuota cuota = alumno.buscarUltimaCuotaPagada().orElseThrow();
        asistio(alumno, cuota.getFechaInicio(), Asistencia.PRESENTE);
        asistio(alumno, hoy().minusDays(2), Asistencia.PRESENTE);
        // No suman: una ausencia, una sin marcar, una cancelada y una de antes del período
        asistio(alumno, hoy().minusDays(3), Asistencia.AUSENTE);
        reservar(alumno, hoy().plusDays(1));
        Reserva cancelada = reservar(alumno, hoy().minusDays(4));
        cancelada.setEstado(EstadoReserva.CANCELADA);
        cancelada.setAsistencia(Asistencia.PRESENTE);
        reservaRepository.save(cancelada);
        asistio(alumno, cuota.getFechaInicio().minusDays(1), Asistencia.PRESENTE);

        JsonNode clases = inicioDe(alumno).cuerpo().path("clasesCuota");

        assertEquals(2, clases.path("presentes").asInt());
        // La última con «Presente», aunque sea de antes del período
        assertEquals(hoy().minusDays(2).toString(), clases.path("ultima").path("fecha").asString());
    }

    @Test
    void el_total_es_de_cuantas_clases_podia_ir_en_el_periodo() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        Cuota cuota = alumno.buscarUltimaCuotaPagada().orElseThrow();
        // Sin franjas cargadas se cuentan todos los días: una por día y hasta 3 por semana
        Map<LocalDate, Long> diasPorSemana = cuota.getFechaInicio().datesUntil(cuota.getFechaVencimiento())
                .collect(Collectors.groupingBy(ReglasReserva::lunesDe, Collectors.counting()));
        long esperado = diasPorSemana.values().stream().mapToLong(dias -> Math.min(dias, 3)).sum();

        JsonNode clases = inicioDe(alumno).cuerpo().path("clasesCuota");

        assertEquals(esperado, clases.path("total").asLong());
        assertEquals(0, clases.path("presentes").asInt());
        assertTrue(clases.path("ultima").isNull());
    }

    @Test
    void con_la_cuota_siguiente_ya_paga_cuenta_desde_que_empieza_la_que_cubre_hoy() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        Inscripcion inscripcion = alumno.buscarInscripcionActiva().orElseThrow();
        Cuota actual = inscripcion.buscarUltimaCuota().orElseThrow();
        inscripcion.agregarCuota(actual.getFechaVencimiento());
        alumnoRepository.save(alumno);
        asistio(alumno, hoy().minusDays(1), Asistencia.PRESENTE);

        assertEquals(1, inicioDe(alumno).cuerpo().path("clasesCuota").path("presentes").asInt());

        // En el perfil, la vigencia va del inicio del período de hoy al último vencimiento pago
        JsonNode cuota = ejecutar(get("/alumnos/me").with(user(alumno.getEmail()).roles("ALUMNO")))
                .cuerpo().path("cuotaActual");
        assertEquals(actual.getFechaInicio().toString(), cuota.path("vigenteDesde").asString());
        assertEquals(ReglasCuota.vencimientoDesde(actual.getFechaVencimiento()).toString(),
                cuota.path("fechaVencimiento").asString());
    }

    @Test
    void sin_una_cuota_que_cubra_hoy_no_hay_contador() throws Exception {
        assertTrue(inicioDe(alumno(3, -3)).cuerpo().path("clasesCuota").isNull());
    }

    // --- Mismas reglas que la reserva ---

    @Test
    void con_el_cupo_agotado_y_una_clase_extra_puede_reservar() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        agotarCupo(alumno, 3);
        // La clase extra nace de un turno que canceló el gimnasio
        Reserva origen = reservaRepository.save(new Reserva(null, alumno, turno(hoy().minusDays(7)), LocalDateTime.now(),
                EstadoReserva.CANCELADA_POR_GIMNASIO));
        claseExtraRepository.save(new ClaseExtra(alumno, origen.getId(), hoy().minusDays(1), hoy().plusDays(10)));

        JsonNode cuerpo = inicioDe(alumno).cuerpo();

        assertTrue(cuerpo.path("puedeReservar").asBoolean());
        assertTrue(cuerpo.path("motivoNoReserva").isNull());
    }

    @Test
    void sin_inscripcion_activa_no_puede_reservar_por_falta_de_membresia() throws Exception {
        JsonNode cuerpo = inicioDe(alumnoRepository.save(alumnoSinInscripcion())).cuerpo();

        assertTrue(cuerpo.path("membresia").isNull());
        assertEquals(0, cuerpo.path("diasPorSemana").asInt());
        assertFalse(cuerpo.path("puedeReservar").asBoolean());
        assertEquals("SIN_MEMBRESIA_ACTIVA", cuerpo.path("motivoNoReserva").asString());
    }

    @Test
    void con_turno_hoy_y_maniana_no_le_quedan_dias_para_reservar() throws Exception {
        Alumno alumno = alumnoAlDia(5);
        reservar(alumno, hoy());
        reservar(alumno, hoy().plusDays(1));

        JsonNode cuerpo = inicioDe(alumno).cuerpo();

        assertFalse(cuerpo.path("puedeReservar").asBoolean());
        assertEquals("SIN_DIAS_DISPONIBLES", cuerpo.path("motivoNoReserva").asString());
    }

    @Test
    void sin_cuotas_registradas_no_se_lo_trata_como_al_dia() throws Exception {
        Alumno alumno = alumnoSinInscripcion();
        Inscripcion.nueva(alumno, membresia(3), hoy().minusDays(60), EstadoInscripcion.ACTIVA);
        alumnoRepository.save(alumno);

        JsonNode cuerpo = inicioDe(alumno).cuerpo();

        assertTrue(cuerpo.path("cuota").path("situacion").isNull());
        assertTrue(cuerpo.path("cuota").path("diasParaVencer").isNull());
        // Como en la reserva: sin cuotas no se bloquea
        assertTrue(cuerpo.path("puedeReservar").asBoolean());
    }

    // --- Seguridad ---

    @Test
    void el_personal_no_puede_consultarlo() throws Exception {
        for (String rol : List.of("PROPIETARIO", "ENTRENADOR", "NUTRICIONISTA")) {
            Respuesta respuesta = ejecutar(get("/mi-inicio").with(user(rol.toLowerCase() + "@test.com").roles(rol)));
            assertEquals(403, respuesta.status(), rol);
        }
    }

    @Test
    void sin_token_responde_401() throws Exception {
        assertEquals(401, ejecutar(get("/mi-inicio")).status());
    }

    @Test
    void una_cuenta_inactiva_recibe_403_con_el_motivo() throws Exception {
        Alumno alumno = alumnoAlDia(3);
        alumno.getCuenta().setActiva(false);
        alumnoRepository.save(alumno);

        Respuesta respuesta = inicioDe(alumno);

        assertEquals(403, respuesta.status());
        assertEquals("Tu cuenta está inactiva.", respuesta.cuerpo().path("error").asString());
    }
}
