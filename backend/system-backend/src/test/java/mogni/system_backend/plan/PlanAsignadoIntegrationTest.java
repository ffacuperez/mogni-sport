package mogni.system_backend.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * HU-266: pruebas de aceptación de punta a punta (HTTP, seguridad, JPA y base). Corren contra un PostgreSQL embebido
 * que se crea y se descarta en cada ejecución: el perfil "local" no se carga, así que nunca tocan una base compartida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanAsignadoIntegrationTest {

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
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper json = JsonMapper.builder().build();

    private record Respuesta(int status, JsonNode cuerpo) {

        String error() {
            return cuerpo.path("error").asString("");
        }
    }

    // --- Datos de prueba ---

    private Long alumno() {
        int n = SECUENCIA.incrementAndGet();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail("alumno" + n + "@test.com");
        cuenta.setPassword("x");
        cuenta.setRol(Rol.ALUMNO);
        Alumno alumno = new Alumno();
        alumno.setNombre("Alumno" + n);
        alumno.setApellido("Prueba");
        alumno.setEdad(30);
        alumno.setDni(String.valueOf(40_000_000 + n));
        alumno.setEmail(cuenta.getEmail());
        alumno.setCuenta(cuenta);
        return alumnoRepository.save(alumno).getId();
    }

    private Long ejercicio() {
        return ejercicio(true);
    }

    private Long ejercicio(boolean activo) {
        int n = SECUENCIA.incrementAndGet();
        return ejercicioRepository.save(new Ejercicio(null, "Ejercicio " + n, "Pecho", null, activo)).getId();
    }

    private static Map<String, Object> ejercicioDeSesion(Long id, int series, int repeticiones, Integer repeticionesMax,
            String descanso, String peso, Integer rir) {
        Map<String, Object> ejercicio = new HashMap<>();
        ejercicio.put("ejercicioId", id);
        ejercicio.put("series", series);
        ejercicio.put("repeticiones", repeticiones);
        ejercicio.put("repeticionesMax", repeticionesMax);
        ejercicio.put("descansoMinutos", descanso);
        ejercicio.put("pesoSugerido", peso);
        ejercicio.put("rirObjetivo", rir);
        return ejercicio;
    }

    private static Map<String, Object> ejercicioSimple(Long id) {
        return ejercicioDeSesion(id, 3, 10, null, null, null, null);
    }

    @SafeVarargs
    private static Map<String, Object> sesion(String nombre, Map<String, Object>... ejercicios) {
        Map<String, Object> sesion = new HashMap<>();
        sesion.put("nombre", nombre);
        sesion.put("ejercicios", Arrays.asList(ejercicios));
        return sesion;
    }

    @SafeVarargs
    private static Map<String, Object> datosPlan(String nombre, Map<String, Object>... sesiones) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("nombre", nombre);
        plan.put("nivel", "INTERMEDIO");
        plan.put("objetivo", "HIPERTROFIA");
        plan.put("sesiones", Arrays.asList(sesiones));
        return plan;
    }

    @SafeVarargs
    private static Map<String, Object> desdeCero(Long alumnoId, String nombre, Map<String, Object>... sesiones) {
        Map<String, Object> plan = datosPlan(nombre, sesiones);
        plan.put("alumnoId", alumnoId);
        return plan;
    }

    private static Map<String, Object> desdePlantilla(Long alumnoId, Long plantillaId) {
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("alumnoId", alumnoId);
        pedido.put("plantillaId", plantillaId);
        return pedido;
    }

    /** Plantilla con tres sesiones y todos los datos de un ejercicio, para ver que la copia no pierda nada. */
    private JsonNode plantillaCompleta() throws Exception {
        Long press = ejercicio();
        Long remo = ejercicio();
        Long sentadilla = ejercicio();
        Map<String, Object> pedido = datosPlan("Plantilla " + SECUENCIA.incrementAndGet(),
                sesion("Empuje", ejercicioDeSesion(sentadilla, 4, 8, 10, "1.5", "60.25", 2), ejercicioSimple(press)),
                sesion(null, ejercicioDeSesion(remo, 5, 5, null, "3", null, 0)),
                sesion("Piernas", ejercicioSimple(sentadilla), ejercicioSimple(remo), ejercicioSimple(press)));
        Respuesta creada = pedir(post("/plantillas"), pedido);
        assertEquals(201, creada.status(), creada.cuerpo().toString());
        return creada.cuerpo();
    }

    // --- Pedidos ---

    private Respuesta pedir(MockHttpServletRequestBuilder pedido) throws Exception {
        return pedirComo("ENTRENADOR", pedido, null);
    }

    private Respuesta pedir(MockHttpServletRequestBuilder pedido, Object cuerpo) throws Exception {
        return pedirComo("ENTRENADOR", pedido, cuerpo);
    }

    private Respuesta pedirComo(String rol, MockHttpServletRequestBuilder pedido, Object cuerpo) throws Exception {
        pedido.with(user(rol.toLowerCase() + "@test.com").roles(rol));
        return ejecutar(pedido, cuerpo);
    }

    private Respuesta ejecutar(MockHttpServletRequestBuilder pedido, Object cuerpo) throws Exception {
        if (cuerpo != null) {
            pedido.contentType("application/json").content(cuerpo instanceof String texto ? texto : json.writeValueAsString(cuerpo));
        }
        MockHttpServletResponse respuesta = mockMvc.perform(pedido).andReturn().getResponse();
        String texto = respuesta.getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(respuesta.getStatus(), texto.isEmpty() ? json.createObjectNode() : json.readTree(texto));
    }

    private JsonNode asignar(Long alumnoId, Long plantillaId) throws Exception {
        Respuesta plan = pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumnoId, plantillaId));
        assertEquals(201, plan.status(), plan.cuerpo().toString());
        return plan.cuerpo();
    }

    private JsonNode plan(Long id) throws Exception {
        Respuesta plan = pedir(get("/planes-asignados/" + id));
        assertEquals(200, plan.status());
        return plan.cuerpo();
    }

    private JsonNode plantilla(Long id) throws Exception {
        Respuesta plantilla = pedir(get("/plantillas/" + id));
        assertEquals(200, plantilla.status());
        return plantilla.cuerpo();
    }

    private int asignacionesActivas(Long plantillaId) throws Exception {
        return plantilla(plantillaId).path("asignacionesActivas").asInt();
    }

    private int planesActivosDe(Long alumnoId) {
        return jdbc.queryForObject("select count(*) from planes_asignados where alumno_id = ? and estado = 'ACTIVO'",
                Integer.class, alumnoId);
    }

    private int planesDe(Long alumnoId) {
        return jdbc.queryForObject("select count(*) from planes_asignados where alumno_id = ?", Integer.class, alumnoId);
    }

    /** Lo que define a un plan o plantilla, sin ids: dos contenidos iguales son el mismo plan. */
    private static String contenido(JsonNode plan) {
        StringBuilder texto = new StringBuilder();
        texto.append(plan.path("nombre").asString()).append('|').append(plan.path("nivel").asString()).append('|')
                .append(plan.path("objetivo").asString());
        for (JsonNode sesion : plan.path("sesiones")) {
            texto.append("\n").append(sesion.path("numero").asInt()).append(':').append(sesion.path("nombre").toString());
            for (JsonNode e : sesion.path("ejercicios")) {
                texto.append("\n  ").append(e.path("ejercicioId").asLong()).append(' ').append(e.path("nombre").asString())
                        .append(' ').append(e.path("series").asInt()).append('x').append(e.path("repeticiones").asInt())
                        .append('-').append(e.path("repeticionesMax").toString())
                        .append(" descanso=").append(e.path("descansoMinutos").toString())
                        .append(" peso=").append(e.path("pesoSugerido").toString())
                        .append(" rir=").append(e.path("rirObjetivo").toString());
            }
        }
        return texto.toString();
    }

    private static Long id(JsonNode entidad) {
        return entidad.path("id").asLong();
    }

    /** Ejecuta las tareas lo más juntas posible y devuelve sus códigos de respuesta, en orden. */
    private List<Integer> enParalelo(List<Callable<Integer>> tareas) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(tareas.size());
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Callable<Integer> tarea : tareas) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                codigos.add(resultado.get());
            }
            return codigos;
        } finally {
            hilos.shutdownNow();
        }
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

    // --- Pruebas de aceptación de la HU ---

    @Test
    void pa1_asignar_una_plantilla_a_un_alumno_sin_plan_activo_crea_una_copia_igual() throws Exception {
        JsonNode original = plantillaCompleta();
        Long alumno = alumno();

        Respuesta creado = pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, id(original)));

        assertEquals(201, creado.status(), creado.cuerpo().toString());
        assertEquals("ACTIVO", creado.cuerpo().path("estado").asString());
        assertEquals(alumno, creado.cuerpo().path("alumnoId").asLong());
        assertEquals(id(original), creado.cuerpo().path("plantillaOrigenId").asLong());
        // Se compara lo que quedó guardado, no lo que había en memoria: nombre, nivel, objetivo, sesiones y ejercicios en orden
        assertEquals(contenido(plantilla(id(original))), contenido(plan(id(creado.cuerpo()))));
        // La copia tiene sus propias filas
        Long planId = id(creado.cuerpo());
        assertEquals(3, jdbc.queryForObject("select count(*) from sesiones_plan where plan_id = ?", Integer.class, planId));
        assertEquals(6, jdbc.queryForObject("select count(*) from ejercicios_sesion_plan e join sesiones_plan s "
                + "on e.sesion_plan_id = s.id where s.plan_id = ?", Integer.class, planId));
        assertEquals(1, asignacionesActivas(id(original)));
    }

    @Test
    void pa2_asignar_un_plan_armado_desde_cero_con_una_sesion_y_un_ejercicio_pasa() throws Exception {
        Long alumno = alumno();
        Long ejercicio = ejercicio();
        String nombre = "Plan propio " + SECUENCIA.incrementAndGet();

        Respuesta creado = pedir(post("/planes-asignados/desde-cero"),
                desdeCero(alumno, "  " + nombre + "  ", sesion("Full body", ejercicioDeSesion(ejercicio, 4, 8, 12, "1.5", "20", 1))));

        assertEquals(201, creado.status(), creado.cuerpo().toString());
        JsonNode guardado = plan(id(creado.cuerpo()));
        assertEquals(nombre, guardado.path("nombre").asString());
        assertEquals("INTERMEDIO", guardado.path("nivel").asString());
        assertEquals("HIPERTROFIA", guardado.path("objetivo").asString());
        assertTrue(guardado.path("plantillaOrigenId").isNull());
        JsonNode cargado = guardado.path("sesiones").get(0).path("ejercicios").get(0);
        assertEquals(ejercicio, cargado.path("ejercicioId").asLong());
        assertEquals(8, cargado.path("repeticiones").asInt());
        assertEquals(12, cargado.path("repeticionesMax").asInt());
        // No queda como plantilla reutilizable
        for (JsonNode plantilla : pedir(get("/plantillas")).cuerpo()) {
            assertNotEquals(nombre, plantilla.path("nombre").asString());
        }
    }

    @Test
    void pa3_asignar_un_plan_armado_desde_cero_sin_ninguna_sesion_falla() throws Exception {
        Long alumno = alumno();

        Respuesta vacio = pedir(post("/planes-asignados/desde-cero"), desdeCero(alumno, "Sin sesiones"));

        assertEquals(400, vacio.status());
        assertEquals("El plan tiene que tener al menos una sesión", vacio.error());
        assertEquals(0, planesDe(alumno));

        Map<String, Object> sinCampo = desdeCero(alumno, "Sin sesiones");
        sinCampo.remove("sesiones");
        assertEquals(400, pedir(post("/planes-asignados/desde-cero"), sinCampo).status());
        assertEquals(0, planesDe(alumno));
    }

    @Test
    void pa4_asignar_a_un_alumno_con_plan_activo_falla_y_pide_finalizar_el_actual() throws Exception {
        JsonNode original = plantillaCompleta();
        Long alumno = alumno();
        JsonNode actual = asignar(alumno, id(original));

        Respuesta otraPlantilla = pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, id(plantillaCompleta())));
        Respuesta otroDesdeCero = pedir(post("/planes-asignados/desde-cero"),
                desdeCero(alumno, "Otro", sesion(null, ejercicioSimple(ejercicio()))));

        assertEquals(409, otraPlantilla.status());
        assertEquals(409, otroDesdeCero.status());
        assertTrue(otraPlantilla.error().contains("primero finalizá su plan actual"), otraPlantilla.error());
        assertTrue(otraPlantilla.error().contains(actual.path("nombre").asString()), otraPlantilla.error());
        assertEquals(1, planesDe(alumno));
        assertEquals(1, asignacionesActivas(id(original)));

        // Finalizado el actual, ya se puede
        assertEquals(200, pedir(put("/planes-asignados/" + id(actual) + "/finalizar")).status());
        assertEquals(0, asignacionesActivas(id(original)));
        assertEquals(201, pedir(post("/planes-asignados/desde-cero"),
                desdeCero(alumno, "Otro", sesion(null, ejercicioSimple(ejercicio())))).status());
        assertEquals(1, planesActivosDe(alumno));
        assertEquals(2, planesDe(alumno));
    }

    @Test
    void pa5_modificar_la_plantilla_original_no_cambia_la_copia_del_alumno() throws Exception {
        JsonNode original = plantillaCompleta();
        Long plantillaId = id(original);
        Long planId = id(asignar(alumno(), plantillaId));
        String antes = contenido(plan(planId));

        Map<String, Object> cambios = datosPlan("Renombrada " + SECUENCIA.incrementAndGet(),
                sesion("Una sola", ejercicioDeSesion(ejercicio(), 20, 100, null, "10", "999.99", 10)));
        cambios.put("nivel", "AVANZADO");
        cambios.put("objetivo", "FUERZA");
        Respuesta modificada = pedir(put("/plantillas/" + plantillaId), cambios);

        assertEquals(200, modificada.status(), modificada.cuerpo().toString());
        assertEquals(1, plantilla(plantillaId).path("sesiones").size());
        assertEquals(antes, contenido(plan(planId)));
    }

    @Test
    void pa6_modificar_la_copia_del_alumno_no_cambia_la_plantilla_original() throws Exception {
        JsonNode original = plantillaCompleta();
        Long plantillaId = id(original);
        Long planId = id(asignar(alumno(), plantillaId));
        String antes = contenido(plantilla(plantillaId));

        Map<String, Object> cambios = datosPlan("Plan retocado",
                sesion("Una sola", ejercicioDeSesion(ejercicio(), 20, 100, null, "10", "999.99", 10)));
        cambios.put("nivel", "AVANZADO");
        cambios.put("objetivo", "FUERZA");
        Respuesta modificado = pedir(put("/planes-asignados/" + planId), cambios);

        assertEquals(200, modificado.status(), modificado.cuerpo().toString());
        JsonNode guardado = plan(planId);
        assertEquals("Plan retocado", guardado.path("nombre").asString());
        assertEquals("AVANZADO", guardado.path("nivel").asString());
        assertEquals(1, guardado.path("sesiones").size());
        assertEquals(antes, contenido(plantilla(plantillaId)));
    }

    @Test
    void la_plantilla_dada_de_baja_no_afecta_a_la_copia_que_ya_tiene_el_alumno() throws Exception {
        JsonNode original = plantillaCompleta();
        Long plantillaId = id(original);
        Long planId = id(asignar(alumno(), plantillaId));
        String antes = contenido(plan(planId));

        // Mientras esté asignada no se puede eliminar; finalizando el plan sí
        assertEquals(409, pedir(delete("/plantillas/" + plantillaId)).status());
        assertEquals(200, pedir(put("/planes-asignados/" + planId + "/finalizar")).status());
        assertEquals(204, pedir(delete("/plantillas/" + plantillaId)).status());

        assertEquals(antes, contenido(plan(planId)));
    }

    // --- La misma regla de validación que una plantilla ---

    @Test
    void plan_desde_cero_y_plantilla_rechazan_lo_mismo() throws Exception {
        Long ejercicio = ejercicio();
        Map<String, Map<String, Object>> invalidos = new HashMap<>();
        invalidos.put("sesión sin ejercicios", datosPlan("Inválido", sesion("Vacía")));
        invalidos.put("ejercicio inexistente", datosPlan("Inválido", sesion(null, ejercicioSimple(99_999_999L))));
        invalidos.put("ejercicio dado de baja", datosPlan("Inválido", sesion(null, ejercicioSimple(ejercicio(false)))));
        invalidos.put("nombre en blanco", datosPlan("   ", sesion(null, ejercicioSimple(ejercicio))));
        invalidos.put("nombre con espacios duros", datosPlan("  ", sesion(null, ejercicioSimple(ejercicio))));
        invalidos.put("nombre de 101 caracteres", datosPlan("x".repeat(101), sesion(null, ejercicioSimple(ejercicio))));
        invalidos.put("series en 0", datosPlan("Inválido", sesion(null, ejercicioDeSesion(ejercicio, 0, 10, null, null, null, null))));
        invalidos.put("21 series", datosPlan("Inválido", sesion(null, ejercicioDeSesion(ejercicio, 21, 10, null, null, null, null))));
        invalidos.put("rango invertido", datosPlan("Inválido", sesion(null, ejercicioDeSesion(ejercicio, 3, 10, 8, null, null, null))));
        invalidos.put("peso con 3 decimales", datosPlan("Inválido", sesion(null, ejercicioDeSesion(ejercicio, 3, 10, null, null, "1.234", null))));
        invalidos.put("RIR 11", datosPlan("Inválido", sesion(null, ejercicioDeSesion(ejercicio, 3, 10, null, null, null, 11))));
        Map<String, Object> sinNivel = datosPlan("Inválido", sesion(null, ejercicioSimple(ejercicio)));
        sinNivel.remove("nivel");
        invalidos.put("sin nivel", sinNivel);

        Long alumno = alumno();
        for (Map.Entry<String, Map<String, Object>> caso : invalidos.entrySet()) {
            Map<String, Object> plan = new HashMap<>(caso.getValue());
            plan.put("alumnoId", alumno);
            assertEquals(400, pedir(post("/planes-asignados/desde-cero"), plan).status(), "plan: " + caso.getKey());
            assertEquals(400, pedir(post("/plantillas"), caso.getValue()).status(), "plantilla: " + caso.getKey());
        }
        assertEquals(0, planesDe(alumno));
    }

    @Test
    void json_malformado_o_con_valores_que_no_existen_responde_400_y_no_500() throws Exception {
        Long alumno = alumno();
        Long ejercicio = ejercicio();
        String sesiones = "\"sesiones\":[{\"ejercicios\":[{\"ejercicioId\":" + ejercicio + ",\"series\":3,\"repeticiones\":10}]}]";
        String[] cuerpos = {
            "{",
            "{\"alumnoId\":" + alumno + ",\"nombre\":\"x\",\"nivel\":\"EXPERTO\",\"objetivo\":\"FUERZA\"," + sesiones + "}",
            "{\"alumnoId\":\"abc\",\"nombre\":\"x\",\"nivel\":\"AVANZADO\",\"objetivo\":\"FUERZA\"," + sesiones + "}",
            "{\"alumnoId\":" + alumno + ",\"nombre\":\"x\",\"nivel\":\"AVANZADO\",\"objetivo\":\"FUERZA\",\"sesiones\":[null]}",
            "{\"alumnoId\":" + alumno + ",\"nombre\":\"x\",\"nivel\":\"AVANZADO\",\"objetivo\":\"FUERZA\",\"sesiones\":[{\"ejercicios\":[null]}]}",
            "{\"alumnoId\":" + alumno + ",\"nombre\":\"x\",\"nivel\":\"AVANZADO\",\"objetivo\":\"FUERZA\",\"sesiones\":[{\"ejercicios\":[{\"ejercicioId\":"
                    + ejercicio + ",\"series\":3.5,\"repeticiones\":10}]}]}",
        };
        for (String cuerpo : cuerpos) {
            assertEquals(400, pedir(post("/planes-asignados/desde-cero"), cuerpo).status(), cuerpo);
        }
        assertEquals(0, planesDe(alumno));
    }

    // --- Casos de borde ---

    @Test
    void asignar_sobre_alumno_o_plantilla_que_no_existen_responde_404() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        Long alumno = alumno();

        assertEquals(404, pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(99_999_999L, plantillaId)).status());
        assertEquals(404, pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, 99_999_999L)).status());
        assertEquals(404, pedir(post("/planes-asignados/desde-cero"),
                desdeCero(99_999_999L, "Plan", sesion(null, ejercicioSimple(ejercicio())))).status());
        assertEquals(0, asignacionesActivas(plantillaId));
    }

    @Test
    void una_plantilla_dada_de_baja_no_se_puede_asignar() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        assertEquals(204, pedir(delete("/plantillas/" + plantillaId)).status());
        Long alumno = alumno();

        assertEquals(404, pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, plantillaId)).status());
        assertEquals(0, planesDe(alumno));
    }

    @Test
    void a_un_alumno_dado_de_baja_no_se_le_asigna_un_plan() throws Exception {
        Long alumno = alumno();
        assertEquals(200, pedirComo("PROPIETARIO", delete("/alumnos/" + alumno), null).status());

        Respuesta rechazado = pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, id(plantillaCompleta())));

        assertEquals(400, rechazado.status());
        assertEquals(0, planesDe(alumno));
    }

    @Test
    void dar_de_baja_al_alumno_finaliza_su_plan_activo_y_libera_la_plantilla() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        Long alumno = alumno();
        Long planId = id(asignar(alumno, plantillaId));
        Long planDeOtro = id(asignar(alumno(), plantillaId));

        assertEquals(200, pedirComo("PROPIETARIO", delete("/alumnos/" + alumno), null).status());

        JsonNode finalizado = plan(planId);
        assertEquals("FINALIZADO", finalizado.path("estado").asString());
        assertFalse(finalizado.path("fechaFinalizacion").isNull());
        assertEquals(204, pedir(get("/planes-asignados/activo").param("alumnoId", alumno.toString())).status());
        // Solo se libera su asignación: el plan del otro alumno sigue igual
        assertEquals(1, asignacionesActivas(plantillaId));
        assertEquals("ACTIVO", plan(planDeOtro).path("estado").asString());

        // Repetir la baja no vuelve a restar
        assertEquals(200, pedirComo("PROPIETARIO", delete("/alumnos/" + alumno), null).status());
        assertEquals(1, asignacionesActivas(plantillaId));
    }

    @Test
    void dar_de_baja_a_un_alumno_con_un_plan_desde_cero_lo_finaliza() throws Exception {
        Long alumno = alumno();
        Respuesta creado = pedir(post("/planes-asignados/desde-cero"), desdeCero(alumno, "Plan", sesion(null, ejercicioSimple(ejercicio()))));
        assertEquals(201, creado.status());

        assertEquals(200, pedirComo("PROPIETARIO", delete("/alumnos/" + alumno), null).status());

        assertEquals("FINALIZADO", plan(id(creado.cuerpo())).path("estado").asString());
    }

    @Test
    void dar_de_baja_a_un_alumno_sin_plan_activo_no_toca_sus_planes_finalizados() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        Long alumno = alumno();
        Long planId = id(asignar(alumno, plantillaId));
        asignar(alumno(), plantillaId);
        assertEquals(200, pedir(put("/planes-asignados/" + planId + "/finalizar")).status());
        assertEquals(1, asignacionesActivas(plantillaId));

        assertEquals(200, pedirComo("PROPIETARIO", delete("/alumnos/" + alumno), null).status());

        assertEquals(1, asignacionesActivas(plantillaId));
    }

    /** Bajas anteriores a esta regla: el plan quedó activo y se tiene que poder finalizar a mano. */
    @Test
    void el_plan_que_quedo_activo_de_una_baja_anterior_se_puede_finalizar() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        Long alumno = alumno();
        Long planId = id(asignar(alumno, plantillaId));
        jdbc.update("update cuentas set activa = false where id = (select cuenta_id from alumnos where id = ?)", alumno);

        assertEquals(200, pedir(put("/planes-asignados/" + planId + "/finalizar")).status());

        assertEquals(0, asignacionesActivas(plantillaId));
        assertEquals(204, pedir(delete("/plantillas/" + plantillaId)).status());
    }

    @Test
    void un_plan_finalizado_no_se_modifica_ni_se_vuelve_a_finalizar() throws Exception {
        Long planId = id(asignar(alumno(), id(plantillaCompleta())));
        assertEquals(200, pedir(put("/planes-asignados/" + planId + "/finalizar")).status());
        String antes = contenido(plan(planId));

        assertEquals(409, pedir(put("/planes-asignados/" + planId + "/finalizar")).status());
        assertEquals(409, pedir(put("/planes-asignados/" + planId),
                datosPlan("Cambio", sesion(null, ejercicioSimple(ejercicio())))).status());
        assertEquals(antes, contenido(plan(planId)));
        assertFalse(plan(planId).path("fechaFinalizacion").isNull());
    }

    @Test
    void modificar_varias_veces_el_plan_reemplaza_las_sesiones_sin_dejar_filas_viejas() throws Exception {
        Long planId = id(asignar(alumno(), id(plantillaCompleta())));
        Long ejercicio = ejercicio();

        for (int vuelta = 1; vuelta <= 3; vuelta++) {
            Respuesta modificado = pedir(put("/planes-asignados/" + planId), datosPlan("Vuelta " + vuelta,
                    sesion("A", ejercicioSimple(ejercicio), ejercicioSimple(ejercicio)), sesion("B", ejercicioSimple(ejercicio))));
            assertEquals(200, modificado.status(), modificado.cuerpo().toString());
            // Las sesiones e ids que devuelve la respuesta son los que quedaron guardados
            assertEquals(contenido(modificado.cuerpo()), contenido(plan(planId)));
            assertEquals(2, jdbc.queryForObject("select count(*) from sesiones_plan where plan_id = ?", Integer.class, planId));
            assertEquals(3, jdbc.queryForObject("select count(*) from ejercicios_sesion_plan e join sesiones_plan s "
                    + "on e.sesion_plan_id = s.id where s.plan_id = ?", Integer.class, planId));
        }
    }

    @Test
    void un_ejercicio_dado_de_baja_despues_sigue_en_la_copia_y_se_puede_conservar_al_modificar() throws Exception {
        Long propio = ejercicio();
        Respuesta creada = pedir(post("/plantillas"), datosPlan("Plantilla " + SECUENCIA.incrementAndGet(), sesion(null, ejercicioSimple(propio))));
        assertEquals(201, creada.status());
        Long planId = id(asignar(alumno(), id(creada.cuerpo())));

        assertEquals(200, pedir(patch("/ejercicios/" + propio + "/estado")).status());

        assertFalse(plan(planId).path("sesiones").get(0).path("ejercicios").get(0).path("ejercicioActivo").asBoolean());
        assertEquals(200, pedir(put("/planes-asignados/" + planId), datosPlan("Conserva", sesion(null, ejercicioSimple(propio)))).status());
        // Pero uno de baja que no tenía no se puede agregar
        assertEquals(400, pedir(put("/planes-asignados/" + planId), datosPlan("Agrega", sesion(null, ejercicioSimple(ejercicio(false))))).status());
    }

    @Test
    void no_se_puede_borrar_del_catalogo_un_ejercicio_que_se_usa_en_un_plan() throws Exception {
        Long ejercicio = ejercicio();
        assertEquals(201, pedir(post("/planes-asignados/desde-cero"), desdeCero(alumno(), "Plan", sesion(null, ejercicioSimple(ejercicio)))).status());

        Respuesta borrado = pedir(delete("/ejercicios/" + ejercicio));

        assertEquals(400, borrado.status(), borrado.cuerpo().toString());
        assertTrue(borrado.error().contains("planes de entrenamiento de alumnos"), borrado.error());
    }

    @Test
    void consultar_el_plan_activo_responde_200_204_o_404_segun_corresponda() throws Exception {
        Long alumno = alumno();
        assertEquals(204, pedir(get("/planes-asignados/activo").param("alumnoId", alumno.toString())).status());
        assertEquals(404, pedir(get("/planes-asignados/activo").param("alumnoId", "99999999")).status());
        assertEquals(400, pedir(get("/planes-asignados/activo").param("alumnoId", "abc")).status());
        assertEquals(400, pedir(get("/planes-asignados/activo")).status());

        JsonNode asignado = asignar(alumno, id(plantillaCompleta()));
        Respuesta activo = pedir(get("/planes-asignados/activo").param("alumnoId", alumno.toString()));

        assertEquals(200, activo.status());
        assertEquals(id(asignado), id(activo.cuerpo()));
    }

    @Test
    void el_listado_de_alumnos_informa_y_filtra_por_plan_activo() throws Exception {
        Long conPlan = alumno();
        Long sinPlan = alumno();
        JsonNode asignado = asignar(conPlan, id(plantillaCompleta()));

        JsonNode conActivo = pedir(get("/alumnos").param("estadoPlan", "ACTIVO")).cuerpo();
        JsonNode sinActivo = pedir(get("/alumnos").param("estadoPlan", "SIN_PLAN")).cuerpo();

        List<Long> idsConActivo = new ArrayList<>();
        for (JsonNode alumno : conActivo) {
            idsConActivo.add(alumno.path("id").asLong());
            if (alumno.path("id").asLong() == conPlan) {
                assertEquals(asignado.path("nombre").asString(), alumno.path("planActivo").path("nombre").asString());
            }
        }
        assertTrue(idsConActivo.contains(conPlan));
        assertFalse(idsConActivo.contains(sinPlan));
        List<Long> idsSinActivo = new ArrayList<>();
        sinActivo.forEach(alumno -> idsSinActivo.add(alumno.path("id").asLong()));
        assertTrue(idsSinActivo.contains(sinPlan));
        assertFalse(idsSinActivo.contains(conPlan));
    }

    // --- Permisos, por el filtro de seguridad real ---

    @Test
    void solo_entrenador_y_propietario_operan_los_planes() throws Exception {
        Long alumno = alumno();
        Long plantillaId = id(plantillaCompleta());
        Long planId = id(asignar(alumno(), plantillaId));
        Map<String, Object> asignacion = desdePlantilla(alumno, plantillaId);

        List<Object[]> operaciones = List.of(
                new Object[] {get("/planes-asignados/" + planId), null},
                new Object[] {get("/planes-asignados/activo").param("alumnoId", alumno.toString()), null},
                new Object[] {post("/planes-asignados/desde-plantilla"), asignacion},
                new Object[] {post("/planes-asignados/desde-cero"), Map.of()},
                new Object[] {put("/planes-asignados/" + planId), Map.of()},
                new Object[] {put("/planes-asignados/" + planId + "/finalizar"), null});
        for (Object[] operacion : operaciones) {
            MockHttpServletRequestBuilder pedido = (MockHttpServletRequestBuilder) operacion[0];
            assertEquals(401, ejecutar(pedido, operacion[1]).status(), "sin sesión: " + pedido);
            assertEquals(403, pedirComo("ALUMNO", pedido, operacion[1]).status(), "alumno");
            assertEquals(403, pedirComo("NUTRICIONISTA", pedido, operacion[1]).status(), "nutricionista");
        }
        assertEquals(0, planesDe(alumno));
        assertEquals("ACTIVO", plan(planId).path("estado").asString());

        Respuesta comoPropietario = pedirComo("PROPIETARIO", post("/planes-asignados/desde-plantilla"), asignacion);
        assertEquals(201, comoPropietario.status());
    }

    // --- Concurrencia, con transacciones reales ---

    @Test
    void dos_asignaciones_simultaneas_al_mismo_alumno_dejan_un_solo_plan_activo() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        Long ejercicio = ejercicio();
        for (int vuelta = 0; vuelta < 10; vuelta++) {
            Long alumno = alumno();
            int inicial = asignacionesActivas(plantillaId);
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tareas.add(() -> pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, plantillaId)).status());
                tareas.add(() -> pedir(post("/planes-asignados/desde-cero"),
                        desdeCero(alumno, "Plan", sesion(null, ejercicioSimple(ejercicio)))).status());
            }

            List<Integer> codigos = enParalelo(tareas);

            assertEquals(1, Collections.frequency(codigos, 201), codigos.toString());
            assertEquals(7, Collections.frequency(codigos, 409), codigos.toString());
            assertEquals(1, planesActivosDe(alumno));
            assertEquals(1, planesDe(alumno));
            // Si ganó uno desde cero la plantilla no suma; si ganó uno desde la plantilla, suma uno
            int esperado = inicial + jdbc.queryForObject("select count(*) from planes_asignados where alumno_id = ? "
                    + "and plantilla_origen_id is not null", Integer.class, alumno);
            assertEquals(esperado, asignacionesActivas(plantillaId));
        }
    }

    @Test
    void asignaciones_simultaneas_de_una_plantilla_a_varios_alumnos_no_pierden_la_cuenta() throws Exception {
        Long plantillaId = id(plantillaCompleta());
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Long alumno = alumno();
            tareas.add(() -> pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, plantillaId)).status());
        }

        List<Integer> codigos = enParalelo(tareas);

        assertEquals(8, Collections.frequency(codigos, 201), codigos.toString());
        assertEquals(8, asignacionesActivas(plantillaId));
    }

    @Disabled("Defecto conocido: finalizarPlan no bloquea el plan, así que dos pedidos simultáneos restan dos asignaciones")
    @Test
    void finalizar_dos_veces_a_la_vez_el_mismo_plan_resta_una_sola_asignacion() throws Exception {
        for (int vuelta = 0; vuelta < 15; vuelta++) {
            Long plantillaId = id(plantillaCompleta());
            Long planId = id(asignar(alumno(), plantillaId));
            asignar(alumno(), plantillaId);

            List<Integer> codigos = enParalelo(List.of(
                    () -> pedir(put("/planes-asignados/" + planId + "/finalizar")).status(),
                    () -> pedir(put("/planes-asignados/" + planId + "/finalizar")).status()));

            assertEquals(1, asignacionesActivas(plantillaId), "vuelta " + vuelta + ": " + codigos);
        }
    }

    @Disabled("Defecto conocido: actualizarPlantilla guarda asignacionesActivas con el valor que leyó y pisa la suma de la asignación")
    @Test
    void modificar_la_plantilla_mientras_se_asigna_no_pierde_la_asignacion() throws Exception {
        for (int vuelta = 0; vuelta < 15; vuelta++) {
            JsonNode original = plantillaCompleta();
            Long plantillaId = id(original);
            Long alumno = alumno();
            Long ejercicio = ejercicio();
            String nombre = original.path("nombre").asString();

            List<Integer> codigos = enParalelo(List.of(
                    () -> pedir(put("/plantillas/" + plantillaId), datosPlan(nombre, sesion(null, ejercicioSimple(ejercicio)))).status(),
                    () -> pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, plantillaId)).status()));

            assertEquals(List.of(200, 201), codigos);
            assertEquals(1, asignacionesActivas(plantillaId), "vuelta " + vuelta);
        }
    }

    @Disabled("Defecto conocido: eliminarPlantilla no se sincroniza con la asignación; ambas pueden terminar bien a la vez")
    @Test
    void eliminar_la_plantilla_mientras_se_asigna_no_deja_un_plan_activo_de_una_plantilla_eliminada() throws Exception {
        for (int vuelta = 0; vuelta < 15; vuelta++) {
            Long plantillaId = id(plantillaCompleta());
            Long alumno = alumno();

            List<Integer> codigos = enParalelo(List.of(
                    () -> pedir(delete("/plantillas/" + plantillaId)).status(),
                    () -> pedir(post("/planes-asignados/desde-plantilla"), desdePlantilla(alumno, plantillaId)).status()));

            boolean eliminada = codigos.get(0) == 204;
            boolean asignada = codigos.get(1) == 201;
            assertFalse(eliminada && asignada, "vuelta " + vuelta + ": " + codigos);
        }
    }
}
