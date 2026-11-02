package mogni.system_backend.progreso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

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

    private void registrar(PlanAsignado plan, int semana, int sesion, LocalDate fecha) {
        registroRepository.save(new RegistroSesion(plan, semana, sesion, fecha));
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
}
