-- =====================================================================
-- Datos de prueba (modelo: Alumno 1..* Inscripcion *..1 Membresia, Inscripcion 1..* Cuota 1..1 Pago)
--
-- CORRER UNA SOLA VEZ, con el profile "seed":
--   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,seed
-- Cuando termine de levantar, frenar la app y volver a levantarla SIN "seed"
-- (./mvnw spring-boot:run). Si se deja el profile seed, cada arranque recrea las
-- tablas y BORRA TODOS LOS DATOS, incluidos los cargados desde la aplicación.
-- Sin el profile seed este archivo no se ejecuta (spring.sql.init.mode queda en "embedded").
--
-- Todas las fechas son relativas al día en que se corre (CURRENT_DATE), así cada alumno queda en la situación
-- indicada sin importar cuándo se cargue. Hay un alumno por cada caso de las pruebas manuales
-- (documentacion/Cuotas-Pruebas-Manuales.md).
--
-- Cuentas que quedan cargadas (contraseñas guardadas con hash BCrypt):
--   PROPIETARIO    admin@mogni.com          / admin123
--   ENTRENADOR     entrenador@mogni.com     / entrenador123
--   NUTRICIONISTA  nutricionista@mogni.com  / nutri123
--   ALUMNOS        su email                 / su DNI (deben cambiarla al primer ingreso)
--
--   Alumno (DNI)                Membresía        Situación
--   Lucía Gómez (30111222)      x4   En mora hace 3 días, la 2ª cuota la pagó en mora con recargo
--   Camila Torres (37222000)    x3   En mora hace 30 días (no hay baja automática)
--   Bruno Acosta (39333000)     x4   Entró en mora hoy
--   Sofía Ruiz (38333444)       x3   Vencida (ayer)
--   Martín Fernández (35222333) x5   Próxima a vencer (3 días)
--   Agustina Romero (41444000)  x3   Al día, vence en 8 días: ya puede renovar (ventana de 10)
--   Joaquín Sosa (36111000)     x4   Al día: ya pagó el mes siguiente con x5 (PROGRAMADA desde
--                                                dentro de 4 días)
--   Nicolás Benítez (33555000)  x5   Al día (20 días): pasó de x3 a x5 hace 3 días y
--                                                pagó la diferencia (historial con dos pagos)
--   Tomás Díaz (40444555)       x4   Al día (20 días), 2 cuotas encadenadas
--   Valentina López (42555666)  x5   Al día (alta hoy)
--   Diego Molina (34666000)     x4   INACTIVO (baja hace 2 días) con 10 días pagos: se reactiva sin pagar
--   Paula Herrera (43777000)    x5   INACTIVA (baja hace 15 días), sin días pagos: se reactiva con pago
--
-- Franjas horarias: lunes a viernes de 08:00 a 12:00 y de 17:00 a 21:00 (turnos de 1 hora, el cupo lo crea
-- InicializacionConfiguracionGimnasio). Los turnos de cada día se generan solos al consultarlos.
-- =====================================================================

-- 1) Borrar todos los datos (CASCADE respeta las foreign keys)
TRUNCATE TABLE pagos, cuotas, inscripciones, alumnos, membresias, cuentas, franjas_horarias RESTART IDENTITY CASCADE;

-- 2) Membresías
INSERT INTO membresias (nombre, precio_actual, frecuencia_semanal, descripcion) VALUES
    ('x3', 18000, 3, 'Tres veces por semana'),
    ('x4', 25000, 4, 'Cuatro veces por semana'),
    ('x5', 38000, 5, 'Acceso completo de lunes a viernes');

-- 3) Cuentas del personal
INSERT INTO cuentas (email, password, rol, debe_cambiar_password, activa) VALUES
    ('admin@mogni.com',         '$2a$10$PKCTgf9QVVhJCgXhEx5f.elRYxZSd4Ior8DPAyeLx.EiZeTYEugx.', 'PROPIETARIO',   false, true),
    ('entrenador@mogni.com',    '$2a$10$.pe2XJjj6QStzOwvtH7mbO6Qb.9UeMWEs754R2t7V9sUc4vWkw9gm', 'ENTRENADOR',    false, true),
    ('nutricionista@mogni.com', '$2a$10$thhBA8zfjSP5/BPudzx1a.Y9J9AB7UR/tab7WUjzConVE7Z3e0me6', 'NUTRICIONISTA', false, true);

-- 4) Un usuario por alumno (email = usuario, DNI = contraseña). Diego y Paula están dados de baja.
INSERT INTO cuentas (email, password, rol, debe_cambiar_password, activa, fecha_baja) VALUES
    ('lucia.gomez@mail.com',      '$2a$10$aI2uc7/XXsf9gB5t/GOp5uQ8clB7doGl0ULbmxQdkKPMBvGRSctZ2', 'ALUMNO', true, true,  NULL),
    ('martin.fernandez@mail.com', '$2a$10$MTmP7kbVp4vA/ySMBFU/z.6C0o1qABN41kfbwvf1JRuelGlb78KQy', 'ALUMNO', true, true,  NULL),
    ('sofia.ruiz@mail.com',       '$2a$10$WOQN9UA6cfw09Mg9ibtl2u8OaLwsFYp/Bl8Qs/YuecYzW8URLdbWO', 'ALUMNO', true, true,  NULL),
    ('tomas.diaz@mail.com',       '$2a$10$Yd4KszCe.zguaocRHVXIe.4OLwtS4mmajlHwKISmz7ujRpElZmug.', 'ALUMNO', true, true,  NULL),
    ('valentina.lopez@mail.com',  '$2a$10$NIoLsLEAT6PK52UsPeTu0e81v8NYWjUt0hzBELEbLvf9LiayRE7AO', 'ALUMNO', true, true,  NULL),
    ('joaquin.sosa@mail.com',     '$2a$10$NoA3vbAlUOjI.KymZISaAOAK1pBtnvWiguRlPEGe.id/tmKo7Ps9K', 'ALUMNO', true, true,  NULL),
    ('camila.torres@mail.com',    '$2a$10$MUpsE3vUmBA4AulEQAFG7.pPtOEyou9FktlIF4Fw77tEDyDzRK3wa', 'ALUMNO', true, true,  NULL),
    ('bruno.acosta@mail.com',     '$2a$10$Pwo3DhchsxaNDUkICg0glufuOWM4V/zdqddr2k0HwXPKy8IdYN3g2', 'ALUMNO', true, true,  NULL),
    ('agustina.romero@mail.com',  '$2a$10$5tz7hQmdB0I8NoWujqljDOAr/10sB6f1KCXUK0JnP8xEGrUtzkS9m', 'ALUMNO', true, true,  NULL),
    ('nicolas.benitez@mail.com',  '$2a$10$W2qYirAjvwejfQwOIaphI./kwUUzxNQYshzvdrhHsdf3DHMW16i9a', 'ALUMNO', true, true,  NULL),
    ('diego.molina@mail.com',     '$2a$10$AV9cayugtuzVtpmQD9FyluPJZkyqCCN3k77Kog.ulDTe303lcGvl2', 'ALUMNO', true, false, CURRENT_DATE - 2),
    ('paula.herrera@mail.com',    '$2a$10$Ca8yHVGSAylgiNVVxvlmVe1MOMBuE1jItdlbkFpB7KHnA5x.mGtD6', 'ALUMNO', true, false, CURRENT_DATE - 15);

-- 5) Alumnos (todos mayores de 16, DNI y email únicos, con sexo: es obligatorio)
INSERT INTO alumnos (nombre, apellido, edad, telefono, sexo, dni, email, fecha_nacimiento, objetivo, cuestionario_salud, cuenta_id)
SELECT a.nombre, a.apellido, date_part('year', age(CURRENT_DATE, a.nacimiento))::int, a.telefono, a.sexo, a.dni,
       a.email, a.nacimiento, a.objetivo, a.salud, c.id
FROM (VALUES
    ('Lucía',     'Gómez',     DATE '1983-04-12', '3415551111', 'Femenino',  '30111222', 'lucia.gomez@mail.com',      'Bajar de peso',       NULL),
    ('Martín',    'Fernández', DATE '1990-09-03', '3415552222', 'Masculino', '35222333', 'martin.fernandez@mail.com', 'Ganar masa muscular', 'Lesión de rodilla en 2022'),
    ('Sofía',     'Ruiz',      DATE '1995-01-25', '3415553333', 'Femenino',  '38333444', 'sofia.ruiz@mail.com',       NULL,                  NULL),
    ('Tomás',     'Díaz',      DATE '1998-07-30', '3415554444', 'Masculino', '40444555', 'tomas.diaz@mail.com',       'Mejorar resistencia', NULL),
    ('Valentina', 'López',     DATE '2001-11-18', '3415555555', 'Femenino',  '42555666', 'valentina.lopez@mail.com',  NULL,                  'Asma leve'),
    ('Joaquín',   'Sosa',      DATE '1992-02-14', '3415556666', 'Masculino', '36111000', 'joaquin.sosa@mail.com',     'Ganar fuerza',        NULL),
    ('Camila',    'Torres',    DATE '1994-06-21', '3415557777', 'Femenino',  '37222000', 'camila.torres@mail.com',    'Tonificar',           NULL),
    ('Bruno',     'Acosta',    DATE '1996-10-05', '3415558888', 'Masculino', '39333000', 'bruno.acosta@mail.com',     NULL,                  NULL),
    ('Agustina',  'Romero',    DATE '2000-03-09', '3415559999', 'Femenino',  '41444000', 'agustina.romero@mail.com',  'Mejorar postura',     'Escoliosis leve'),
    ('Nicolás',   'Benítez',   DATE '1988-12-01', '3415550000', 'Masculino', '33555000', 'nicolas.benitez@mail.com',  'Preparación física',  NULL),
    ('Diego',     'Molina',    DATE '1989-08-17', '3415551212', 'Masculino', '34666000', 'diego.molina@mail.com',     NULL,                  NULL),
    ('Paula',     'Herrera',   DATE '2002-05-28', '3415553434', 'Femenino',  '43777000', 'paula.herrera@mail.com',    'Bajar de peso',       NULL)
) AS a(nombre, apellido, nacimiento, telefono, sexo, dni, email, objetivo, salud)
JOIN cuentas c ON c.email = a.email;

-- 6) Inscripciones. Las ACTIVA tienen fecha_fin NULL. Cada alumno tiene como mucho una por estado, así las cuotas y
--    los pagos de abajo la encuentran por (DNI, estado).
--    Joaquín: la ACTIVA (x4) sigue hasta su vencimiento y la PROGRAMADA (x5) empieza ese día.
--    Nicolás: la de x3 terminó cuando cambió a x5 (hace 3 días).
--    Diego y Paula: canceladas el día de su baja.
INSERT INTO inscripciones (fecha_inicio, fecha_fin, estado, alumno_id, membresia_id)
SELECT CURRENT_DATE + i.inicio, CURRENT_DATE + i.fin, i.estado, a.id, m.id
FROM (VALUES
    ('30111222', 'x4',  -70, NULL::int, 'ACTIVA'),
    ('35222333', 'x5',  -27, NULL,      'ACTIVA'),
    ('38333444', 'x3',  -31, NULL,      'ACTIVA'),
    ('40444555', 'x4',  -40, NULL,      'ACTIVA'),
    ('42555666', 'x5',    0, NULL,      'ACTIVA'),
    ('36111000', 'x4',  -26, NULL,      'ACTIVA'),
    ('36111000', 'x5',    4, NULL,      'PROGRAMADA'),
    ('37222000', 'x3',  -62, NULL,      'ACTIVA'),
    ('39333000', 'x4',  -32, NULL,      'ACTIVA'),
    ('41444000', 'x3',  -22, NULL,      'ACTIVA'),
    ('33555000', 'x3',  -10, -3,        'CANCELADA'),
    ('33555000', 'x5',   -3, NULL,      'ACTIVA'),
    ('34666000', 'x4',  -20, -2,        'CANCELADA'),
    ('43777000', 'x5',  -50, -15,       'CANCELADA')
) AS i(dni, membresia, inicio, fin, estado)
JOIN alumnos a ON a.dni = i.dni
JOIN membresias m ON m.nombre = i.membresia;

-- 7) Cuotas: períodos ya pagados (inicio → vencimiento), con su precio sin recargo. Son de 30 días, salvo la
--    diferencia del cambio de membresía de Nicolás, que termina el mismo día que la cuota original ($38.000 −
--    $18.000 = $20.000). La situación de cada alumno se calcula con su última cuota (activa o programada).
INSERT INTO cuotas (inscripcion_id, fecha_inicio, fecha_vencimiento, precio)
SELECT i.id, CURRENT_DATE + c.inicio, CURRENT_DATE + c.fin, c.precio
FROM (VALUES
    ('30111222', 'ACTIVA',     -70, -40, 25000),
    ('30111222', 'ACTIVA',     -35,  -5, 25000),
    ('35222333', 'ACTIVA',     -27,   3, 38000),
    ('38333444', 'ACTIVA',     -31,  -1, 18000),
    ('40444555', 'ACTIVA',     -40, -10, 25000),
    ('40444555', 'ACTIVA',     -10,  20, 25000),
    ('42555666', 'ACTIVA',       0,  30, 38000),
    ('36111000', 'ACTIVA',     -26,   4, 25000),
    ('36111000', 'PROGRAMADA',   4,  34, 38000),
    ('37222000', 'ACTIVA',     -62, -32, 18000),
    ('39333000', 'ACTIVA',     -32,  -2, 25000),
    ('41444000', 'ACTIVA',     -22,   8, 18000),
    ('33555000', 'CANCELADA',  -10,  20, 18000),
    ('33555000', 'ACTIVA',      -3,  20, 20000),
    ('34666000', 'CANCELADA',  -20,  10, 25000),
    ('43777000', 'CANCELADA',  -50, -20, 38000)
) AS c(dni, estado, inicio, fin, precio)
JOIN alumnos a ON a.dni = c.dni
JOIN inscripciones i ON i.alumno_id = a.id AND i.estado = c.estado;

-- 8) Un pago por cuota. El monto es el total (precio + recargo).
--    Lucía renovó en mora (hace 35 días, 5 días después del vencimiento): recargo del 10% redondeado a la decena
--    hacia arriba ($25.000 → total $27.500, recargo $2.500).
--    Joaquín pagó ayer el mes siguiente con x5 (queda programado).
INSERT INTO pagos (cuota_id, monto, recargo, medio_de_pago, fecha_pago)
SELECT cu.id, cu.precio + p.recargo, p.recargo, p.medio, CURRENT_DATE + p.fecha_pago
FROM (VALUES
    ('30111222', 'ACTIVA',     -70, 0,    'EFECTIVO',      -70),
    ('30111222', 'ACTIVA',     -35, 2500, 'TRANSFERENCIA', -35),
    ('35222333', 'ACTIVA',     -27, 0,    'TRANSFERENCIA', -27),
    ('38333444', 'ACTIVA',     -31, 0,    'EFECTIVO',      -31),
    ('40444555', 'ACTIVA',     -40, 0,    'EFECTIVO',      -40),
    ('40444555', 'ACTIVA',     -10, 0,    'TRANSFERENCIA', -12),
    ('42555666', 'ACTIVA',       0, 0,    'EFECTIVO',        0),
    ('36111000', 'ACTIVA',     -26, 0,    'EFECTIVO',      -26),
    ('36111000', 'PROGRAMADA',   4, 0,    'TRANSFERENCIA',  -1),
    ('37222000', 'ACTIVA',     -62, 0,    'EFECTIVO',      -62),
    ('39333000', 'ACTIVA',     -32, 0,    'TRANSFERENCIA', -32),
    ('41444000', 'ACTIVA',     -22, 0,    'EFECTIVO',      -22),
    ('33555000', 'CANCELADA',  -10, 0,    'EFECTIVO',      -10),
    ('33555000', 'ACTIVA',      -3, 0,    'TRANSFERENCIA',  -3),
    ('34666000', 'CANCELADA',  -20, 0,    'EFECTIVO',      -20),
    ('43777000', 'CANCELADA',  -50, 0,    'TRANSFERENCIA', -50)
) AS p(dni, estado, inicio, recargo, medio, fecha_pago)
JOIN alumnos a ON a.dni = p.dni
JOIN inscripciones i ON i.alumno_id = a.id AND i.estado = p.estado
JOIN cuotas cu ON cu.inscripcion_id = i.id AND cu.fecha_inicio = CURRENT_DATE + p.inicio;

-- 9) Franjas horarias: lunes a viernes, mañana y tarde
INSERT INTO franjas_horarias (dia_semana, hora_inicio, hora_fin)
SELECT d.dia, f.inicio, f.fin
FROM (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia)
CROSS JOIN (VALUES (TIME '08:00', TIME '12:00'), (TIME '17:00', TIME '21:00')) AS f(inicio, fin);
