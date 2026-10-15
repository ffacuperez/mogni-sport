-- =====================================================================
-- Datos de prueba (modelo: Alumno 1..* Inscripcion *..1 Membresia, Inscripcion 1..* Cuota 1..* Pago)
--
-- CORRER UNA SOLA VEZ, con el profile "seed":
--   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,seed
-- Cuando termine de levantar, frenar la app y volver a levantarla SIN "seed"
-- (./mvnw spring-boot:run). Si se deja el profile seed, cada arranque recrea las
-- tablas y BORRA TODOS LOS DATOS, incluidos los cargados desde la aplicación.
-- Sin el profile seed este archivo no se ejecuta (spring.sql.init.mode queda en "embedded").
--
-- Cuentas que quedan cargadas (contraseñas guardadas con hash BCrypt):
--   PROPIETARIO  admin@mogni.com       / admin123
--   ENTRENADOR   entrenador@mogni.com  / entrenador123
--   ALUMNOS      su email              / su DNI (deben cambiarla al primer ingreso)
-- =====================================================================

-- 1) Borrar todos los datos (CASCADE respeta las foreign keys)
TRUNCATE TABLE pagos, cuotas, inscripciones, alumnos, membresias, cuentas RESTART IDENTITY CASCADE;

-- 2) Membresías
INSERT INTO membresias (nombre, precio_actual, frecuencia_semanal, descripcion) VALUES
    ('Funcional 2x',   18000, 2, 'Clases de funcional dos veces por semana'),
    ('Musculación 3x', 25000, 3, 'Sala de musculación tres veces por semana'),
    ('Full 5x',        38000, 5, 'Acceso completo de lunes a viernes');

-- 3) Cuentas del personal
INSERT INTO cuentas (email, password, rol, debe_cambiar_password, activa) VALUES
    ('admin@mogni.com',      '$2a$10$PKCTgf9QVVhJCgXhEx5f.elRYxZSd4Ior8DPAyeLx.EiZeTYEugx.', 'PROPIETARIO', false, true),
    ('entrenador@mogni.com', '$2a$10$.pe2XJjj6QStzOwvtH7mbO6Qb.9UeMWEs754R2t7V9sUc4vWkw9gm', 'ENTRENADOR',  false, true);

-- 4) Un usuario por alumno (email = usuario, DNI = contraseña)
INSERT INTO cuentas (email, password, rol, debe_cambiar_password, activa) VALUES
    ('lucia.gomez@mail.com',      '$2a$10$aI2uc7/XXsf9gB5t/GOp5uQ8clB7doGl0ULbmxQdkKPMBvGRSctZ2', 'ALUMNO', true, true),
    ('martin.fernandez@mail.com', '$2a$10$MTmP7kbVp4vA/ySMBFU/z.6C0o1qABN41kfbwvf1JRuelGlb78KQy', 'ALUMNO', true, true),
    ('sofia.ruiz@mail.com',       '$2a$10$WOQN9UA6cfw09Mg9ibtl2u8OaLwsFYp/Bl8Qs/YuecYzW8URLdbWO', 'ALUMNO', true, true),
    ('tomas.diaz@mail.com',       '$2a$10$Yd4KszCe.zguaocRHVXIe.4OLwtS4mmajlHwKISmz7ujRpElZmug.', 'ALUMNO', true, true),
    ('valentina.lopez@mail.com',  '$2a$10$NIoLsLEAT6PK52UsPeTu0e81v8NYWjUt0hzBELEbLvf9LiayRE7AO', 'ALUMNO', true, true);

-- 5) Alumnos (todos mayores de 16, DNI y email únicos)
INSERT INTO alumnos (nombre, apellido, edad, telefono, sexo, dni, email, fecha_nacimiento, objetivo, cuestionario_salud, modalidad, cuenta_id) VALUES
    ('Lucía', 'Gómez', date_part('year', age(CURRENT_DATE, DATE '1983-04-12'))::int, '3415551111', 'Femenino', '30111222',
        'lucia.gomez@mail.com', DATE '1983-04-12', 'Bajar de peso', NULL,
        'PRESENCIAL', (SELECT id FROM cuentas WHERE email = 'lucia.gomez@mail.com')),
    ('Martín', 'Fernández', date_part('year', age(CURRENT_DATE, DATE '1990-09-03'))::int, '3415552222', 'Masculino', '35222333',
        'martin.fernandez@mail.com', DATE '1990-09-03', 'Ganar masa muscular', 'Lesión de rodilla en 2022',
        'PRESENCIAL', (SELECT id FROM cuentas WHERE email = 'martin.fernandez@mail.com')),
    ('Sofía', 'Ruiz', date_part('year', age(CURRENT_DATE, DATE '1995-01-25'))::int, '3415553333', 'Femenino', '38333444',
        'sofia.ruiz@mail.com', DATE '1995-01-25', NULL, NULL,
        'PRESENCIAL', (SELECT id FROM cuentas WHERE email = 'sofia.ruiz@mail.com')),
    ('Tomás', 'Díaz', date_part('year', age(CURRENT_DATE, DATE '1998-07-30'))::int, '3415554444', 'Masculino', '40444555',
        'tomas.diaz@mail.com', DATE '1998-07-30', 'Mejorar resistencia', NULL,
        'ONLINE', (SELECT id FROM cuentas WHERE email = 'tomas.diaz@mail.com')),
    ('Valentina', 'López', date_part('year', age(CURRENT_DATE, DATE '2001-11-18'))::int, '3415555555', 'Femenino', '42555666',
        'valentina.lopez@mail.com', DATE '2001-11-18', NULL, 'Asma leve',
        'PRESENCIAL', (SELECT id FROM cuentas WHERE email = 'valentina.lopez@mail.com'));

-- 6) Una inscripción ACTIVA por alumno (fecha_fin queda NULL mientras está activa).
--    Las fechas de inicio varían para que haya cuotas en todos los estados.
INSERT INTO inscripciones (fecha_inicio, fecha_fin, estado, alumno_id, membresia_id) VALUES
    (CURRENT_DATE - 40, NULL, 'ACTIVA', (SELECT id FROM alumnos WHERE dni = '30111222'), (SELECT id FROM membresias WHERE nombre = 'Musculación 3x')),
    (CURRENT_DATE - 27, NULL, 'ACTIVA', (SELECT id FROM alumnos WHERE dni = '35222333'), (SELECT id FROM membresias WHERE nombre = 'Full 5x')),
    (CURRENT_DATE - 35, NULL, 'ACTIVA', (SELECT id FROM alumnos WHERE dni = '38333444'), (SELECT id FROM membresias WHERE nombre = 'Funcional 2x')),
    (CURRENT_DATE - 10, NULL, 'ACTIVA', (SELECT id FROM alumnos WHERE dni = '40444555'), (SELECT id FROM membresias WHERE nombre = 'Musculación 3x')),
    (CURRENT_DATE,      NULL, 'ACTIVA', (SELECT id FROM alumnos WHERE dni = '42555666'), (SELECT id FROM membresias WHERE nombre = 'Full 5x'));

-- 7) Primera cuota de cada inscripción (vence a 30 días del inicio, al precio de la membresía):
--    Lucía ATRASADA, Martín PENDIENTE próxima a vencer, Sofía (pagada abajo), Tomás y Valentina PENDIENTE
INSERT INTO cuotas (inscripcion_id, periodo, monto_esperado, fecha_vencimiento, estado)
SELECT i.id, to_char(i.fecha_inicio, 'YYYY-MM'), m.precio_actual, i.fecha_inicio + 30,
       CASE WHEN i.fecha_inicio + 30 < CURRENT_DATE THEN 'ATRASADA' ELSE 'PENDIENTE' END
FROM inscripciones i JOIN membresias m ON m.id = i.membresia_id;

-- 8) Sofía pagó su cuota hace 3 días, en efectivo
UPDATE cuotas SET estado = 'PAGADA'
WHERE inscripcion_id = (SELECT i.id FROM inscripciones i JOIN alumnos a ON a.id = i.alumno_id WHERE a.dni = '38333444');
INSERT INTO pagos (cuota_id, monto, medio_de_pago, fecha_pago)
SELECT c.id, c.monto_esperado, 'EFECTIVO', CURRENT_DATE - 3
FROM cuotas c JOIN inscripciones i ON i.id = c.inscripcion_id JOIN alumnos a ON a.id = i.alumno_id
WHERE a.dni = '38333444';
