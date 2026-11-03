package mogni.system_backend.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

/**
 * Plantillas de mail guardadas en src/main/resources/mail/ (una versión HTML y una de texto plano por mail). Los
 * valores se reemplazan en los marcadores {{nombre}}. Para cambiar el diseño del mail se edita el .html: no hace
 * falta tocar Brevo.
 */
public final class PlantillaMail {

    private PlantillaMail() {
    }

    /** Lee mail/{archivo} y reemplaza cada {{clave}} por su valor. */
    public static String completar(String archivo, Map<String, String> valores) {
        String contenido = leer("mail/" + archivo);
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            contenido = contenido.replace("{{" + valor.getKey() + "}}", valor.getValue());
        }
        return contenido;
    }

    private static String leer(String ruta) {
        try (InputStream entrada = new ClassPathResource(ruta).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró la plantilla de mail " + ruta, e);
        }
    }
}
