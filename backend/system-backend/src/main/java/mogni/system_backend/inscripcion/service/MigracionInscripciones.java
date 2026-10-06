package mogni.system_backend.inscripcion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapta los datos existentes al modelo sin estado VENCIDA (fechaFin = null mientras está ACTIVA,
 * fecha de cancelación cuando se CANCELA). Es idempotente: corre en cada arranque y, una vez
 * migrada la base, no modifica nada. Corre antes que cualquier otro proceso de arranque.
 */
@Component
@Order(1)
public class MigracionInscripciones implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionInscripciones.class);

    private final JdbcTemplate jdbcTemplate;

    public MigracionInscripciones(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // ddl-auto=update no quita restricciones NOT NULL que ya existen
        jdbcTemplate.execute("ALTER TABLE inscripciones ALTER COLUMN fecha_fin DROP NOT NULL");
        int canceladas = jdbcTemplate.update(
                "UPDATE inscripciones SET estado = 'CANCELADA' WHERE estado = 'VENCIDA'");
        int activas = jdbcTemplate.update(
                "UPDATE inscripciones SET fecha_fin = NULL WHERE estado = 'ACTIVA' AND fecha_fin IS NOT NULL");
        if (canceladas > 0 || activas > 0) {
            log.info("Migración de inscripciones: {} VENCIDA -> CANCELADA, {} ACTIVA sin fecha de fin", canceladas, activas);
        }
    }
}
