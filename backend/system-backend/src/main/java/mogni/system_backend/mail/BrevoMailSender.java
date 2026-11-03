package mogni.system_backend.mail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Envío de mails transaccionales con la API HTTP de Brevo (POST /v3/smtp/email). La API key se lee SOLO de la
 * variable de entorno BREVO_API_KEY (ver application.properties): nunca se escribe en el código ni en un .properties.
 * Sin API key (por ejemplo, en un entorno de pruebas) no envía nada y avisa en el log.
 */
@Component
public class BrevoMailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoMailSender.class);
    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final String apiKey;
    private final String remitenteEmail;
    private final String remitenteNombre;

    public BrevoMailSender(@Value("${brevo.api-key:}") String apiKey,
                           @Value("${brevo.sender-email:}") String remitenteEmail,
                           @Value("${brevo.sender-name:Mogni Sport}") String remitenteNombre) {
        this.apiKey = apiKey.trim();
        this.remitenteEmail = remitenteEmail.trim();
        this.remitenteNombre = remitenteNombre.trim();
    }

    /** Tiene API key y remitente: puede enviar. */
    public boolean estaConfigurado() {
        return !apiKey.isEmpty() && !remitenteEmail.isEmpty();
    }

    /**
     * Envía un mail. Lanza {@link MailNoEnviadoException} si Brevo lo rechaza o no responde, o si no está configurado.
     */
    public void enviar(String destinatario, String asunto, String html, String texto) {
        if (!estaConfigurado()) {
            throw new MailNoEnviadoException(
                    "Brevo no está configurado: faltan las variables BREVO_API_KEY y/o BREVO_SENDER_EMAIL");
        }
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", remitenteNombre, "email", remitenteEmail),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "htmlContent", html,
                "textContent", texto);
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(cuerpo)))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                // El cuerpo de error de Brevo explica el motivo (remitente sin verificar, IP no autorizada, etc.)
                throw new MailNoEnviadoException("Brevo respondió " + respuesta.statusCode() + ": " + respuesta.body());
            }
            log.info("Mail «{}» enviado con Brevo", asunto);
        } catch (IOException e) {
            throw new MailNoEnviadoException("No se pudo conectar con Brevo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailNoEnviadoException("Se interrumpió el envío del mail", e);
        }
    }
}
