package mogni.system_backend.cuota.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapta la base al modelo "la cuota es un período pagado" (rediseño de cuotas, sección 14, "Base de datos:
 * migración"). No borra columnas ni datos, para no romper las otras ramas que usan la misma base. Es idempotente:
 * corre en cada arranque y, una vez migrada la base, no modifica nada.
 *
 * <ol>
 *   <li>Saca el NOT NULL de cuotas.estado y cuotas.periodo, que el código nuevo ya no carga.</li>
 *   <li>Completa fecha_inicio (vencimiento - 30) en las cuotas existentes.</li>
 *   <li>Pone recargo 0 en los pagos existentes.</li>
 *   <li>Completa membresia_id de cada cuota con la de su inscripción.</li>
 *   <li>Agrega CANCELADA_POR_SISTEMA a la regla CHECK de reservas.estado: Hibernate la crea con los valores del enum
 *       al crear la tabla y ddl-auto=update nunca la actualiza.</li>
 * </ol>
 */
@Component
@Order(2)
public class MigracionCuotasPeriodoPagado implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionCuotasPeriodoPagado.class);

    static final String VALORES_ESTADO_RESERVA = "'RESERVADA', 'CANCELADA', 'CANCELADA_POR_GIMNASIO', 'CANCELADA_POR_SISTEMA'";

    private final JdbcTemplate jdbcTemplate;

    public MigracionCuotasPeriodoPagado(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1) En una base creada desde cero (perfil seed) las columnas viejas no existen
        for (String columna : List.of("estado", "periodo")) {
            if (existeColumnaObligatoria("cuotas", columna)) {
                jdbcTemplate.execute("ALTER TABLE cuotas ALTER COLUMN " + columna + " DROP NOT NULL");
                log.info("Migración de cuotas: cuotas.{} ahora acepta NULL", columna);
            }
        }

        // 2), 3) y 4)
        int conInicio = jdbcTemplate.update(
                "UPDATE cuotas SET fecha_inicio = fecha_vencimiento - 30 WHERE fecha_inicio IS NULL");
        int conRecargo = jdbcTemplate.update("UPDATE pagos SET recargo = 0 WHERE recargo IS NULL");
        int conMembresia = jdbcTemplate.update("""
                UPDATE cuotas c SET membresia_id = i.membresia_id
                FROM inscripciones i
                WHERE i.id = c.inscripcion_id AND c.membresia_id IS NULL""");
        if (conInicio > 0 || conRecargo > 0 || conMembresia > 0) {
            log.info("Migración de cuotas: {} cuotas con fecha de inicio, {} pagos con recargo 0, {} cuotas con membresía",
                    conInicio, conRecargo, conMembresia);
        }

        // 5)
        List<String> regla = jdbcTemplate.queryForList("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conname = 'reservas_estado_check' AND conrelid = 'reservas'::regclass""", String.class);
        if (!regla.isEmpty() && !regla.get(0).contains("CANCELADA_POR_SISTEMA")) {
            jdbcTemplate.execute("ALTER TABLE reservas DROP CONSTRAINT reservas_estado_check");
            jdbcTemplate.execute("ALTER TABLE reservas ADD CONSTRAINT reservas_estado_check CHECK (estado IN ("
                    + VALORES_ESTADO_RESERVA + "))");
            log.info("Migración de reservas: la regla reservas_estado_check ahora acepta CANCELADA_POR_SISTEMA");
        }
    }

    private boolean existeColumnaObligatoria(String tabla, String columna) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND column_name = ? AND is_nullable = 'NO'""",
                Integer.class, tabla, columna);
        return cantidad != null && cantidad > 0;
    }
}
