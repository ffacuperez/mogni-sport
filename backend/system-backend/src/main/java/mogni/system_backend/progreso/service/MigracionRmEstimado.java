package mogni.system_backend.progreso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MOGNI-269: recalcula el 1RM, los kg usados y el RIR real de los ejercicios ya registrados con la regla nueva (la
 * serie de más peso; si empatan, la de más repeticiones; si también, la primera), la misma que
 * RegistroEjercicio.serieDeReferencia. El 1RM de esa serie ya está guardado en registros_serie con la misma fórmula.
 * Los registros anteriores a HU-269 no tienen series y no se tocan. Es idempotente: corre en cada arranque y, una vez
 * recalculada la base, no modifica nada.
 */
@Component
@Order(2)
public class MigracionRmEstimado implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionRmEstimado.class);

    private static final String RECALCULAR = """
            UPDATE registros_ejercicio e
               SET rm_estimado = r.rm_estimado, kg_usados = r.peso, rir_real = r.rir
              FROM (SELECT DISTINCT ON (registro_ejercicio_id) registro_ejercicio_id, rm_estimado, peso, rir
                      FROM registros_serie
                     ORDER BY registro_ejercicio_id, peso DESC, repeticiones DESC, orden ASC) r
             WHERE e.id = r.registro_ejercicio_id
               AND (e.rm_estimado IS DISTINCT FROM r.rm_estimado
                    OR e.kg_usados IS DISTINCT FROM r.peso
                    OR e.rir_real IS DISTINCT FROM r.rir)
            """;

    private final JdbcTemplate jdbcTemplate;

    public MigracionRmEstimado(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int recalculados = jdbcTemplate.update(RECALCULAR);
        if (recalculados > 0) {
            log.info("MOGNI-269: 1RM recalculado con la serie de más peso en {} ejercicios registrados", recalculados);
        }
    }
}
