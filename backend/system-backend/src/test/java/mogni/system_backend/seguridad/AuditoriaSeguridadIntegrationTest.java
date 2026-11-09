package mogni.system_backend.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.security.JwtService;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.TurnoRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Auditoría de seguridad: control de acceso, autenticación, validación y concurrencia contra el servidor completo
 * (filtros, JWT reales, JPA) y un PostgreSQL embebido, nunca contra una base compartida.
 * Las pruebas marcadas con @Disabled("HALLAZGO ...") describen el comportamiento esperado que hoy NO se cumple: se
 * activan cuando se corrija.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaSeguridadIntegrationTest {

    private static final String SECRETO = "clave-solo-para-las-pruebas-automatizadas-de-mogni";
    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final AtomicInteger SECUENCIA = new AtomicInteger(5000);

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
        registry.add("jwt.secret", () -> SECRETO);
        registry.add("jwt.expiration-ms", () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @LocalServerPort
    private int puerto;

    private final ObjectMapper json = JsonMapper.builder().build();

    private record Respuesta(int status, JsonNode cuerpo, String texto) {
    }

    // --- Datos de prueba ---

    private Cuenta cuenta(Rol rol, String password, boolean debeCambiar) {
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(rol.name().toLowerCase() + SECUENCIA.incrementAndGet() + "@test.com");
        cuenta.setPassword(passwordEncoder.encode(password));
        cuenta.setRol(rol);
        cuenta.setDebeCambiarPassword(debeCambiar);
        cuenta.setActiva(true);
        return cuentaRepository.save(cuenta);
    }

    private String token(Cuenta cuenta) {
        return jwtService.generarToken(cuenta);
    }

    private String tokenDe(Rol rol) {
        return token(cuenta(rol, "clave-segura-1", false));
    }

    private long membresia(String precio, int dias) throws Exception {
        Respuesta r = ejecutar(post("/membresias"), tokenDe(Rol.PROPIETARIO),
                "{\"nombre\":\"Plan " + SECUENCIA.incrementAndGet() + "\",\"precioActual\":" + precio
                        + ",\"frecuenciaSemanal\":" + dias + "}");
        assertEquals(201, r.status(), r.texto());
        return r.cuerpo().path("id").asLong();
    }

    private String cuerpoAlumno(int n, long membresiaId, String extra) {
        return "{\"nombre\":\"Ana\",\"apellido\":\"Prueba" + n + "\",\"dni\":\"" + (30_000_000 + n) + "\","
                + "\"email\":\"alumno" + n + "@test.com\",\"fechaNacimiento\":\"1995-05-05\",\"telefono\":\"351 555 0000\","
                + "\"sexo\":\"F\",\"membresiaId\":" + membresiaId + ",\"medioDePago\":\"EFECTIVO\","
                + "\"objetivo\":\"Bajar de peso\",\"cuestionarioSalud\":\"Asma\"" + extra + "}";
    }

    /** Alumno inscripto por el propietario (con la primera cuota paga), ya con la contraseña cambiada. */
    private Alta alumno(long membresiaId) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        Respuesta r = ejecutar(post("/alumnos"), tokenDe(Rol.PROPIETARIO), cuerpoAlumno(n, membresiaId, ""));
        assertEquals(201, r.status(), r.texto());
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase("alumno" + n + "@test.com").orElseThrow();
        cuenta.setDebeCambiarPassword(false);
        cuentaRepository.save(cuenta);
        return new Alta(r.cuerpo().path("id").asLong(), cuenta, token(cuenta));
    }

    private record Alta(long id, Cuenta cuenta, String token) {
    }

    private Turno turnoDeManiana(LocalTime hora, int cupo) {
        LocalDate maniana = ZonaHoraria.hoy().plusDays(1);
        return turnoRepository.save(new Turno(null, maniana, hora, hora.plusHours(1), cupo, EstadoTurno.DISPONIBLE));
    }

    // --- Pedidos ---

    private Respuesta ejecutar(MockHttpServletRequestBuilder pedido, String token) throws Exception {
        return ejecutar(pedido, token, null);
    }

    private Respuesta ejecutar(MockHttpServletRequestBuilder pedido, String token, String cuerpo) throws Exception {
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        MockHttpServletResponse respuesta = mockMvc.perform(pedido).andReturn().getResponse();
        String texto = respuesta.getContentAsString(StandardCharsets.UTF_8);
        JsonNode nodo;
        try {
            nodo = texto.isEmpty() ? json.createObjectNode() : json.readTree(texto);
        } catch (RuntimeException e) {
            nodo = json.createObjectNode();
        }
        return new Respuesta(respuesta.getStatus(), nodo, texto);
    }

    private static <T> List<T> aLaVez(Callable<T> uno, Callable<T> otro) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<T> a = hilos.submit(() -> { largada.await(); return uno.call(); });
            Future<T> b = hilos.submit(() -> { largada.await(); return otro.call(); });
            largada.countDown();
            return List.of(a.get(), b.get());
        } finally {
            hilos.shutdown();
        }
    }

    // ===================================================================================================
    // 1. Control de acceso
    // ===================================================================================================

    /** Endpoints que solo puede usar el propietario (o propietario y entrenador). */
    private static final List<String[]> DE_GESTION = List.of(
            new String[] {"POST", "/pagos"},
            new String[] {"GET", "/alumnos/1/pagos"},
            new String[] {"GET", "/alumnos/1/renovacion"},
            new String[] {"GET", "/franjas"},
            new String[] {"PUT", "/configuracion-gimnasio/cupo-maximo"},
            new String[] {"POST", "/agenda/cancelar-dia"},
            new String[] {"GET", "/agenda/dia"},
            new String[] {"PUT", "/agenda/reservas/1/asistencia"},
            new String[] {"PUT", "/turnos/1/revertir-cancelacion"},
            new String[] {"GET", "/plantillas"},
            new String[] {"POST", "/plantillas"},
            new String[] {"POST", "/planes-asignados/desde-cero"},
            new String[] {"PUT", "/planes-asignados/1/finalizar"},
            new String[] {"GET", "/alumnos/1/progreso/planes"});

    private MockHttpServletRequestBuilder pedido(String metodo, String ruta) {
        return switch (metodo) {
            case "GET" -> get(ruta);
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            default -> delete(ruta);
        };
    }

    @Test
    void alumno_y_nutricionista_reciben_403_en_los_endpoints_del_propietario_y_del_entrenador() throws Exception {
        for (Rol rol : List.of(Rol.ALUMNO, Rol.NUTRICIONISTA)) {
            String token = tokenDe(rol);
            for (String[] e : DE_GESTION) {
                assertEquals(403, ejecutar(pedido(e[0], e[1]), token, "{}").status(), rol + " " + e[0] + " " + e[1]);
            }
        }
    }

    @Test
    void el_alumno_recibe_403_en_toda_la_gestion_de_alumnos_membresias_y_ejercicios() throws Exception {
        String alumno = tokenDe(Rol.ALUMNO);
        for (String[] e : List.of(new String[] {"GET", "/alumnos"}, new String[] {"POST", "/alumnos"},
                new String[] {"GET", "/alumnos/1"}, new String[] {"PUT", "/alumnos/1"}, new String[] {"DELETE", "/alumnos/1"},
                new String[] {"POST", "/membresias"}, new String[] {"PUT", "/membresias/1"},
                new String[] {"GET", "/ejercicios"}, new String[] {"POST", "/ejercicios"})) {
            assertEquals(403, ejecutar(pedido(e[0], e[1]), alumno, "{}").status(), e[0] + " " + e[1]);
        }
    }

    @Test
    void el_personal_recibe_403_en_los_endpoints_del_alumno() throws Exception {
        for (Rol rol : List.of(Rol.PROPIETARIO, Rol.ENTRENADOR, Rol.NUTRICIONISTA)) {
            String token = tokenDe(rol);
            for (String ruta : List.of("/alumnos/me", "/mi-progreso/planes", "/turnos/mis-turnos", "/notificaciones")) {
                assertEquals(403, ejecutar(get(ruta), token).status(), rol + " " + ruta);
            }
        }
    }

    /**
     * HALLAZGO (a confirmar con el negocio): /alumnos/** y /membresias/** están abiertos a todo el personal, así que
     * el NUTRICIONISTA y el ENTRENADOR pueden inscribir alumnos (lo que registra el pago de la primera cuota), darlos
     * de baja y crear o cambiar el precio de las membresías.
     */
    @Test
    @Disabled("HALLAZGO M-1: nutricionista y entrenador pueden inscribir/dar de baja alumnos y modificar membresías")
    void solo_el_propietario_inscribe_da_de_baja_alumnos_y_administra_membresias() throws Exception {
        long membresiaId = membresia("20000", 3);
        for (Rol rol : List.of(Rol.NUTRICIONISTA, Rol.ENTRENADOR)) {
            String token = tokenDe(rol);
            int n = SECUENCIA.incrementAndGet();
            assertEquals(403, ejecutar(post("/alumnos"), token, cuerpoAlumno(n, membresiaId, "")).status(), rol + " inscribe");
            assertEquals(403, ejecutar(post("/membresias"), token,
                    "{\"nombre\":\"X" + n + "\",\"precioActual\":1,\"frecuenciaSemanal\":3}").status(), rol + " crea membresía");
            assertEquals(403, ejecutar(delete("/alumnos/" + alumno(membresiaId).id()), token).status(), rol + " da de baja");
        }
    }

    @Test
    void un_alumno_no_puede_cancelar_la_reserva_de_otro() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta duenio = alumno(membresiaId);
        Alta otro = alumno(membresiaId);
        Turno turno = turnoDeManiana(LocalTime.of(18, 0), 10);
        Respuesta reserva = ejecutar(post("/turnos/" + turno.getId() + "/reservar"), duenio.token());
        assertEquals(201, reserva.status(), reserva.texto());
        long reservaId = reserva.cuerpo().path("id").asLong();

        Respuesta intento = ejecutar(post("/turnos/reservas/" + reservaId + "/cancelar"), otro.token());

        assertEquals(403, intento.status(), intento.texto());
        assertEquals(1, reservasDelTurno(turno.getId()));
    }

    @Test
    void un_alumno_no_ve_ni_registra_sesiones_del_plan_de_otro_ni_sus_datos_de_perfil() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta duenio = alumno(membresiaId);
        Alta otro = alumno(membresiaId);
        // El plan del dueño, asignado por el entrenador
        String entrenador = tokenDe(Rol.ENTRENADOR);
        Respuesta ejercicio = ejecutar(post("/ejercicios"), entrenador,
                "{\"nombre\":\"Sentadilla " + SECUENCIA.incrementAndGet() + "\",\"grupoMuscular\":\"Piernas\",\"activo\":true}");
        assertEquals(201, ejercicio.status(), ejercicio.texto());
        Respuesta plan = ejecutar(post("/planes-asignados/desde-cero"), entrenador,
                "{\"alumnoId\":" + duenio.id() + ",\"nombre\":\"Plan\",\"nivel\":\"PRINCIPIANTE\",\"objetivo\":\"HIPERTROFIA\","
                        + "\"cantidadSemanas\":4,\"sesiones\":[{\"ejercicios\":[{\"ejercicioId\":" + ejercicio.cuerpo().path("id").asLong()
                        + ",\"series\":3,\"repeticiones\":10}]}]}");
        assertEquals(201, plan.status(), plan.texto());
        long planId = plan.cuerpo().path("id").asLong();
        Respuesta iniciada = ejecutar(post("/mi-progreso/planes/" + planId + "/sesiones"), duenio.token(),
                "{\"semana\":1,\"numeroSesion\":1}");
        assertEquals(200, iniciada.status(), iniciada.texto());
        long registroId = iniciada.cuerpo().path("registroId").asLong();

        assertEquals(404, ejecutar(get("/mi-progreso/planes/" + planId), otro.token()).status());
        assertEquals(404, ejecutar(post("/mi-progreso/planes/" + planId + "/sesiones"), otro.token(),
                "{\"semana\":1,\"numeroSesion\":1}").status());
        assertEquals(404, ejecutar(put("/mi-progreso/sesiones/" + registroId + "/ejercicios/1"), otro.token(),
                "{\"series\":[{\"orden\":1,\"peso\":10,\"repeticiones\":10,\"rir\":2},{\"orden\":2,\"peso\":10,\"repeticiones\":10,\"rir\":2},"
                        + "{\"orden\":3,\"peso\":10,\"repeticiones\":10,\"rir\":2}]}").status());
        assertEquals(404, ejecutar(post("/mi-progreso/sesiones/" + registroId + "/completar"), otro.token()).status());
        // Perfil, pagos y progreso del otro por la vía del personal: 403
        assertEquals(403, ejecutar(get("/alumnos/" + duenio.id()), otro.token()).status());
        assertEquals(403, ejecutar(get("/alumnos/" + duenio.id() + "/pagos"), otro.token()).status());
        assertEquals(403, ejecutar(get("/alumnos/" + duenio.id() + "/progreso/planes"), otro.token()).status());
        // /alumnos/me devuelve los datos de salud del propio alumno, no de otro
        Respuesta me = ejecutar(get("/alumnos/me"), otro.token());
        assertEquals(otro.id(), me.cuerpo().path("id").asLong());
    }

    @Test
    void desuscribir_push_con_el_endpoint_de_otro_no_borra_su_suscripcion() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta duenio = alumno(membresiaId);
        Alta otro = alumno(membresiaId);
        String endpoint = "https://push.example/" + SECUENCIA.incrementAndGet();
        Respuesta suscripcion = ejecutar(post("/notificaciones/push/suscripcion"), duenio.token(),
                "{\"endpoint\":\"" + endpoint + "\",\"p256dh\":\"clave\",\"auth\":\"auth\"}");
        assertEquals(2, suscripcion.status() / 100, suscripcion.texto());

        ejecutar(delete("/notificaciones/push/suscripcion"), otro.token(), "{\"endpoint\":\"" + endpoint + "\"}");

        Integer quedan = new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from suscripciones_push where endpoint = ?", Integer.class, endpoint);
        assertEquals(1, quedan);
    }

    @Test
    void el_personal_ve_objetivo_y_antecedentes_y_un_alumno_solo_los_suyos() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta alumno = alumno(membresiaId);

        assertEquals("Asma", ejecutar(get("/alumnos/me"), alumno.token()).cuerpo().path("cuestionarioSalud").asString());
        for (Rol rol : List.of(Rol.PROPIETARIO, Rol.ENTRENADOR, Rol.NUTRICIONISTA)) {
            Respuesta r = ejecutar(get("/alumnos/" + alumno.id()), tokenDe(rol));
            assertEquals(200, r.status(), rol.name());
            assertEquals("Asma", r.cuerpo().path("cuestionarioSalud").asString(), rol.name());
        }
    }

    @Test
    void un_alumno_dado_de_baja_no_puede_seguir_usando_su_token() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta alumno = alumno(membresiaId);
        assertEquals(200, ejecutar(delete("/alumnos/" + alumno.id()), tokenDe(Rol.PROPIETARIO)).status());

        for (String ruta : List.of("/mi-progreso/planes", "/turnos/mis-turnos", "/turnos/disponibles")) {
            int status = ejecutar(get(ruta), alumno.token()).status();
            assertTrue(status == 401 || status == 403, ruta + " respondió " + status);
        }
    }

    @Test
    void un_alumno_dado_de_baja_no_accede_a_ningun_endpoint_con_su_token_viejo() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta alumno = alumno(membresiaId);
        ejecutar(delete("/alumnos/" + alumno.id()), tokenDe(Rol.PROPIETARIO));

        for (String ruta : List.of("/alumnos/me", "/notificaciones")) {
            int status = ejecutar(get(ruta), alumno.token()).status();
            assertTrue(status == 401 || status == 403, ruta + " respondió " + status);
        }
    }

    @Test
    void un_miembro_del_personal_desactivado_no_puede_usar_su_token() throws Exception {
        Cuenta entrenador = cuenta(Rol.ENTRENADOR, "clave-segura-1", false);
        String token = token(entrenador);
        entrenador.setActiva(false);
        cuentaRepository.save(entrenador);

        assertEquals(401, ejecutar(get("/alumnos"), token).status());
    }

    @Test
    void con_el_primer_inicio_pendiente_solo_puede_cambiar_la_contrasenia() throws Exception {
        for (Rol rol : List.of(Rol.ALUMNO, Rol.PROPIETARIO)) {
            Cuenta cuenta = cuenta(rol, "clave-inicial", true);
            String token = token(cuenta);
            for (String ruta : List.of("/alumnos", "/alumnos/me", "/mi-progreso/planes", "/turnos/disponibles", "/alumnos/1/pagos")) {
                assertEquals(403, ejecutar(get(ruta), token).status(), rol + " " + ruta);
            }
            Respuesta cambio = ejecutar(post("/auth/cambiar-password"), token,
                    "{\"passwordActual\":\"clave-inicial\",\"passwordNueva\":\"otra-clave-1\",\"passwordConfirmacion\":\"otra-clave-1\"}");
            assertEquals(200, cambio.status(), cambio.texto());
        }
    }

    // ===================================================================================================
    // 2. Autenticación y sesión
    // ===================================================================================================

    private Respuesta login(String email, String password) throws Exception {
        return ejecutar(post("/auth/login"), null, "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    @Test
    void las_contrasenias_se_guardan_con_bcrypt() throws Exception {
        Cuenta cuenta = cuenta(Rol.ENTRENADOR, "clave-inicial", false);
        ejecutar(post("/auth/cambiar-password"), token(cuenta),
                "{\"passwordActual\":\"clave-inicial\",\"passwordNueva\":\"nueva-clave-9\",\"passwordConfirmacion\":\"nueva-clave-9\"}");

        String guardada = new JdbcTemplate(dataSource).queryForObject(
                "select password from cuentas where id = ?", String.class, cuenta.getId());
        assertTrue(guardada.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$"), "no es un hash BCrypt");
        assertFalse(guardada.contains("nueva-clave-9"));
    }

    @Test
    void el_login_responde_igual_con_email_inexistente_o_contrasenia_incorrecta() throws Exception {
        Cuenta cuenta = cuenta(Rol.ENTRENADOR, "clave-correcta", false);

        Respuesta inexistente = login("no-existe-" + SECUENCIA.incrementAndGet() + "@test.com", "lo-que-sea");
        Respuesta incorrecta = login(cuenta.getEmail(), "clave-incorrecta");

        assertEquals(401, inexistente.status());
        assertEquals(inexistente.status(), incorrecta.status());
        assertEquals(inexistente.texto(), incorrecta.texto());
    }

    @Test
    void el_login_de_una_cuenta_desactivada_no_revela_que_existe_sin_la_contrasenia_correcta() throws Exception {
        Cuenta cuenta = cuenta(Rol.ALUMNO, "clave-correcta", false);
        cuenta.setActiva(false);
        cuentaRepository.save(cuenta);

        Respuesta inexistente = login("no-existe-" + SECUENCIA.incrementAndGet() + "@test.com", "lo-que-sea");
        Respuesta desactivada = login(cuenta.getEmail(), "clave-incorrecta");

        assertEquals(401, desactivada.status());
        assertEquals(inexistente.texto(), desactivada.texto());
        // Con la contraseña correcta, el dueño sí ve por qué no puede entrar (y no recibe token)
        Respuesta conLaCorrecta = login(cuenta.getEmail(), "clave-correcta");
        assertEquals(401, conLaCorrecta.status());
        assertTrue(conLaCorrecta.texto().contains("deshabilitada"), conLaCorrecta.texto());
        assertFalse(conLaCorrecta.texto().contains("token"));
    }

    @Test
    @Disabled("HALLAZGO A-3: no hay límite de intentos de login (fuerza bruta)")
    void despues_de_muchos_intentos_fallidos_el_login_se_frena() throws Exception {
        Cuenta cuenta = cuenta(Rol.PROPIETARIO, "clave-correcta", false);
        for (int i = 0; i < 20; i++) {
            login(cuenta.getEmail(), "intento-" + i);
        }
        Respuesta siguiente = login(cuenta.getEmail(), "otro-intento");
        assertTrue(siguiente.status() == 429 || siguiente.status() == 423, "respondió " + siguiente.status());
    }

    @Test
    void un_token_valido_funciona_y_uno_adulterado_vencido_sin_firma_o_de_otra_clave_no() throws Exception {
        Cuenta cuenta = cuenta(Rol.PROPIETARIO, "clave-segura-1", false);
        String valido = token(cuenta);
        assertEquals(200, ejecutar(get("/membresias"), valido).status());

        String[] partes = valido.split("\\.");
        // Payload cambiado (otro rol/email) sin volver a firmar
        String payloadAdulterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + cuenta.getEmail() + "\",\"rol\":\"PROPIETARIO\",\"exp\":9999999999}").getBytes(StandardCharsets.UTF_8));
        String adulterado = partes[0] + "." + payloadAdulterado + "." + partes[2];
        // alg "none": sin firma
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String sinFirma = header + "." + payloadAdulterado + ".";
        var clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        String vencido = Jwts.builder().subject(cuenta.getEmail()).issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000)).signWith(clave).compact();
        String otraClave = Jwts.builder().subject(cuenta.getEmail()).expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor("otra-clave-cualquiera-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8))).compact();

        for (String malo : List.of(adulterado, sinFirma, vencido, otraClave, "basura")) {
            assertEquals(401, ejecutar(get("/membresias"), malo).status(), malo);
        }
    }

    @Test
    @Disabled("HALLAZGO M-2: el token sigue sirviendo después de cerrar sesión y de cambiar la contraseña")
    void despues_de_cerrar_sesion_o_cambiar_la_contrasenia_el_token_viejo_no_sirve() throws Exception {
        Cuenta cuenta = cuenta(Rol.ENTRENADOR, "clave-inicial", false);
        String viejo = token(cuenta);
        Thread.sleep(1100); // el token nuevo tiene que tener otro "iat"

        ejecutar(post("/auth/logout"), viejo);
        ejecutar(post("/auth/cambiar-password"), viejo,
                "{\"passwordActual\":\"clave-inicial\",\"passwordNueva\":\"nueva-clave-9\",\"passwordConfirmacion\":\"nueva-clave-9\"}");

        assertEquals(401, ejecutar(get("/alumnos"), viejo).status());
    }

    // ===================================================================================================
    // 3. Validación y asignación masiva
    // ===================================================================================================

    @Test
    void los_dtos_rechazan_valores_fuera_de_rango_y_formatos_invalidos() throws Exception {
        String propietario = tokenDe(Rol.PROPIETARIO);
        // Membresía: precio > 0, días <= 5
        assertEquals(400, ejecutar(post("/membresias"), propietario, "{\"nombre\":\"A\",\"precioActual\":0,\"frecuenciaSemanal\":3}").status());
        assertEquals(400, ejecutar(post("/membresias"), propietario, "{\"nombre\":\"A\",\"precioActual\":10,\"frecuenciaSemanal\":6}").status());
        // Cupo > 0
        assertEquals(400, ejecutar(put("/configuracion-gimnasio/cupo-maximo"), propietario, "{\"cupoMaximo\":0}").status());
        // Semanas 1-52
        assertEquals(400, ejecutar(post("/planes-asignados/desde-plantilla"), propietario,
                "{\"alumnoId\":1,\"plantillaId\":1,\"cantidadSemanas\":53}").status());
        // Email
        long membresiaId = membresia("20000", 3);
        assertEquals(400, ejecutar(post("/alumnos"), propietario,
                cuerpoAlumno(SECUENCIA.incrementAndGet(), membresiaId, "").replaceFirst("@test.com", "sin-arroba")).status());
        // JSON mal formado
        assertEquals(400, ejecutar(post("/membresias"), propietario, "{\"nombre\":").status());
    }

    @Test
    void la_membresia_exige_entre_3_y_5_dias() throws Exception {
        assertEquals(400, ejecutar(post("/membresias"), tokenDe(Rol.PROPIETARIO),
                "{\"nombre\":\"Corta" + SECUENCIA.incrementAndGet() + "\",\"precioActual\":10,\"frecuenciaSemanal\":1}").status());
    }

    @Test
    void un_texto_libre_demasiado_largo_se_rechaza_con_400() throws Exception {
        long membresiaId = membresia("20000", 3);
        String largo = "x".repeat(10_000);
        String propietario = tokenDe(Rol.PROPIETARIO);
        // Un texto TEXT (antecedentes) y uno varchar(255) (nombre), que antes terminaba en 500
        for (String campo : List.of("\"Asma\"", "\"Ana\"")) {
            Respuesta r = ejecutar(post("/alumnos"), propietario,
                    cuerpoAlumno(SECUENCIA.incrementAndGet(), membresiaId, "").replace(campo, "\"" + largo + "\""));
            assertEquals(400, r.status(), r.texto());
            assertTrue(r.cuerpo().path("error").asString().contains("caracteres"), r.texto());
        }
        assertEquals(400, ejecutar(post("/membresias"), propietario,
                "{\"nombre\":\"" + largo + "\",\"precioActual\":10,\"frecuenciaSemanal\":3}").status());
        assertEquals(400, ejecutar(post("/ejercicios"), propietario,
                "{\"nombre\":\"Press\",\"grupoMuscular\":\"Pecho\",\"descripcion\":\"" + largo + "\",\"activo\":true}").status());
    }

    /** Contra el servidor HTTP real (embebido): MockMvc no pasa por la página de error de Spring Boot. */
    @Test
    void los_errores_no_exponen_stack_traces_ni_sql() throws Exception {
        long membresiaId = membresia("20000", 3);
        String nombreLargo = "x".repeat(10_000);
        String cuerpo = cuerpoAlumno(SECUENCIA.incrementAndGet(), membresiaId, "").replace("\"Ana\"", "\"" + nombreLargo + "\"");
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/alumnos"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + tokenDe(Rol.PROPIETARIO))
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),
                HttpResponse.BodyHandlers.ofString());

        // Antes era un 500 de la base; desde B-3 lo frena la validación. En los dos casos, sin detalles internos
        assertTrue(r.statusCode() >= 400, r.body());
        for (String filtrado : List.of("Exception", "at mogni.", "org.hibernate", "SQL", "insert into", "trace", "varying")) {
            assertFalse(r.body().contains(filtrado), "la respuesta expone '" + filtrado + "': " + r.body());
        }
    }

    @Test
    void los_campos_extra_del_json_no_se_guardan_y_el_monto_lo_calcula_el_backend() throws Exception {
        String propietario = tokenDe(Rol.PROPIETARIO);
        long membresiaId = membresia("25000", 3);
        int n = SECUENCIA.incrementAndGet();
        Respuesta alta = ejecutar(post("/alumnos"), propietario, cuerpoAlumno(n, membresiaId,
                ",\"id\":999999,\"rol\":\"PROPIETARIO\",\"activo\":false,\"monto\":1,\"precio\":1,\"debeCambiarPassword\":false"));
        assertEquals(201, alta.status(), alta.texto());
        long id = alta.cuerpo().path("id").asLong();

        assertNotEquals(999999L, id);
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase("alumno" + n + "@test.com").orElseThrow();
        assertEquals(Rol.ALUMNO, cuenta.getRol());
        assertTrue(cuenta.isActiva());
        assertTrue(cuenta.isDebeCambiarPassword());
        JsonNode pagos = ejecutar(get("/alumnos/" + id + "/pagos"), propietario).cuerpo();
        assertEquals(1, pagos.size());
        assertEquals(0, new BigDecimal("25000").compareTo(pagos.get(0).path("monto").decimalValue()));

        // Membresía: el id del body no pisa otra
        long otra = membresia("30000", 4);
        Respuesta cambio = ejecutar(put("/membresias/" + membresiaId), propietario,
                "{\"id\":" + otra + ",\"nombre\":\"Renombrada" + n + "\",\"precioActual\":26000,\"frecuenciaSemanal\":3}");
        assertEquals(200, cambio.status(), cambio.texto());
        assertEquals(0, new BigDecimal("30000").compareTo(
                ejecutar(get("/membresias/" + otra), propietario).cuerpo().path("precioActual").decimalValue()));
    }

    // ===================================================================================================
    // 4. Concurrencia y reglas de negocio por API
    // ===================================================================================================

    @Test
    void dos_alumnos_reservando_el_ultimo_lugar_a_la_vez_no_superan_el_cupo() throws Exception {
        long membresiaId = membresia("20000", 3);
        Alta uno = alumno(membresiaId);
        Alta otro = alumno(membresiaId);
        Turno turno = turnoDeManiana(LocalTime.of(19, 0), 1);

        List<Respuesta> respuestas = aLaVez(
                () -> ejecutar(post("/turnos/" + turno.getId() + "/reservar"), uno.token()),
                () -> ejecutar(post("/turnos/" + turno.getId() + "/reservar"), otro.token()));

        assertEquals(1, respuestas.stream().filter(r -> r.status() == 201).count(), respuestas.toString());
        assertEquals(1, reservasDelTurno(turno.getId()));
    }

    @Test
    void doble_clic_en_reservar_crea_una_sola_reserva() throws Exception {
        Alta alumno = alumno(membresia("20000", 3));
        Turno turno = turnoDeManiana(LocalTime.of(20, 0), 10);

        List<Respuesta> respuestas = aLaVez(
                () -> ejecutar(post("/turnos/" + turno.getId() + "/reservar"), alumno.token()),
                () -> ejecutar(post("/turnos/" + turno.getId() + "/reservar"), alumno.token()));

        assertEquals(1, respuestas.stream().filter(r -> r.status() == 201).count(), respuestas.toString());
        assertEquals(1, reservasDelTurno(turno.getId()));
    }

    private int reservasDelTurno(long turnoId) {
        return new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from reservas where turno_id = ? and estado = 'RESERVADA'", Integer.class, turnoId);
    }

    @Test
    void la_api_no_deja_saltear_un_turno_por_dia_ni_el_limite_semanal() throws Exception {
        Alta alumno = alumno(membresia("20000", 3));
        Turno manianaTemprano = turnoDeManiana(LocalTime.of(8, 0), 10);
        Turno manianaTarde = turnoDeManiana(LocalTime.of(21, 0), 10);

        assertEquals(201, ejecutar(post("/turnos/" + manianaTemprano.getId() + "/reservar"), alumno.token()).status());
        Respuesta segundo = ejecutar(post("/turnos/" + manianaTarde.getId() + "/reservar"), alumno.token());

        assertEquals(409, segundo.status(), segundo.texto());
    }

    @Test
    void dos_pagos_simultaneos_del_mismo_alumno_no_registran_dos_veces_la_misma_cuota() throws Exception {
        String propietario = tokenDe(Rol.PROPIETARIO);
        Alta alumno = alumno(membresia("20000", 3));
        String cuerpo = "{\"alumnoId\":" + alumno.id() + ",\"medioDePago\":\"EFECTIVO\"}";

        List<Respuesta> respuestas = aLaVez(
                () -> ejecutar(post("/pagos"), propietario, cuerpo),
                () -> ejecutar(post("/pagos"), propietario, cuerpo));

        // A lo sumo uno (puede no corresponder pagar todavía); nunca dos cuotas pagadas por el mismo pedido duplicado
        long aceptados = respuestas.stream().filter(r -> r.status() == 201 || r.status() == 200).count();
        assertTrue(aceptados <= 1, respuestas.toString());
        if (aceptados == 1) {
            JsonNode ok = respuestas.stream().filter(r -> r.status() / 100 == 2).findFirst().orElseThrow().cuerpo();
            Integer pagosDeLaCuota = new JdbcTemplate(dataSource).queryForObject(
                    "select count(*) from pagos where cuota_id = ?", Integer.class, ok.path("cuotaId").asLong());
            assertEquals(1, pagosDeLaCuota);
        }
    }

    @Test
    void la_zona_horaria_de_la_aplicacion_es_buenos_aires() {
        assertEquals("America/Argentina/Buenos_Aires", ZonaHoraria.ZONA);
    }

    // ===================================================================================================
    // 5. Configuración
    // ===================================================================================================

    /** M-3: fuera del perfil local, Swagger y la documentación de la API no se publican. */
    @Test
    void swagger_y_la_documentacion_de_la_api_no_son_publicos() throws Exception {
        for (String ruta : List.of("/v3/api-docs", "/swagger-ui/index.html", "/swagger-ui.html")) {
            int status = ejecutar(get(ruta), null).status();
            assertNotEquals(200, status, ruta);
            assertNotEquals(302, status, ruta);
        }
    }
}
