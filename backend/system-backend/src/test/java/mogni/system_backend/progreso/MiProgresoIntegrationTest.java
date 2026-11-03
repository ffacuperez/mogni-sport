package mogni.system_backend.progreso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.plan.entity.EjercicioSesionPlan;
import mogni.system_backend.plan.entity.MetodoPlan;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.progreso.entity.RegistroEjercicio;
import mogni.system_backend.progreso.entity.RegistroSesion;
import mogni.system_backend.progreso.repository.RegistroSesionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * HU-270: pruebas de punta a punta (HTTP, seguridad, JPA y base). Corren contra un PostgreSQL embebido que se crea y
 * se descarta en cada ejecución: el perfil "local" no se carga, así que nunca tocan una base compartida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MiProgresoIntegrationTest {

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
    private EjercicioRepository ejercicioRepository;

    @Autowired
    private PlanAsignadoRepository planRepository;

    @Autowired
    private RegistroSesionRepository registroRepository;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper json = JsonMapper.builder().build();

    private record Respuesta(int status, JsonNode cuerpo) {
    }

    // --- Datos de prueba ---

    private Alumno alumno(boolean cuentaActiva) {
        int n = SECUENCIA.incrementAndGet();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail("alumno" + n + "@test.com");
        cuenta.setPassword("x");
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setActiva(cuentaActiva);
        Alumno alumno = new Alumno();
        alumno.setNombre("Alumno" + n);
        alumno.setApellido("Prueba");
        alumno.setEdad(30);
        alumno.setDni(String.valueOf(40_000_000 + n));
        alumno.setEmail(cuenta.getEmail());
        alumno.setCuenta(cuenta);
        return alumnoRepository.save(alumno);
    }

    private Alumno alumno() {
        return alumno(true);
    }

    private EjercicioSesionPlan ejercicio(String nombre, int series, int repeticiones, Integer repeticionesMax,
            String descanso, Integer rir, String indicaciones) {
        Ejercicio catalogo = ejercicioRepository.save(
                new Ejercicio(null, nombre + " " + SECUENCIA.incrementAndGet(), "Piernas", null, true));
        EjercicioSesionPlan ejercicio = new EjercicioSesionPlan(catalogo, series, repeticiones, repeticionesMax,
                descanso == null ? null : new BigDecimal(descanso), null, rir);
        ejercicio.setIndicaciones(indicaciones);
        return ejercicio;
    }

    private SesionPlan sesion(EjercicioSesionPlan... ejercicios) {
        SesionPlan sesion = new SesionPlan((String) null);
        sesion.agregarEjercicios(List.of(ejercicios));
        return sesion;
    }

    /** Plan de dos sesiones, sin los datos de HU-270 cargados. */
    private PlanAsignado planBasico(Alumno alumno, LocalDate inicio) {
        PlanAsignado plan = new PlanAsignado(alumno, inicio);
        plan.setNombre("Plan de prueba");
        plan.setNivel(NivelPlantilla.INTERMEDIO);
        plan.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        plan.reemplazarSesiones(List.of(
                sesion(ejercicio("Sentadilla", 4, 15, 18, "1.25", 3, "Tempo 2-1-3"), ejercicio("Prensa", 4, 12, null, null, null, null)),
                sesion(ejercicio("Remo", 3, 10, 12, "1.5", 2, null))));
        return plan;
    }

    private PlanAsignado planDeSemanas(Alumno alumno, LocalDate inicio, int semanas) {
        PlanAsignado plan = planBasico(alumno, inicio);
        plan.setMetodo(MetodoPlan.ULTRA_MASS);
        plan.setMesociclo("Hipertrofia");
        plan.setObjetivoMesociclo("Aumentar masa muscular");
        plan.setCantidadSemanas(semanas);
        return planRepository.save(plan);
    }

    /** Una sesión ya finalizada. */
    private void registrar(PlanAsignado plan, int semana, int sesion, LocalDate fecha) {
        RegistroSesion registro = new RegistroSesion(plan, semana, sesion, fecha);
        registro.setCompletada(true);
        registroRepository.save(registro);
    }

    // --- Pedidos ---

    private Respuesta planActivoDe(Alumno alumno) throws Exception {
        return ejecutar(get("/mi-progreso/plan-activo").with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    private Respuesta ejecutar(MockHttpServletRequestBuilder pedido) throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(pedido).andReturn().getResponse();
        String texto = respuesta.getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(respuesta.getStatus(), texto.isEmpty() ? json.createObjectNode() : json.readTree(texto));
    }

    // --- Las pruebas corren aisladas ---

    @Test
    void las_pruebas_corren_contra_la_base_embebida() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            String url = conexion.getMetaData().getURL();
            assertTrue(url.contains("localhost"), url);
            assertFalse(url.contains("neon.tech"), url);
        }
    }

    // --- Plan actual ---

    @Test
    void sin_plan_activo_responde_204() throws Exception {
        assertEquals(204, planActivoDe(alumno()).status());
    }

    @Test
    void un_plan_finalizado_no_es_el_plan_activo() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusWeeks(6), 5);
        plan.finalizar(ZonaHoraria.hoy().minusWeeks(1));
        planRepository.save(plan);

        assertEquals(204, planActivoDe(alumno).status());
    }

    @Test
    void el_resumen_trae_los_datos_del_plan_y_sus_fechas() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(8);
        PlanAsignado plan = planDeSemanas(alumno, inicio, 5);

        Respuesta respuesta = planActivoDe(alumno);

        assertEquals(200, respuesta.status());
        JsonNode cuerpo = respuesta.cuerpo();
        assertEquals(plan.getId(), cuerpo.path("id").asLong());
        assertEquals("ULTRA_MASS", cuerpo.path("metodo").asString());
        assertEquals("Hipertrofia", cuerpo.path("mesociclo").asString());
        assertEquals("Aumentar masa muscular", cuerpo.path("objetivoMesociclo").asString());
        assertEquals("ACTIVO", cuerpo.path("estado").asString());
        assertEquals(inicio.toString(), cuerpo.path("fechaInicio").asString());
        assertEquals(inicio.plusDays(34).toString(), cuerpo.path("fechaFin").asString());
        assertEquals(5, cuerpo.path("cantidadSemanas").asInt());
        assertEquals(10, cuerpo.path("sesionesTotales").asInt());
    }

    @Test
    void las_semanas_son_de_siete_dias_desde_la_asignacion_y_repiten_las_sesiones() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(1);
        planDeSemanas(alumno, inicio, 3);

        JsonNode semanas = planActivoDe(alumno).cuerpo().path("semanas");

        assertEquals(3, semanas.size());
        JsonNode segunda = semanas.get(1);
        assertEquals(2, segunda.path("numero").asInt());
        assertEquals(inicio.plusDays(7).toString(), segunda.path("desde").asString());
        assertEquals(inicio.plusDays(13).toString(), segunda.path("hasta").asString());
        assertEquals(2, segunda.path("sesiones").size());
    }

    @Test
    void la_semana_actual_sale_de_la_fecha_de_hoy() throws Exception {
        LocalDate hoy = ZonaHoraria.hoy();
        Alumno primerDia = alumno();
        planDeSemanas(primerDia, hoy, 5);
        Alumno ultimoDiaDeLaPrimera = alumno();
        planDeSemanas(ultimoDiaDeLaPrimera, hoy.minusDays(6), 5);
        Alumno primerDiaDeLaSegunda = alumno();
        planDeSemanas(primerDiaDeLaSegunda, hoy.minusDays(7), 5);

        assertEquals(1, planActivoDe(primerDia).cuerpo().path("semanaActual").asInt());
        assertEquals(1, planActivoDe(ultimoDiaDeLaPrimera).cuerpo().path("semanaActual").asInt());
        assertEquals(2, planActivoDe(primerDiaDeLaSegunda).cuerpo().path("semanaActual").asInt());
    }

    @Test
    void pasado_el_fin_previsto_la_semana_actual_es_la_ultima() throws Exception {
        Alumno alumno = alumno();
        planDeSemanas(alumno, ZonaHoraria.hoy().minusWeeks(9), 5);

        assertEquals(5, planActivoDe(alumno).cuerpo().path("semanaActual").asInt());
    }

    @Test
    void cada_sesion_informa_su_estado_y_la_fecha_en_que_se_hizo() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(8);
        PlanAsignado plan = planDeSemanas(alumno, inicio, 5);
        registrar(plan, 1, 1, inicio.plusDays(1));
        registrar(plan, 2, 2, inicio.plusDays(8));

        JsonNode cuerpo = planActivoDe(alumno).cuerpo();

        assertEquals(2, cuerpo.path("sesionesRegistradas").asInt());
        JsonNode primeraSemana = cuerpo.path("semanas").get(0).path("sesiones");
        assertEquals("REGISTRADA", primeraSemana.get(0).path("estado").asString());
        assertEquals(inicio.plusDays(1).toString(), primeraSemana.get(0).path("fechaRegistro").asString());
        assertEquals("PENDIENTE", primeraSemana.get(1).path("estado").asString());
        assertTrue(primeraSemana.get(1).path("fechaRegistro").isNull());
        JsonNode segundaSemana = cuerpo.path("semanas").get(1).path("sesiones");
        assertEquals("PENDIENTE", segundaSemana.get(0).path("estado").asString());
        assertEquals("REGISTRADA", segundaSemana.get(1).path("estado").asString());
    }

    @Test
    void un_registro_de_una_sesion_que_ya_no_existe_no_cuenta() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusDays(8), 5);
        registrar(plan, 1, 1, ZonaHoraria.hoy());
        registrar(plan, 1, 9, ZonaHoraria.hoy());

        assertEquals(1, planActivoDe(alumno).cuerpo().path("sesionesRegistradas").asInt());
    }

    @Test
    void cada_ejercicio_trae_lo_que_indico_el_profe() throws Exception {
        Alumno alumno = alumno();
        planDeSemanas(alumno, ZonaHoraria.hoy(), 2);

        JsonNode ejercicios = planActivoDe(alumno).cuerpo().path("semanas").get(0).path("sesiones").get(0).path("ejercicios");

        assertEquals(2, ejercicios.size());
        JsonNode completo = ejercicios.get(0);
        assertTrue(completo.path("nombre").asString().startsWith("Sentadilla"));
        assertEquals(4, completo.path("series").asInt());
        assertEquals(15, completo.path("repeticiones").asInt());
        assertEquals(18, completo.path("repeticionesMax").asInt());
        assertEquals(3, completo.path("rirObjetivo").asInt());
        assertEquals(0, new BigDecimal("1.25").compareTo(completo.path("descansoMinutos").decimalValue()));
        assertEquals("Tempo 2-1-3", completo.path("indicaciones").asString());
        JsonNode sinOpcionales = ejercicios.get(1);
        assertTrue(sinOpcionales.path("repeticionesMax").isNull());
        assertTrue(sinOpcionales.path("rirObjetivo").isNull());
        assertTrue(sinOpcionales.path("descansoMinutos").isNull());
        assertTrue(sinOpcionales.path("indicaciones").isNull());
    }

    @Test
    void un_plan_sin_los_datos_nuevos_se_toma_como_de_una_semana() throws Exception {
        Alumno alumno = alumno();
        planRepository.save(planBasico(alumno, ZonaHoraria.hoy().minusDays(20)));

        JsonNode cuerpo = planActivoDe(alumno).cuerpo();

        assertEquals("Plan de prueba", cuerpo.path("nombre").asString());
        assertTrue(cuerpo.path("metodo").isNull());
        assertTrue(cuerpo.path("mesociclo").isNull());
        assertEquals(1, cuerpo.path("cantidadSemanas").asInt());
        assertEquals(1, cuerpo.path("semanaActual").asInt());
        assertEquals(2, cuerpo.path("sesionesTotales").asInt());
    }

    @Test
    void con_fecha_fin_las_semanas_salen_del_rango_de_fechas() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy();
        PlanAsignado plan = planBasico(alumno, inicio);
        plan.setFechaFin(inicio.plusDays(20));
        planRepository.save(plan);

        JsonNode cuerpo = planActivoDe(alumno).cuerpo();

        assertEquals(3, cuerpo.path("cantidadSemanas").asInt());
        assertEquals(3, cuerpo.path("semanas").size());
        assertEquals(6, cuerpo.path("sesionesTotales").asInt());
    }

    @Test
    void si_la_fecha_fin_no_cierra_una_semana_la_ultima_queda_incompleta() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy();
        PlanAsignado plan = planBasico(alumno, inicio);
        plan.setFechaFin(inicio.plusDays(21));
        planRepository.save(plan);

        JsonNode semanas = planActivoDe(alumno).cuerpo().path("semanas");

        assertEquals(4, semanas.size());
        assertEquals(inicio.plusDays(21).toString(), semanas.get(3).path("desde").asString());
        assertEquals(inicio.plusDays(21).toString(), semanas.get(3).path("hasta").asString());
    }

    @Test
    void cada_alumno_ve_solo_su_plan() throws Exception {
        Alumno conPlan = alumno();
        planDeSemanas(conPlan, ZonaHoraria.hoy(), 5);

        assertEquals(200, planActivoDe(conPlan).status());
        assertEquals(204, planActivoDe(alumno()).status());
    }

    // --- Planes ---

    private PlanAsignado planFinalizado(Alumno alumno, LocalDate inicio, int semanas, LocalDate fin) {
        PlanAsignado plan = planDeSemanas(alumno, inicio, semanas);
        plan.finalizar(fin);
        return planRepository.save(plan);
    }

    private Respuesta planesDe(Alumno alumno) throws Exception {
        return ejecutar(get("/mi-progreso/planes").with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    @Test
    void sin_planes_no_hay_activo_ni_anteriores() throws Exception {
        Respuesta respuesta = planesDe(alumno());

        assertEquals(200, respuesta.status());
        assertTrue(respuesta.cuerpo().path("activo").isNull());
        assertEquals(0, respuesta.cuerpo().path("anteriores").size());
    }

    @Test
    void el_plan_activo_informa_su_fin_previsto_y_lo_registrado() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(8);
        PlanAsignado plan = planDeSemanas(alumno, inicio, 5);
        registrar(plan, 1, 1, inicio);
        registrar(plan, 1, 2, inicio.plusDays(1));

        JsonNode activo = planesDe(alumno).cuerpo().path("activo");

        assertEquals(plan.getId(), activo.path("id").asLong());
        assertEquals("ULTRA_MASS", activo.path("metodo").asString());
        assertEquals("Hipertrofia", activo.path("mesociclo").asString());
        assertEquals("ACTIVO", activo.path("estado").asString());
        assertEquals(inicio.toString(), activo.path("fechaInicio").asString());
        assertEquals(inicio.plusDays(34).toString(), activo.path("fechaFin").asString());
        assertEquals(5, activo.path("cantidadSemanas").asInt());
        assertEquals(2, activo.path("sesionesRegistradas").asInt());
        assertEquals(10, activo.path("sesionesTotales").asInt());
    }

    @Test
    void los_anteriores_van_del_mas_reciente_al_mas_antiguo_con_su_fecha_real_de_fin() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado antiguo = planFinalizado(alumno, hoy.minusWeeks(20), 3, hoy.minusWeeks(17));
        PlanAsignado reciente = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(3));
        PlanAsignado intermedio = planFinalizado(alumno, hoy.minusWeeks(12), 4, hoy.minusWeeks(8));
        planDeSemanas(alumno, hoy, 5);

        JsonNode anteriores = planesDe(alumno).cuerpo().path("anteriores");

        assertEquals(3, anteriores.size());
        assertEquals(reciente.getId(), anteriores.get(0).path("id").asLong());
        assertEquals(intermedio.getId(), anteriores.get(1).path("id").asLong());
        assertEquals(antiguo.getId(), anteriores.get(2).path("id").asLong());
        assertEquals("FINALIZADO", anteriores.get(0).path("estado").asString());
        assertEquals(hoy.minusWeeks(3).toString(), anteriores.get(0).path("fechaFin").asString());
    }

    @Test
    void sin_plan_activo_igual_lista_los_anteriores() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));

        JsonNode cuerpo = planesDe(alumno).cuerpo();

        assertTrue(cuerpo.path("activo").isNull());
        assertEquals(1, cuerpo.path("anteriores").size());
    }

    @Test
    void el_listado_no_incluye_planes_de_otro_alumno() throws Exception {
        planDeSemanas(alumno(), ZonaHoraria.hoy(), 5);

        JsonNode cuerpo = planesDe(alumno()).cuerpo();

        assertTrue(cuerpo.path("activo").isNull());
        assertEquals(0, cuerpo.path("anteriores").size());
    }

    // --- Detalle de un plan ---

    private Respuesta planDe(Alumno alumno, Long planId) throws Exception {
        return ejecutar(get("/mi-progreso/planes/" + planId).with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    @Test
    void un_plan_finalizado_arranca_en_su_ultima_semana_con_su_fecha_real_de_fin() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado plan = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));

        Respuesta respuesta = planDe(alumno, plan.getId());

        assertEquals(200, respuesta.status());
        JsonNode cuerpo = respuesta.cuerpo();
        assertEquals("FINALIZADO", cuerpo.path("estado").asString());
        assertEquals(5, cuerpo.path("semanaActual").asInt());
        assertEquals(hoy.minusWeeks(1).toString(), cuerpo.path("fechaFin").asString());
    }

    @Test
    void en_un_plan_finalizado_lo_que_no_se_hizo_figura_como_no_registrada() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado plan = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));
        registrar(plan, 5, 1, hoy.minusWeeks(2));

        JsonNode sesiones = planDe(alumno, plan.getId()).cuerpo().path("semanas").get(4).path("sesiones");

        assertEquals("REGISTRADA", sesiones.get(0).path("estado").asString());
        assertEquals("NO_REGISTRADA", sesiones.get(1).path("estado").asString());
        assertTrue(sesiones.get(1).path("fechaRegistro").isNull());
    }

    @Test
    void con_plan_activo_el_detalle_de_uno_anterior_devuelve_el_anterior() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado anterior = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));
        planDeSemanas(alumno, hoy, 3);

        JsonNode cuerpo = planDe(alumno, anterior.getId()).cuerpo();

        assertEquals(anterior.getId(), cuerpo.path("id").asLong());
        assertEquals("FINALIZADO", cuerpo.path("estado").asString());
        assertEquals(5, cuerpo.path("cantidadSemanas").asInt());
    }

    @Test
    void el_plan_de_otro_alumno_responde_404() throws Exception {
        PlanAsignado ajeno = planFinalizado(alumno(), ZonaHoraria.hoy().minusWeeks(6), 5, ZonaHoraria.hoy());

        assertEquals(404, planDe(alumno(), ajeno.getId()).status());
    }

    @Test
    void un_plan_que_no_existe_responde_404() throws Exception {
        assertEquals(404, planDe(alumno(), 999_999L).status());
    }

    // --- Sesiones registradas ---

    /** Registra la sesión ya finalizada con todos sus ejercicios, con los kg, el RIR real y la nota indicados. */
    private RegistroSesion registrarConEjercicios(PlanAsignado plan, int semana, int numeroSesion, LocalDate fecha,
            String kg, Integer rirReal, String notas) {
        RegistroSesion registro = new RegistroSesion(plan, semana, numeroSesion, fecha);
        registro.setCompletada(true);
        registro.agregarEjercicios(plan.getSesiones().get(numeroSesion - 1).getEjercicios().stream()
                .map(indicado -> new RegistroEjercicio(indicado, kg == null ? null : new BigDecimal(kg), rirReal, notas))
                .toList());
        return registroRepository.save(registro);
    }

    private Respuesta sesionesDe(Alumno alumno, Long planId) throws Exception {
        return ejecutar(get("/mi-progreso/planes/" + planId + "/sesiones").with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    @Test
    void un_plan_sin_registros_no_tiene_sesiones() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);

        Respuesta respuesta = sesionesDe(alumno, plan.getId());

        assertEquals(200, respuesta.status());
        assertEquals(0, respuesta.cuerpo().size());
    }

    @Test
    void las_sesiones_van_de_la_mas_reciente_a_la_mas_antigua_y_solo_las_registradas() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(10);
        PlanAsignado plan = planDeSemanas(alumno, inicio, 5);
        RegistroSesion primera = registrarConEjercicios(plan, 1, 1, inicio, "40", 2, null);
        RegistroSesion tercera = registrarConEjercicios(plan, 2, 2, inicio.plusDays(9), "40", 2, null);
        RegistroSesion segunda = registrarConEjercicios(plan, 2, 1, inicio.plusDays(8), "40", 2, null);

        JsonNode sesiones = sesionesDe(alumno, plan.getId()).cuerpo();

        assertEquals(3, sesiones.size());
        assertEquals(tercera.getId(), sesiones.get(0).path("id").asLong());
        assertEquals(segunda.getId(), sesiones.get(1).path("id").asLong());
        assertEquals(primera.getId(), sesiones.get(2).path("id").asLong());
        assertEquals(2, sesiones.get(0).path("semana").asInt());
        assertEquals(2, sesiones.get(0).path("numeroSesion").asInt());
        assertEquals(inicio.plusDays(9).toString(), sesiones.get(0).path("fecha").asString());
    }

    @Test
    void cada_sesion_informa_cuantos_ejercicios_se_registraron() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        registrarConEjercicios(plan, 1, 1, ZonaHoraria.hoy(), "40", 2, null);

        assertEquals(2, sesionesDe(alumno, plan.getId()).cuerpo().get(0).path("ejerciciosRegistrados").asInt());
    }

    @Test
    void las_sesiones_de_un_plan_finalizado_se_pueden_consultar() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado plan = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));
        registrarConEjercicios(plan, 1, 1, hoy.minusWeeks(6), "40", 2, null);

        assertEquals(1, sesionesDe(alumno, plan.getId()).cuerpo().size());
    }

    @Test
    void las_sesiones_del_plan_de_otro_alumno_responden_404() throws Exception {
        PlanAsignado ajeno = planDeSemanas(alumno(), ZonaHoraria.hoy(), 5);

        assertEquals(404, sesionesDe(alumno(), ajeno.getId()).status());
    }

    // --- Detalle de sesión ---

    private Respuesta sesionDe(Alumno alumno, Long registroId) throws Exception {
        return ejecutar(get("/mi-progreso/sesiones/" + registroId).with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    @Test
    void el_detalle_trae_la_sesion_el_plan_y_lo_indicado_y_realizado() throws Exception {
        Alumno alumno = alumno();
        LocalDate fecha = ZonaHoraria.hoy().minusDays(1);
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusDays(8), 5);
        RegistroSesion registro = registrarConEjercicios(plan, 2, 1, fecha, "40.5", 2, "Me costó la última serie.");

        Respuesta respuesta = sesionDe(alumno, registro.getId());

        assertEquals(200, respuesta.status());
        JsonNode cuerpo = respuesta.cuerpo();
        assertEquals(2, cuerpo.path("semana").asInt());
        assertEquals(1, cuerpo.path("numeroSesion").asInt());
        assertEquals(fecha.toString(), cuerpo.path("fecha").asString());
        assertEquals("ULTRA_MASS", cuerpo.path("metodo").asString());
        assertEquals("Hipertrofia", cuerpo.path("mesociclo").asString());
        JsonNode ejercicio = cuerpo.path("ejercicios").get(0);
        assertTrue(ejercicio.path("nombre").asString().startsWith("Sentadilla"));
        assertEquals(4, ejercicio.path("series").asInt());
        assertEquals(15, ejercicio.path("repeticiones").asInt());
        assertEquals(18, ejercicio.path("repeticionesMax").asInt());
        assertEquals(3, ejercicio.path("rirObjetivo").asInt());
        assertEquals("Tempo 2-1-3", ejercicio.path("indicaciones").asString());
        assertEquals(0, new BigDecimal("40.5").compareTo(ejercicio.path("kgUsados").decimalValue()));
        assertEquals(2, ejercicio.path("rirReal").asInt());
        assertEquals("Me costó la última serie.", ejercicio.path("notas").asString());
    }

    @Test
    void los_ejercicios_van_en_el_orden_de_la_sesion() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        RegistroSesion registro = registrarConEjercicios(plan, 1, 1, ZonaHoraria.hoy(), "40", 2, null);

        JsonNode ejercicios = sesionDe(alumno, registro.getId()).cuerpo().path("ejercicios");

        assertEquals(2, ejercicios.size());
        assertTrue(ejercicios.get(0).path("nombre").asString().startsWith("Sentadilla"));
        assertTrue(ejercicios.get(1).path("nombre").asString().startsWith("Prensa"));
    }

    @Test
    void la_diferencia_de_rir_es_el_real_menos_el_objetivo() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusDays(8), 5);
        // La sesión 1 empieza con un RIR objetivo 3 y la sesión 2 tiene RIR objetivo 2
        RegistroSesion menor = registrarConEjercicios(plan, 1, 1, ZonaHoraria.hoy(), "40", 2, null);
        RegistroSesion mayor = registrarConEjercicios(plan, 1, 2, ZonaHoraria.hoy(), "40", 3, null);

        assertEquals(-1, sesionDe(alumno, menor.getId()).cuerpo().path("ejercicios").get(0).path("diferenciaRir").asInt());
        assertEquals(1, sesionDe(alumno, mayor.getId()).cuerpo().path("ejercicios").get(0).path("diferenciaRir").asInt());
    }

    @Test
    void sin_diferencia_de_rir_si_coincide_o_si_falta_un_dato() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusDays(8), 5);
        RegistroSesion coincide = registrarConEjercicios(plan, 1, 1, ZonaHoraria.hoy(), "40", 3, null);
        RegistroSesion sinReal = registrarConEjercicios(plan, 2, 1, ZonaHoraria.hoy(), "40", null, null);

        JsonNode ejercicios = sesionDe(alumno, coincide.getId()).cuerpo().path("ejercicios");
        assertTrue(ejercicios.get(0).path("diferenciaRir").isNull());
        // El segundo ejercicio de la sesión 1 no tiene RIR objetivo
        assertTrue(ejercicios.get(1).path("diferenciaRir").isNull());
        assertTrue(sesionDe(alumno, sinReal.getId()).cuerpo().path("ejercicios").get(0).path("diferenciaRir").isNull());
    }

    @Test
    void lo_indicado_no_cambia_si_el_profe_modifica_el_plan() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        RegistroSesion registro = registrarConEjercicios(plan, 1, 1, ZonaHoraria.hoy(), "40", 3, null);

        plan.reemplazarSesiones(List.of(sesion(ejercicio("Otro", 2, 5, null, null, 1, null))));
        planRepository.save(plan);

        JsonNode ejercicios = sesionDe(alumno, registro.getId()).cuerpo().path("ejercicios");
        assertEquals(2, ejercicios.size());
        assertTrue(ejercicios.get(0).path("nombre").asString().startsWith("Sentadilla"));
        assertEquals(4, ejercicios.get(0).path("series").asInt());
        assertEquals("Tempo 2-1-3", ejercicios.get(0).path("indicaciones").asString());
    }

    @Test
    void la_sesion_de_otro_alumno_responde_404() throws Exception {
        PlanAsignado ajeno = planDeSemanas(alumno(), ZonaHoraria.hoy(), 5);
        RegistroSesion registro = registrarConEjercicios(ajeno, 1, 1, ZonaHoraria.hoy(), "40", 2, null);

        assertEquals(404, sesionDe(alumno(), registro.getId()).status());
    }

    @Test
    void una_sesion_que_no_existe_responde_404() throws Exception {
        assertEquals(404, sesionDe(alumno(), 999_999L).status());
    }

    // --- Seguridad ---

    @Test
    void sin_token_responde_401() throws Exception {
        assertEquals(401, ejecutar(get("/mi-progreso/plan-activo")).status());
    }

    @Test
    void el_personal_no_puede_consultarlo() throws Exception {
        for (String rol : List.of("PROPIETARIO", "ENTRENADOR", "NUTRICIONISTA")) {
            Respuesta respuesta = ejecutar(get("/mi-progreso/plan-activo").with(user(rol.toLowerCase() + "@test.com").roles(rol)));
            assertEquals(403, respuesta.status(), rol);
        }
    }

    @Test
    void una_cuenta_inactiva_recibe_403_con_el_motivo() throws Exception {
        Respuesta respuesta = planActivoDe(alumno(false));

        assertEquals(403, respuesta.status());
        assertEquals("Tu cuenta está inactiva.", respuesta.cuerpo().path("error").asString());
    }

    // --- HU-266: fecha fin real y planes vencidos ---

    private PlanAsignado planConFin(Alumno alumno, LocalDate inicio, int semanas, LocalDate fechaFin) {
        PlanAsignado plan = planDeSemanas(alumno, inicio, semanas);
        plan.setFechaFin(fechaFin);
        return planRepository.save(plan);
    }

    @Test
    void con_fecha_fin_real_el_plan_activo_informa_esa_y_no_la_calculada_por_semanas() throws Exception {
        Alumno alumno = alumno();
        LocalDate inicio = ZonaHoraria.hoy().minusDays(8);
        // Por semanas terminaría el día 34; la fecha fin real manda
        planConFin(alumno, inicio, 5, inicio.plusDays(50));

        assertEquals(inicio.plusDays(50).toString(), planActivoDe(alumno).cuerpo().path("fechaFin").asString());
        assertEquals(inicio.plusDays(50).toString(), planesDe(alumno).cuerpo().path("activo").path("fechaFin").asString());
    }

    @Test
    void el_dia_de_la_fecha_fin_sigue_siendo_el_plan_actual() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado plan = planConFin(alumno, hoy.minusWeeks(4), 4, hoy);

        Respuesta activo = planActivoDe(alumno);

        assertEquals(200, activo.status());
        assertEquals(plan.getId(), activo.cuerpo().path("id").asLong());
        assertEquals("ACTIVO", activo.cuerpo().path("estado").asString());
    }

    @Test
    void un_plan_vencido_pasa_a_anteriores_como_finalizado_el_dia_de_su_fecha_fin() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado finalizado = planFinalizado(alumno, hoy.minusWeeks(20), 3, hoy.minusWeeks(17));
        PlanAsignado vencido = planConFin(alumno, hoy.minusWeeks(4), 4, hoy.minusDays(1));

        assertEquals(204, planActivoDe(alumno).status());
        JsonNode cuerpo = planesDe(alumno).cuerpo();
        assertTrue(cuerpo.path("activo").isNull());
        JsonNode anteriores = cuerpo.path("anteriores");
        assertEquals(2, anteriores.size());
        // El vencido terminó ayer, así que va antes que el finalizado hace 17 semanas
        assertEquals(vencido.getId(), anteriores.get(0).path("id").asLong());
        assertEquals("FINALIZADO", anteriores.get(0).path("estado").asString());
        assertEquals(hoy.minusDays(1).toString(), anteriores.get(0).path("fechaFin").asString());
        assertEquals(finalizado.getId(), anteriores.get(1).path("id").asLong());
    }

    @Test
    void en_un_plan_vencido_lo_que_no_se_hizo_figura_como_no_registrada() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado vencido = planConFin(alumno, hoy.minusWeeks(2), 2, hoy.minusDays(1));
        registrar(vencido, 2, 1, hoy.minusDays(3));

        JsonNode detalle = planDe(alumno, vencido.getId()).cuerpo();

        assertEquals("FINALIZADO", detalle.path("estado").asString());
        assertEquals(2, detalle.path("semanaActual").asInt());
        JsonNode ultimaSemana = detalle.path("semanas").get(1).path("sesiones");
        assertEquals("REGISTRADA", ultimaSemana.get(0).path("estado").asString());
        assertEquals("NO_REGISTRADA", ultimaSemana.get(1).path("estado").asString());
    }

    // --- HU-269: registrar una sesión ---
    // El plan básico tiene la sesión 1 con Sentadilla (4 series) y Prensa (4 series), y la sesión 2 con Remo (3 series).

    private Respuesta iniciar(Alumno alumno, Long planId, int semana, int numeroSesion) throws Exception {
        return ejecutar(post("/mi-progreso/planes/" + planId + "/sesiones")
                .with(user(alumno.getEmail()).roles("ALUMNO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"semana\":" + semana + ",\"numeroSesion\":" + numeroSesion + "}"));
    }

    private Respuesta registrarEjercicio(Alumno alumno, long registroId, int orden, String cuerpo) throws Exception {
        return ejecutar(put("/mi-progreso/sesiones/" + registroId + "/ejercicios/" + orden)
                .with(user(alumno.getEmail()).roles("ALUMNO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private Respuesta completar(Alumno alumno, long registroId) throws Exception {
        return ejecutar(post("/mi-progreso/sesiones/" + registroId + "/completar").with(user(alumno.getEmail()).roles("ALUMNO")));
    }

    /** Las series 1..cantidad, todas con el mismo peso, repeticiones y RIR. */
    private static String series(int cantidad, String peso, int repeticiones, int rir) {
        return "{\"series\":[" + IntStream.rangeClosed(1, cantidad)
                .mapToObj(orden -> serie(orden, peso, String.valueOf(repeticiones), String.valueOf(rir)))
                .collect(Collectors.joining(",")) + "]}";
    }

    /** Un valor "null" queda como null en el JSON. */
    private static String serie(int orden, String peso, String repeticiones, String rir) {
        return "{\"orden\":" + orden + ",\"peso\":" + peso + ",\"repeticiones\":" + repeticiones + ",\"rir\":" + rir + "}";
    }

    private long iniciarYObtenerId(Alumno alumno, PlanAsignado plan, int semana, int numeroSesion) throws Exception {
        Respuesta respuesta = iniciar(alumno, plan.getId(), semana, numeroSesion);
        assertEquals(200, respuesta.status(), respuesta.cuerpo().toString());
        return respuesta.cuerpo().path("registroId").asLong();
    }

    private JsonNode sesionDelPlanActivo(Alumno alumno, int semana, int numeroSesion) throws Exception {
        return planActivoDe(alumno).cuerpo().path("semanas").get(semana - 1).path("sesiones").get(numeroSesion - 1);
    }

    private int seriesGuardadas(long registroId) {
        return new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from registros_serie s join registros_ejercicio e on e.id = s.registro_ejercicio_id"
                        + " where e.registro_sesion_id = ?",
                Integer.class, registroId);
    }

    @Test
    void iniciar_una_sesion_del_plan_activo_lista_sus_ejercicios_con_sus_series() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);

        Respuesta respuesta = iniciar(alumno, plan.getId(), 1, 1);

        assertEquals(200, respuesta.status());
        JsonNode sesion = respuesta.cuerpo();
        assertEquals("EN_CURSO", sesion.path("estado").asString());
        assertTrue(sesion.path("registroId").asLong() > 0);
        assertTrue(sesion.path("registrable").asBoolean());
        assertEquals(ZonaHoraria.hoy().toString(), sesion.path("fechaRegistro").asString());
        JsonNode ejercicios = sesion.path("ejercicios");
        assertEquals(2, ejercicios.size());
        assertEquals(1, ejercicios.get(0).path("orden").asInt());
        assertTrue(ejercicios.get(0).path("nombre").asString().startsWith("Sentadilla"));
        assertEquals(4, ejercicios.get(0).path("series").asInt());
        assertFalse(ejercicios.get(0).path("completo").asBoolean());
        assertEquals(2, ejercicios.get(1).path("orden").asInt());
        assertEquals(4, ejercicios.get(1).path("series").asInt());
    }

    @Test
    void una_sesion_en_curso_no_cuenta_como_registrada_y_se_recupera_al_volver() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        registrarEjercicio(alumno, registroId, 2, series(4, "60", 12, 2));

        JsonNode cuerpo = planActivoDe(alumno).cuerpo();

        assertEquals(0, cuerpo.path("sesionesRegistradas").asInt());
        JsonNode sesion = cuerpo.path("semanas").get(0).path("sesiones").get(0);
        assertEquals("EN_CURSO", sesion.path("estado").asString());
        assertEquals(registroId, sesion.path("registroId").asLong());
        assertFalse(sesion.path("ejercicios").get(0).path("completo").asBoolean());
        assertTrue(sesion.path("ejercicios").get(1).path("completo").asBoolean());
        assertEquals(4, sesion.path("ejercicios").get(1).path("seriesRealizadas").size());
        assertEquals(0, sesionesDe(alumno, plan.getId()).cuerpo().size());
    }

    @Test
    void iniciar_dos_veces_la_misma_sesion_devuelve_el_mismo_registro() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);

        long primero = iniciarYObtenerId(alumno, plan, 1, 1);
        long segundo = iniciarYObtenerId(alumno, plan, 1, 1);

        assertEquals(primero, segundo);
        assertEquals(1, registroRepository.findByPlanId(plan.getId()).size());
    }

    @Test
    void dos_pedidos_simultaneos_de_iniciar_crean_un_solo_registro() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Respuesta> pedido = () -> {
            largada.await();
            return iniciar(alumno, plan.getId(), 1, 1);
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Respuesta> uno = hilos.submit(pedido);
            Future<Respuesta> otro = hilos.submit(pedido);
            largada.countDown();

            assertEquals(200, uno.get().status(), uno.get().cuerpo().toString());
            assertEquals(200, otro.get().status(), otro.get().cuerpo().toString());
            assertEquals(uno.get().cuerpo().path("registroId").asLong(), otro.get().cuerpo().path("registroId").asLong());
        } finally {
            hilos.shutdown();
        }
        assertEquals(1, registroRepository.findByPlanId(plan.getId()).size());
    }

    @Test
    void registrar_todas_las_series_marca_el_ejercicio_y_guarda_el_mayor_1rm() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        // 1RM = peso x (1 + (reps + RIR) / 30): 133,33 / 132,00 / 126,00 / 133,00
        String cuerpo = "{\"notas\":\"  Buenas sensaciones  \",\"series\":[" + String.join(",",
                serie(1, "100", "8", "2"), serie(2, "110", "5", "1"), serie(3, "90", "10", "2"), serie(4, "105", "6", "2")) + "]}";

        Respuesta respuesta = registrarEjercicio(alumno, registroId, 1, cuerpo);

        assertEquals(200, respuesta.status(), respuesta.cuerpo().toString());
        JsonNode ejercicio = respuesta.cuerpo().path("ejercicios").get(0);
        assertTrue(ejercicio.path("completo").asBoolean());
        assertEquals(0, new BigDecimal("133.33").compareTo(ejercicio.path("rmEstimado").decimalValue()));
        assertEquals("Buenas sensaciones", ejercicio.path("notas").asString());
        JsonNode realizadas = ejercicio.path("seriesRealizadas");
        assertEquals(4, realizadas.size());
        assertEquals(0, new BigDecimal("132.00").compareTo(realizadas.get(1).path("rmEstimado").decimalValue()));
        assertEquals("EN_CURSO", respuesta.cuerpo().path("estado").asString());
        // El detalle de HU-270 toma kg y RIR de la serie de mayor 1RM
        JsonNode detalle = sesionDe(alumno, registroId).cuerpo().path("ejercicios").get(0);
        assertEquals(0, new BigDecimal("100").compareTo(detalle.path("kgUsados").decimalValue()));
        assertEquals(2, detalle.path("rirReal").asInt());
        assertEquals(0, new BigDecimal("133.33").compareTo(detalle.path("rmEstimado").decimalValue()));
        assertEquals(4, detalle.path("seriesRealizadas").size());
    }

    @Test
    void el_peso_puede_ser_cero_para_ejercicios_con_el_peso_del_cuerpo() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 2);

        assertEquals(200, registrarEjercicio(alumno, registroId, 1, series(3, "0", 10, 2)).status());
    }

    @Test
    void los_ejercicios_se_registran_en_cualquier_orden() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);

        assertEquals(200, registrarEjercicio(alumno, registroId, 2, series(4, "60", 12, 2)).status());
        Respuesta despues = registrarEjercicio(alumno, registroId, 1, series(4, "80", 15, 3));

        assertEquals(200, despues.status());
        assertTrue(despues.cuerpo().path("ejercicios").get(0).path("completo").asBoolean());
        assertTrue(despues.cuerpo().path("ejercicios").get(1).path("completo").asBoolean());
    }

    @Test
    void con_alguna_serie_sin_peso_repeticiones_o_rir_el_ejercicio_no_se_marca() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 2);
        String completas = serie(1, "50", "10", "2") + "," + serie(2, "50", "10", "2") + ",";

        for (String incompleta : List.of(serie(3, "null", "10", "2"), serie(3, "50", "null", "2"), serie(3, "50", "10", "null"))) {
            Respuesta respuesta = registrarEjercicio(alumno, registroId, 1, "{\"series\":[" + completas + incompleta + "]}");
            assertEquals(400, respuesta.status(), incompleta);
        }

        assertFalse(sesionDelPlanActivo(alumno, 1, 2).path("ejercicios").get(0).path("completo").asBoolean());
        assertEquals(0, seriesGuardadas(registroId));
    }

    @Test
    void tienen_que_venir_exactamente_las_series_del_plan() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 2);
        String repetidas = "{\"series\":[" + String.join(",",
                serie(1, "50", "10", "2"), serie(1, "50", "10", "2"), serie(2, "50", "10", "2")) + "]}";

        Respuesta faltan = registrarEjercicio(alumno, registroId, 1, series(2, "50", 10, 2));

        assertEquals(400, faltan.status());
        assertEquals("Cargá peso, repeticiones y RIR de las 3 series del ejercicio", faltan.cuerpo().path("error").asString());
        assertEquals(400, registrarEjercicio(alumno, registroId, 1, series(4, "50", 10, 2)).status());
        assertEquals(400, registrarEjercicio(alumno, registroId, 1, repetidas).status());
        assertEquals(400, registrarEjercicio(alumno, registroId, 1, "{\"series\":[]}").status());
        assertEquals(0, seriesGuardadas(registroId));
    }

    @Test
    void un_ejercicio_hecho_se_puede_corregir_mientras_la_sesion_esta_en_curso() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        registrarEjercicio(alumno, registroId, 1, series(4, "100", 8, 2));

        Respuesta correccion = registrarEjercicio(alumno, registroId, 1, series(4, "80", 10, 0));

        assertEquals(200, correccion.status());
        JsonNode ejercicio = correccion.cuerpo().path("ejercicios").get(0);
        assertEquals(0, new BigDecimal("106.67").compareTo(ejercicio.path("rmEstimado").decimalValue()));
        assertEquals(0, new BigDecimal("80").compareTo(ejercicio.path("seriesRealizadas").get(0).path("peso").decimalValue()));
        assertEquals(4, seriesGuardadas(registroId));
    }

    @Test
    void no_se_puede_finalizar_la_sesion_sin_todos_sus_ejercicios() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        registrarEjercicio(alumno, registroId, 1, series(4, "100", 8, 2));

        Respuesta respuesta = completar(alumno, registroId);

        assertEquals(400, respuesta.status());
        assertEquals("Para finalizar la sesión, registrá todos sus ejercicios", respuesta.cuerpo().path("error").asString());
        assertEquals("EN_CURSO", sesionDelPlanActivo(alumno, 1, 1).path("estado").asString());
        assertEquals(0, planActivoDe(alumno).cuerpo().path("sesionesRegistradas").asInt());
        assertEquals(0, sesionesDe(alumno, plan.getId()).cuerpo().size());
    }

    @Test
    void con_todos_los_ejercicios_la_sesion_se_finaliza_y_queda_bloqueada() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        registrarEjercicio(alumno, registroId, 1, series(4, "100", 8, 2));
        registrarEjercicio(alumno, registroId, 2, series(4, "60", 12, 2));

        Respuesta respuesta = completar(alumno, registroId);

        assertEquals(200, respuesta.status(), respuesta.cuerpo().toString());
        assertEquals("REGISTRADA", respuesta.cuerpo().path("estado").asString());
        assertFalse(respuesta.cuerpo().path("registrable").asBoolean());
        assertEquals(1, planActivoDe(alumno).cuerpo().path("sesionesRegistradas").asInt());
        assertEquals(1, sesionesDe(alumno, plan.getId()).cuerpo().size());
        Respuesta corregir = registrarEjercicio(alumno, registroId, 1, series(4, "80", 10, 0));
        assertEquals(400, corregir.status());
        assertEquals("La sesión ya está finalizada", corregir.cuerpo().path("error").asString());
        assertEquals(400, completar(alumno, registroId).status());
        // Iniciarla de nuevo devuelve la finalizada, sin permitir cargarla
        Respuesta otraVez = iniciar(alumno, plan.getId(), 1, 1);
        assertEquals(registroId, otraVez.cuerpo().path("registroId").asLong());
        assertEquals("REGISTRADA", otraVez.cuerpo().path("estado").asString());
    }

    @Test
    void sin_plan_activo_no_se_puede_iniciar_una_sesion() throws Exception {
        Alumno alumno = alumno();
        LocalDate hoy = ZonaHoraria.hoy();
        PlanAsignado finalizado = planFinalizado(alumno, hoy.minusWeeks(6), 5, hoy.minusWeeks(1));
        Alumno conVencido = alumno();
        PlanAsignado vencido = planConFin(conVencido, hoy.minusWeeks(2), 2, hoy.minusDays(1));

        Respuesta respuesta = iniciar(alumno, finalizado.getId(), 5, 1);

        assertEquals(400, respuesta.status());
        assertEquals("Solo podés registrar sesiones de tu plan activo", respuesta.cuerpo().path("error").asString());
        assertEquals(400, iniciar(conVencido, vencido.getId(), 2, 1).status());
        assertEquals(0, registroRepository.findByPlanId(finalizado.getId()).size());
    }

    @Test
    void no_se_puede_seguir_cargando_una_sesion_si_el_plan_termino() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(alumno, plan, 1, 1);
        plan.finalizar(ZonaHoraria.hoy());
        planRepository.save(plan);

        assertEquals(400, registrarEjercicio(alumno, registroId, 1, series(4, "100", 8, 2)).status());
        // Lo que quedó sin terminar figura como no registrado
        JsonNode sesion = planDe(alumno, plan.getId()).cuerpo().path("semanas").get(0).path("sesiones").get(0);
        assertEquals("NO_REGISTRADA", sesion.path("estado").asString());
        assertTrue(sesion.path("registroId").isNull());
    }

    @Test
    void solo_se_registran_sesiones_de_la_semana_actual() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy().minusDays(8), 5);

        Respuesta pasada = iniciar(alumno, plan.getId(), 1, 1);

        assertEquals(400, pasada.status());
        assertEquals("Solo podés registrar sesiones de la semana actual", pasada.cuerpo().path("error").asString());
        assertEquals(400, iniciar(alumno, plan.getId(), 3, 1).status());
        assertEquals(200, iniciar(alumno, plan.getId(), 2, 1).status());
        JsonNode semanas = planActivoDe(alumno).cuerpo().path("semanas");
        assertFalse(semanas.get(0).path("sesiones").get(0).path("registrable").asBoolean());
        assertTrue(semanas.get(1).path("sesiones").get(1).path("registrable").asBoolean());
        assertFalse(semanas.get(2).path("sesiones").get(0).path("registrable").asBoolean());
    }

    @Test
    void una_sesion_que_no_existe_en_el_plan_no_se_puede_iniciar() throws Exception {
        Alumno alumno = alumno();
        PlanAsignado plan = planDeSemanas(alumno, ZonaHoraria.hoy(), 5);

        assertEquals(400, iniciar(alumno, plan.getId(), 1, 3).status());
    }

    @Test
    void no_se_registra_en_el_plan_ni_en_la_sesion_de_otro_alumno() throws Exception {
        Alumno duenio = alumno();
        PlanAsignado plan = planDeSemanas(duenio, ZonaHoraria.hoy(), 5);
        long registroId = iniciarYObtenerId(duenio, plan, 1, 1);
        Alumno otro = alumno();

        assertEquals(404, iniciar(otro, plan.getId(), 1, 1).status());
        assertEquals(404, registrarEjercicio(otro, registroId, 1, series(4, "100", 8, 2)).status());
        assertEquals(404, completar(otro, registroId).status());
        assertEquals(404, registrarEjercicio(duenio, registroId, 9, series(4, "100", 8, 2)).status());
    }
}
