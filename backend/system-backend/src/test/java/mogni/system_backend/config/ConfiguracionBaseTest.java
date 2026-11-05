package mogni.system_backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * La configuración que se empaqueta con la aplicación (auditoría de seguridad). Lee los archivos tal cual, sin
 * levantar Spring: lo que vale en producción es application.properties más las variables de entorno del deploy.
 */
class ConfiguracionBaseTest {

    private static Properties leer(String archivo) throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConfiguracionBaseTest.class.getResourceAsStream("/" + archivo)) {
            propiedades.load(entrada);
        }
        return propiedades;
    }

    /** B-4: el SQL de Hibernate no va a los logs, salvo en el perfil local. */
    @Test
    void el_sql_solo_se_muestra_en_el_perfil_local() throws IOException {
        assertEquals("false", leer("application.properties").getProperty("spring.jpa.show-sql"));
        assertEquals("true", leer("application-local.properties").getProperty("spring.jpa.show-sql"));
    }
}
