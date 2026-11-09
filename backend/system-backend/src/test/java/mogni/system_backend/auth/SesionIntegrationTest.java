package mogni.system_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import mogni.system_backend.auth.controller.CookieSesion;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.entity.TokenRecuperacionPassword;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.auth.repository.TokenRecuperacionPasswordRepository;
import mogni.system_backend.config.ZonaHoraria;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sesiones de punta a punta (HTTP, filtros, cookies y base embebida): login con cookie de renovación, renovación,
 * cierre de sesión y cierre en todos los dispositivos al cambiar o restablecer la contraseña.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SesionIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(9000);
    private static final String CLAVE = "clave-inicial-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private CuentaRepository cuentaRepository;
    @Autowired private TokenRecuperacionPasswordRepository tokenRecuperacionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final JsonMapper json = JsonMapper.builder().build();

    /** Lo que ve el dispositivo después de un login o una renovación. */
    private record Sesion(String token, Cookie cookie, MockHttpServletResponse respuesta) {
    }

    private Cuenta cuenta(Rol rol) {
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(rol.name().toLowerCase() + SECUENCIA.incrementAndGet() + "@test.com");
        cuenta.setPassword(passwordEncoder.encode(CLAVE));
        cuenta.setRol(rol);
        cuenta.setDebeCambiarPassword(false);
        cuenta.setActiva(true);
        return cuentaRepository.save(cuenta);
    }

    private Sesion login(Cuenta cuenta) throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + cuenta.getEmail() + "\",\"password\":\"" + CLAVE + "\"}")).andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        return sesion(respuesta);
    }

    private MockHttpServletResponse renovar(Cookie cookie) throws Exception {
        MockHttpServletRequestBuilder pedido = post("/auth/refresh");
        if (cookie != null) pedido.cookie(cookie);
        return mockMvc.perform(pedido).andReturn().getResponse();
    }

    private Sesion sesion(MockHttpServletResponse respuesta) throws Exception {
        JsonNode cuerpo = json.readTree(respuesta.getContentAsString());
        return new Sesion(cuerpo.get("token").asText(), respuesta.getCookie(CookieSesion.NOMBRE), respuesta);
    }

    private int consultar(String token) throws Exception {
        return mockMvc.perform(get("/membresias").header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
    }

    @Test
    void el_login_entrega_la_cookie_de_renovacion_que_el_javascript_no_puede_leer() throws Exception {
        Sesion sesion = login(cuenta(Rol.ALUMNO));

        String setCookie = sesion.respuesta().getHeader("Set-Cookie");
        assertNotNull(sesion.cookie());
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"), setCookie);
        assertEquals(45 * 24 * 3600, sesion.cookie().getMaxAge());
    }

    @Test
    void la_cookie_del_personal_dura_14_dias() throws Exception {
        assertEquals(14 * 24 * 3600, login(cuenta(Rol.PROPIETARIO)).cookie().getMaxAge());
    }

    @Test
    void con_la_cookie_se_obtiene_un_token_nuevo_sin_volver_a_iniciar_sesion() throws Exception {
        Sesion inicial = login(cuenta(Rol.PROPIETARIO));

        MockHttpServletResponse respuesta = renovar(inicial.cookie());

        assertEquals(200, respuesta.getStatus());
        Sesion renovada = sesion(respuesta);
        assertEquals(200, consultar(renovada.token()));
        // El token de renovación también se reemplaza
        assertNotEquals(inicial.cookie().getValue(), renovada.cookie().getValue());
    }

    @Test
    void sin_cookie_o_con_una_inventada_responde_401_y_la_borra() throws Exception {
        assertEquals(401, renovar(null).getStatus());

        MockHttpServletResponse respuesta = renovar(new Cookie(CookieSesion.NOMBRE, "inventada"));
        assertEquals(401, respuesta.getStatus());
        assertEquals(0, respuesta.getCookie(CookieSesion.NOMBRE).getMaxAge());
    }

    @Test
    void al_cerrar_sesion_dejan_de_servir_el_token_y_la_cookie() throws Exception {
        Sesion sesion = login(cuenta(Rol.PROPIETARIO));

        MockHttpServletResponse logout = mockMvc.perform(post("/auth/logout").cookie(sesion.cookie())).andReturn().getResponse();

        assertEquals(204, logout.getStatus());
        assertEquals(0, logout.getCookie(CookieSesion.NOMBRE).getMaxAge());
        assertEquals(401, consultar(sesion.token()));
        assertEquals(401, renovar(sesion.cookie()).getStatus());
    }

    @Test
    void cerrar_sesion_en_un_dispositivo_no_cierra_los_demas() throws Exception {
        Cuenta cuenta = cuenta(Rol.PROPIETARIO);
        Sesion celular = login(cuenta);
        Sesion compu = login(cuenta);

        mockMvc.perform(post("/auth/logout").cookie(compu.cookie()));

        assertEquals(200, consultar(celular.token()));
        assertEquals(200, renovar(celular.cookie()).getStatus());
    }

    @Test
    void al_cambiar_la_contrasenia_se_cierran_los_otros_dispositivos_y_este_sigue_adentro() throws Exception {
        Cuenta cuenta = cuenta(Rol.PROPIETARIO);
        Sesion otro = login(cuenta);
        Sesion este = login(cuenta);

        MockHttpServletResponse cambio = mockMvc.perform(post("/auth/cambiar-password")
                .header("Authorization", "Bearer " + este.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"" + CLAVE + "\",\"passwordNueva\":\"clave-nueva-2\",\"passwordConfirmacion\":\"clave-nueva-2\"}"))
                .andReturn().getResponse();

        assertEquals(200, cambio.getStatus());
        assertEquals(401, consultar(otro.token()));
        assertEquals(401, renovar(otro.cookie()).getStatus());
        Sesion nueva = sesion(cambio);
        assertEquals(200, consultar(nueva.token()));
        assertEquals(200, renovar(nueva.cookie()).getStatus());
    }

    @Test
    void al_restablecer_la_contrasenia_con_el_link_del_mail_se_cierran_todas_las_sesiones() throws Exception {
        Cuenta cuenta = cuenta(Rol.ALUMNO);
        Sesion sesion = login(cuenta);
        String link = "token-del-link-de-prueba-" + SECUENCIA.incrementAndGet();
        tokenRecuperacionRepository.save(new TokenRecuperacionPassword(
                cuenta, sha256(link), ZonaHoraria.ahora(), ZonaHoraria.ahora().plusMinutes(30)));

        int status = mockMvc.perform(post("/auth/restablecer-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + link + "\",\"passwordNueva\":\"clave-nueva-2\",\"passwordConfirmacion\":\"clave-nueva-2\"}"))
                .andReturn().getResponse().getStatus();

        assertEquals(200, status);
        assertEquals(401, renovar(sesion.cookie()).getStatus());
    }

    @Test
    void una_cuenta_dada_de_baja_no_puede_renovar() throws Exception {
        Cuenta cuenta = cuenta(Rol.ALUMNO);
        Sesion sesion = login(cuenta);
        cuenta.setActiva(false);
        cuentaRepository.save(cuenta);

        assertEquals(401, renovar(sesion.cookie()).getStatus());
    }

    @Test
    void un_token_firmado_pero_sin_sesion_no_sirve() throws Exception {
        Cuenta cuenta = cuenta(Rol.PROPIETARIO);
        // Como los tokens de antes de las sesiones: firma y vencimiento válidos, pero sin "sid"
        String sinSesion = Jwts.builder().subject(cuenta.getEmail()).claim("rol", "PROPIETARIO")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor("clave-solo-para-las-pruebas-automatizadas-de-mogni".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(401, consultar(sinSesion));
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }
}
