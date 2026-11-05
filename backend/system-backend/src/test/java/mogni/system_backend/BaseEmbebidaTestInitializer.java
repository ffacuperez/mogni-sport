package mogni.system_backend;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Base de datos de los tests (perfil "test", ver application-test.properties): un PostgreSQL embebido por ejecución y
 * una base nueva y vacía por cada contexto de Spring, así los contextos que quedan en caché no se pisan los datos.
 * Ningún test se conecta a una base compartida. Las clases que arman su propia base con @DynamicPropertySource la
 * siguen usando: esas propiedades tienen prioridad sobre estas.
 */
public class BaseEmbebidaTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final AtomicInteger CONTEXTOS = new AtomicInteger();
    private static EmbeddedPostgres servidor;

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        String base = "test_" + CONTEXTOS.incrementAndGet();
        EmbeddedPostgres postgres = servidor();
        try (Connection conexion = postgres.getPostgresDatabase().getConnection();
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("CREATE DATABASE " + base);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base de los tests", e);
        }
        contexto.getEnvironment().getPropertySources().addLast(new MapPropertySource("baseEmbebidaDeTests", Map.of(
                "spring.datasource.url", postgres.getJdbcUrl("postgres", base),
                "spring.datasource.username", "postgres",
                "spring.datasource.password", "postgres")));
    }

    private static synchronized EmbeddedPostgres servidor() {
        if (servidor == null) {
            try {
                servidor = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo iniciar el PostgreSQL embebido", e);
            }
        }
        return servidor;
    }
}
