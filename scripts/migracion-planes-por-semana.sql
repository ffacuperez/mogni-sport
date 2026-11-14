-- Paso de la base del servidor a los planes por semana (HU «REFACTOR: Gestionar planes de entrenamiento por semana»).
--
-- Solo sirve mientras NO haya plantillas, planes ni sesiones registradas (en el servidor, al 14/11/2026, el profe
-- solo cargó ejercicios). Borra las tres tablas que cambiaron de estructura, que están vacías, y el backend nuevo las
-- vuelve a crear al arrancar, junto con las tablas de semanas. Ejercicios, alumnos, pagos y turnos no se tocan.
--
-- Se corre con el backend detenido y después de hacer el backup:
--   docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < scripts/migracion-planes-por-semana.sql

BEGIN;

-- Si hay algo cargado, corta acá y no borra nada
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM plantillas_plan)
        OR EXISTS (SELECT 1 FROM planes_asignados)
        OR EXISTS (SELECT 1 FROM registros_sesion) THEN
        RAISE EXCEPTION 'Hay plantillas, planes o sesiones registradas: este script no sirve. No se borró nada.';
    END IF;
END $$;

-- CASCADE solo saca las claves foráneas que apuntan a estas tablas; el backend las vuelve a crear
DROP TABLE sesiones_plan, sesiones_plantilla, planes_asignados CASCADE;

COMMIT;
