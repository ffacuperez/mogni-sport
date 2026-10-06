package mogni.system_backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Reglas de seguridad contra el servidor real (MockMvc no hace el re-despacho a /error que hace
 * Tomcat con sendError, y ahí era donde un 403 terminaba convertido en 401).
 * Usa las cuentas del seed, que existen en la base.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHttpRealTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    private Environment environment;

    private String url(String path) {
        return "http://localhost:" + environment.getProperty("local.server.port") + path;
    }

    private String token(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url("/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "login de " + email);
        return response.body().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private int status(String method, String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .method(method, method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("{}"));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void sinTokenResponde401() throws Exception {
        assertEquals(401, status("GET", "/cuotas", null));
        assertEquals(401, status("GET", "/alumnos", null));
    }

    @Test
    void rolSinPermisoResponde403YNo401() throws Exception {
        String entrenador = token("entrenador@mogni.com", "entrenador123");

        assertEquals(403, status("GET", "/cuotas", entrenador));
        assertEquals(403, status("POST", "/pagos", entrenador));
        assertEquals(403, status("GET", "/alumnos/me", entrenador));
        assertEquals(403, status("GET", "/alumnos/1/pagos", entrenador));
        // Lo que sí puede hacer sigue funcionando
        assertEquals(200, status("GET", "/alumnos", entrenador));
    }

    @Test
    void propietarioAccedeACobranzas() throws Exception {
        String propietario = token("admin@mogni.com", "admin123");

        assertEquals(200, status("GET", "/cuotas", propietario));
        // Pasa la seguridad; el cuerpo vacío lo rechaza la validación (no se registra ningún pago)
        assertEquals(400, status("POST", "/pagos", propietario));
    }
}
