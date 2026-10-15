package mogni.system_backend.alumno.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-253: los alumnos cargados antes de que existiera la modalidad quedan con la columna en NULL;
 * se les pone PRESENCIAL. Es idempotente: una vez migrada la base, no modifica nada.
 */
@Component
@Order(1)
public class MigracionModalidadAlumnos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionModalidadAlumnos.class);

    private final JdbcTemplate jdbcTemplate;

    public MigracionModalidadAlumnos(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int actualizados = jdbcTemplate.update(
                "UPDATE alumnos SET modalidad = 'PRESENCIAL' WHERE modalidad IS NULL");
        if (actualizados > 0) {
            log.info("Migración de modalidad: {} alumnos sin modalidad pasaron a PRESENCIAL", actualizados);
        }
    }
}
