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
TRUNCATE TABLE pagos, cuotas, inscripciones, alumnos, membresias, cuentas, franjas_horarias, ejercicios, plantillas_plan RESTART IDENTITY CASCADE;

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

-- 7) Cuotas: períodos ya pagados, con su precio sin recargo. Son mensuales: vencen el mismo día del mes siguiente al
--    inicio (del 13 al 13), así cada alumno paga siempre el mismo día. Cada fila fija el día de vencimiento que
--    define el caso (ancla, en días desde hoy) y cuántos meses antes de él empieza el período (meses: -1 = la cuota
--    que vence en el ancla, -2 = la anterior, 0 = la siguiente). Así las cuotas encadenadas quedan pegadas.
INSERT INTO cuotas (inscripcion_id, fecha_inicio, fecha_vencimiento, precio)
SELECT i.id,
       (CURRENT_DATE + c.ancla + make_interval(months => c.meses))::date,
       (CURRENT_DATE + c.ancla + make_interval(months => c.meses + 1))::date,
       c.precio
FROM (VALUES
    ('30111222', 'ACTIVA',     -40, -1, 25000),  -- Lucía: la 1ª venció hace 40 días...
    ('30111222', 'ACTIVA',      -5, -1, 25000),  -- ...y la 2ª la pagó en mora: empezó el día que pagó
    ('35222333', 'ACTIVA',       3, -1, 38000),
    ('38333444', 'ACTIVA',      -1, -1, 18000),
    ('40444555', 'ACTIVA',      20, -2, 25000),  -- Tomás: dos cuotas encadenadas
    ('40444555', 'ACTIVA',      20, -1, 25000),
    ('42555666', 'ACTIVA',       0,  0, 38000),  -- Valentina: alta hoy
    ('36111000', 'ACTIVA',       4, -1, 25000),
    ('36111000', 'PROGRAMADA',   4,  0, 38000),  -- Joaquín: el mes siguiente, ya pagado con x5
    ('37222000', 'ACTIVA',     -32, -1, 18000),
    ('39333000', 'ACTIVA',      -2, -1, 25000),
    ('41444000', 'ACTIVA',       8, -1, 18000),
    ('33555000', 'CANCELADA',   20, -1, 18000),
    ('34666000', 'CANCELADA',   10, -1, 25000),
    ('43777000', 'CANCELADA',  -20, -1, 38000)
) AS c(dni, estado, ancla, meses, precio)
JOIN alumnos a ON a.dni = c.dni
JOIN inscripciones i ON i.alumno_id = a.id AND i.estado = c.estado;

--    La diferencia del cambio de membresía de Nicolás va de hace 3 días al mismo vencimiento que la cuota original
--    ($38.000 − $18.000 = $20.000).
INSERT INTO cuotas (inscripcion_id, fecha_inicio, fecha_vencimiento, precio)
SELECT i.id, CURRENT_DATE - 3, CURRENT_DATE + 20, 20000
FROM alumnos a
JOIN inscripciones i ON i.alumno_id = a.id AND i.estado = 'ACTIVA'
WHERE a.dni = '33555000';

--    Cada inscripción empieza con su primera cuota (las fechas de arriba dependen de cuántos días tiene cada mes).
UPDATE inscripciones i
SET fecha_inicio = (SELECT min(cu.fecha_inicio) FROM cuotas cu WHERE cu.inscripcion_id = i.id)
WHERE EXISTS (SELECT 1 FROM cuotas cu WHERE cu.inscripcion_id = i.id);

-- 8) Un pago por cuota. El monto es el total (precio + recargo). Cada pago se identifica por el número de cuota
--    dentro de su inscripción (1 = la más vieja) y su fecha va en días desde el inicio de esa cuota.
--    Lucía renovó en mora (pocos días después del vencimiento): recargo del 10% redondeado a la decena hacia arriba
--    ($25.000 → total $27.500, recargo $2.500). Tomás pagó la 2ª dos días antes de que empezara.
--    Joaquín pagó ayer el mes siguiente con x5 (queda programado; empieza dentro de 4 días).
INSERT INTO pagos (cuota_id, monto, recargo, medio_de_pago, fecha_pago)
SELECT cu.id, cu.precio + p.recargo, p.recargo, p.medio, cu.fecha_inicio + p.dias_desde_inicio
FROM (VALUES
    ('30111222', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('30111222', 'ACTIVA',     2, 2500, 'TRANSFERENCIA',  0),
    ('35222333', 'ACTIVA',     1, 0,    'TRANSFERENCIA',  0),
    ('38333444', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('40444555', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('40444555', 'ACTIVA',     2, 0,    'TRANSFERENCIA', -2),
    ('42555666', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('36111000', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('36111000', 'PROGRAMADA', 1, 0,    'TRANSFERENCIA', -5),
    ('37222000', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('39333000', 'ACTIVA',     1, 0,    'TRANSFERENCIA',  0),
    ('41444000', 'ACTIVA',     1, 0,    'EFECTIVO',       0),
    ('33555000', 'CANCELADA',  1, 0,    'EFECTIVO',       0),
    ('33555000', 'ACTIVA',     1, 0,    'TRANSFERENCIA',  0),
    ('34666000', 'CANCELADA',  1, 0,    'EFECTIVO',       0),
    ('43777000', 'CANCELADA',  1, 0,    'TRANSFERENCIA',  0)
) AS p(dni, estado, numero, recargo, medio, dias_desde_inicio)
JOIN alumnos a ON a.dni = p.dni
JOIN inscripciones i ON i.alumno_id = a.id AND i.estado = p.estado
JOIN (SELECT c.*, row_number() OVER (PARTITION BY c.inscripcion_id ORDER BY c.fecha_inicio) AS numero
      FROM cuotas c) cu ON cu.inscripcion_id = i.id AND cu.numero = p.numero;

-- 9) Franjas horarias: lunes a viernes, mañana y tarde
INSERT INTO franjas_horarias (dia_semana, hora_inicio, hora_fin)
SELECT d.dia, f.inicio, f.fin
FROM (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia)
CROSS JOIN (VALUES (TIME '08:00', TIME '12:00'), (TIME '17:00', TIME '21:00')) AS f(inicio, fin);

-- 10) Catálogo de ejercicios: los más comunes de un gimnasio, por grupo muscular. Los grupos son los mismos que
--     ofrece la pantalla de Ejercicios (Piernas, Espalda, Pecho, Hombros, Brazos, Core y Glúteos). Dos están dados
--     de baja, para mostrar que no se ofrecen al armar planes.
INSERT INTO ejercicios (nombre, grupo_muscular, activo) VALUES
    -- Piernas
    ('Sentadilla libre con barra',          'Piernas', true),
    ('Prensa 45°',                          'Piernas', true),
    ('Sentadilla búlgara',                  'Piernas', true),
    ('Estocadas con mancuernas',            'Piernas', true),
    ('Extensión de cuádriceps en máquina',  'Piernas', true),
    ('Curl femoral acostado',               'Piernas', true),
    ('Peso muerto rumano',                  'Piernas', true),
    ('Aducción de cadera en máquina',       'Piernas', true),
    ('Elevación de talones de pie',         'Piernas', true),
    -- Glúteos
    ('Hip thrust con barra',                'Glúteos', true),
    ('Puente de glúteos',                   'Glúteos', true),
    ('Patada de glúteo en polea',           'Glúteos', true),
    ('Abducción de cadera en máquina',      'Glúteos', true),
    ('Peso muerto sumo',                    'Glúteos', true),
    ('Step-up al cajón',                    'Glúteos', true),
    -- Pecho
    ('Press plano con barra',               'Pecho', true),
    ('Press inclinado con mancuernas',      'Pecho', true),
    ('Press declinado con barra',           'Pecho', true),
    ('Aperturas con mancuernas',            'Pecho', true),
    ('Cruce de poleas',                     'Pecho', true),
    ('Pec deck',                            'Pecho', true),
    ('Fondos en paralelas',                 'Pecho', true),
    ('Flexiones de brazos',                 'Pecho', true),
    -- Espalda
    ('Dominadas',                           'Espalda', true),
    ('Jalón al pecho',                      'Espalda', true),
    ('Remo con barra',                      'Espalda', true),
    ('Remo con mancuerna a una mano',       'Espalda', true),
    ('Remo en polea baja',                  'Espalda', true),
    ('Remo en barra T',                     'Espalda', true),
    ('Pullover en polea',                   'Espalda', true),
    ('Peso muerto convencional',            'Espalda', true),
    -- Hombros
    ('Press militar con barra',             'Hombros', true),
    ('Press Arnold',                        'Hombros', true),
    ('Elevaciones laterales con mancuernas','Hombros', true),
    ('Elevaciones frontales con mancuernas','Hombros', true),
    ('Vuelos posteriores',                  'Hombros', true),
    ('Face pull',                           'Hombros', true),
    ('Encogimientos de hombros',            'Hombros', true),
    ('Remo al mentón con barra',            'Hombros', false),
    -- Brazos
    ('Curl con barra',                      'Brazos', true),
    ('Curl martillo',                       'Brazos', true),
    ('Curl concentrado con mancuerna',      'Brazos', true),
    ('Curl en banco Scott',                 'Brazos', true),
    ('Extensión de tríceps en polea',       'Brazos', true),
    ('Press francés con barra Z',           'Brazos', true),
    ('Extensión de tríceps tras nuca',      'Brazos', true),
    ('Fondos en banco',                     'Brazos', true),
    -- Core
    ('Plancha frontal',                     'Core', true),
    ('Plancha lateral',                     'Core', true),
    ('Crunch abdominal',                    'Core', true),
    ('Elevación de piernas colgado',        'Core', true),
    ('Rueda abdominal',                     'Core', true),
    ('Russian twist',                       'Core', true),
    ('Pallof press',                        'Core', true),
    ('Bicho muerto',                        'Core', true),
    ('Sit-up con disco',                    'Core', false);

-- 11) Plantillas de plan de entrenamiento: dos por nivel, una semana cada una (cada sesión es un día de entrenamiento).
--     La cantidad de sesiones sigue a las membresías: principiante 3 (x3), intermedio 4 (x4) y avanzado 5 (x5). Entre
--     las seis cubren los cuatro objetivos. Casos para mostrar:
--     - "Acondicionamiento y quema" tiene "Remo al mentón con barra", que se dio de baja después: la plantilla lo
--       conserva y lo marca "(dado de baja)", pero no se puede agregar a otras sesiones.
--     - "Circuito express 2 días" está inactiva: no se lista en Activas ni se puede asignar, y su nombre queda libre.
--       Se ve en la pestaña Inactivas, donde se puede reactivar (si no hay otra activa con su nombre).
--     - El peso sugerido está cargado solo en unos pocos ejercicios: lo normal es que el profe lo ajuste por alumno.
INSERT INTO plantillas_plan (nombre, nombre_unico, nivel, objetivo, activa, asignaciones_activas, fecha_actualizacion)
SELECT p.nombre, CASE WHEN p.activa THEN lower(p.nombre) END, p.nivel, p.objetivo, p.activa, 0,
       (now() AT TIME ZONE 'America/Argentina/Buenos_Aires') - make_interval(days => p.dias)
FROM (VALUES
    ('Full body inicial',                   'PRINCIPIANTE', 'HIPERTROFIA',      true,  40),
    ('Acondicionamiento y quema',           'PRINCIPIANTE', 'PERDIDA_DE_GRASA', true,  35),
    ('Torso / Pierna',                      'INTERMEDIO',   'HIPERTROFIA',      true,  30),
    ('Fuerza base 4 días',                  'INTERMEDIO',   'FUERZA',           true,  21),
    ('Push / Pull / Legs + Torso / Pierna', 'AVANZADO',     'HIPERTROFIA',      true,  14),
    ('Resistencia muscular 5 días',         'AVANZADO',     'RESISTENCIA',      true,   7),
    ('Circuito express 2 días',             'PRINCIPIANTE', 'RESISTENCIA',      false, 60)
) AS p(nombre, nivel, objetivo, activa, dias);

INSERT INTO sesiones_plantilla (plantilla_id, numero, nombre)
SELECT p.id, s.numero, s.nombre
FROM (VALUES
    ('Full body inicial', 1, 'Full body A'),
    ('Full body inicial', 2, 'Full body B'),
    ('Full body inicial', 3, 'Full body C'),
    ('Acondicionamiento y quema', 1, 'Tren inferior + core'),
    ('Acondicionamiento y quema', 2, 'Tren superior + core'),
    ('Acondicionamiento y quema', 3, 'Circuito full body'),
    ('Torso / Pierna', 1, 'Torso A'),
    ('Torso / Pierna', 2, 'Pierna A'),
    ('Torso / Pierna', 3, 'Torso B'),
    ('Torso / Pierna', 4, 'Pierna B'),
    ('Fuerza base 4 días', 1, 'Sentadilla'),
    ('Fuerza base 4 días', 2, 'Press banca'),
    ('Fuerza base 4 días', 3, 'Peso muerto'),
    ('Fuerza base 4 días', 4, 'Press militar'),
    ('Push / Pull / Legs + Torso / Pierna', 1, 'Push'),
    ('Push / Pull / Legs + Torso / Pierna', 2, 'Pull'),
    ('Push / Pull / Legs + Torso / Pierna', 3, 'Legs'),
    ('Push / Pull / Legs + Torso / Pierna', 4, 'Torso'),
    ('Push / Pull / Legs + Torso / Pierna', 5, 'Pierna + glúteos'),
    ('Resistencia muscular 5 días', 1, 'Pecho + tríceps'),
    ('Resistencia muscular 5 días', 2, 'Espalda + bíceps'),
    ('Resistencia muscular 5 días', 3, 'Piernas'),
    ('Resistencia muscular 5 días', 4, 'Hombros + core'),
    ('Resistencia muscular 5 días', 5, 'Glúteos + full body'),
    ('Circuito express 2 días', 1, 'Circuito A'),
    ('Circuito express 2 días', 2, 'Circuito B')
) AS s(plantilla, numero, nombre)
JOIN plantillas_plan p ON p.nombre = s.plantilla;

-- Ejercicios de cada sesión: series, repeticiones (mínimo y máximo si es un rango), descanso en minutos, peso sugerido
-- en kg, RIR e indicaciones. En las planchas, las "repeticiones" son segundos (lo aclara la indicación).
INSERT INTO ejercicios_sesion (sesion_id, orden, ejercicio_id, series, repeticiones, repeticiones_max, descanso_minutos,
                               peso_sugerido, rir_objetivo, indicaciones)
SELECT s.id, x.orden, e.id, x.series, x.reps, x.reps_max, x.descanso, x.peso, x.rir, x.indicaciones
FROM (VALUES
    -- Full body inicial (principiante, hipertrofia): 3 x 10-12, RIR 3
    ('Full body inicial', 1, 1, 'Sentadilla libre con barra',           3, 10, 12,   1.5,  40,   3, 'Primera serie de aproximación con la barra sola'),
    ('Full body inicial', 1, 2, 'Press plano con barra',                3, 10, 12,   1.5,  NULL, 3, NULL),
    ('Full body inicial', 1, 3, 'Jalón al pecho',                       3, 10, 12,   1.5,  NULL, 3, NULL),
    ('Full body inicial', 1, 4, 'Elevaciones laterales con mancuernas', 3, 12, 15,   1,    NULL, 3, NULL),
    ('Full body inicial', 1, 5, 'Plancha frontal',                      3, 30, NULL, 1,    NULL, NULL, '30 segundos por serie'),
    ('Full body inicial', 2, 1, 'Prensa 45°',                           3, 10, 12,   1.5,  NULL, 3, NULL),
    ('Full body inicial', 2, 2, 'Press inclinado con mancuernas',       3, 10, 12,   1.5,  NULL, 3, NULL),
    ('Full body inicial', 2, 3, 'Remo en polea baja',                   3, 10, 12,   1.5,  NULL, 3, NULL),
    ('Full body inicial', 2, 4, 'Curl con barra',                       3, 10, 12,   1,    NULL, 3, NULL),
    ('Full body inicial', 2, 5, 'Crunch abdominal',                     3, 15, 20,   1,    NULL, NULL, NULL),
    ('Full body inicial', 3, 1, 'Peso muerto rumano',                   3, 10, 12,   1.5,  NULL, 3, 'Tempo 3-1-1: bajar en 3 segundos'),
    ('Full body inicial', 3, 2, 'Flexiones de brazos',                  3, 8,  12,   1.5,  NULL, 3, 'Si cuesta, apoyar las rodillas'),
    ('Full body inicial', 3, 3, 'Remo con mancuerna a una mano',        3, 10, 12,   1.5,  NULL, 3, 'Por brazo'),
    ('Full body inicial', 3, 4, 'Extensión de tríceps en polea',        3, 12, 15,   1,    NULL, 3, NULL),
    ('Full body inicial', 3, 5, 'Puente de glúteos',                    3, 15, NULL, 1,    NULL, 3, NULL),
    -- Acondicionamiento y quema (principiante, pérdida de grasa): 3 x 12-15, descansos cortos
    ('Acondicionamiento y quema', 1, 1, 'Estocadas con mancuernas',     3, 12, NULL, 1,    NULL, 3, '12 por pierna'),
    ('Acondicionamiento y quema', 1, 2, 'Step-up al cajón',             3, 12, NULL, 1,    NULL, 3, '12 por pierna'),
    ('Acondicionamiento y quema', 1, 3, 'Sentadilla búlgara',           3, 10, 12,   1,    NULL, 3, 'Por pierna'),
    ('Acondicionamiento y quema', 1, 4, 'Elevación de talones de pie',  3, 15, 20,   0.75, NULL, 3, NULL),
    ('Acondicionamiento y quema', 1, 5, 'Russian twist',                3, 20, NULL, 0.75, NULL, NULL, 'Contar cada giro'),
    ('Acondicionamiento y quema', 2, 1, 'Flexiones de brazos',          3, 10, 15,   1,    NULL, 3, NULL),
    ('Acondicionamiento y quema', 2, 2, 'Remo en polea baja',           3, 12, 15,   1,    NULL, 3, NULL),
    ('Acondicionamiento y quema', 2, 3, 'Press Arnold',                 3, 12, 15,   1,    NULL, 3, NULL),
    ('Acondicionamiento y quema', 2, 4, 'Remo al mentón con barra',     3, 12, 15,   1,    NULL, 3, NULL),
    ('Acondicionamiento y quema', 2, 5, 'Fondos en banco',              3, 12, 15,   1,    NULL, 3, NULL),
    ('Acondicionamiento y quema', 2, 6, 'Plancha lateral',              3, 30, NULL, 0.75, NULL, NULL, '30 segundos de cada lado'),
    ('Acondicionamiento y quema', 3, 1, 'Prensa 45°',                   3, 15, NULL, 0.75, NULL, 3, 'Circuito: pasar al siguiente sin descanso y descansar al final de la vuelta'),
    ('Acondicionamiento y quema', 3, 2, 'Jalón al pecho',               3, 15, NULL, 0.75, NULL, 3, NULL),
    ('Acondicionamiento y quema', 3, 3, 'Hip thrust con barra',         3, 15, NULL, 0.75, NULL, 3, NULL),
    ('Acondicionamiento y quema', 3, 4, 'Face pull',                    3, 15, NULL, 0.75, NULL, 3, NULL),
    ('Acondicionamiento y quema', 3, 5, 'Bicho muerto',                 3, 12, NULL, 1.5,  NULL, NULL, '12 por lado'),
    -- Torso / Pierna (intermedio, hipertrofia): básicos 4 x 8-10, accesorios 3 x 10-12, RIR 2
    ('Torso / Pierna', 1, 1, 'Press plano con barra',                   4, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 1, 2, 'Remo con barra',                          4, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 1, 3, 'Press militar con barra',                 3, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 1, 4, 'Jalón al pecho',                          3, 10, 12,   1.5,  NULL, 2, NULL),
    ('Torso / Pierna', 1, 5, 'Curl martillo',                           3, 10, 12,   1,    12,   2, NULL),
    ('Torso / Pierna', 1, 6, 'Extensión de tríceps en polea',           3, 10, 12,   1,    NULL, 2, 'Superserie con el anterior'),
    ('Torso / Pierna', 2, 1, 'Sentadilla libre con barra',              4, 8,  10,   2.5,  NULL, 2, 'Primera serie de aproximación'),
    ('Torso / Pierna', 2, 2, 'Peso muerto rumano',                      4, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 2, 3, 'Extensión de cuádriceps en máquina',      3, 10, 12,   1.5,  NULL, 2, NULL),
    ('Torso / Pierna', 2, 4, 'Curl femoral acostado',                   3, 10, 12,   1.5,  NULL, 2, NULL),
    ('Torso / Pierna', 2, 5, 'Elevación de talones de pie',             4, 12, 15,   1,    NULL, 2, NULL),
    ('Torso / Pierna', 3, 1, 'Press inclinado con mancuernas',          4, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 3, 2, 'Dominadas',                               4, 6,  10,   2,    NULL, 2, 'Si no llegás a 6, usar banda elástica'),
    ('Torso / Pierna', 3, 3, 'Elevaciones laterales con mancuernas',    3, 12, 15,   1,    NULL, 2, NULL),
    ('Torso / Pierna', 3, 4, 'Remo en barra T',                         3, 8,  10,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 3, 5, 'Curl con barra',                          3, 10, 12,   1,    NULL, 2, NULL),
    ('Torso / Pierna', 3, 6, 'Press francés con barra Z',               3, 10, 12,   1,    NULL, 2, NULL),
    ('Torso / Pierna', 4, 1, 'Prensa 45°',                              4, 10, 12,   2,    NULL, 2, NULL),
    ('Torso / Pierna', 4, 2, 'Hip thrust con barra',                    4, 8,  10,   2,    NULL, 2, 'Pausa de 1 segundo arriba'),
    ('Torso / Pierna', 4, 3, 'Sentadilla búlgara',                      3, 10, 12,   1.5,  NULL, 2, 'Por pierna'),
    ('Torso / Pierna', 4, 4, 'Abducción de cadera en máquina',          3, 12, 15,   1,    NULL, 2, NULL),
    ('Torso / Pierna', 4, 5, 'Elevación de piernas colgado',            3, 10, 15,   1,    NULL, NULL, NULL),
    -- Fuerza base 4 días (intermedio, fuerza): principal 5 x 5 con 3 min de descanso, accesorios 3 x 8
    ('Fuerza base 4 días', 1, 1, 'Sentadilla libre con barra',          5, 5,  NULL, 3,    NULL, 2, 'Subir 2,5 kg por semana si se completan todas las series'),
    ('Fuerza base 4 días', 1, 2, 'Prensa 45°',                          3, 8,  NULL, 2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 1, 3, 'Curl femoral acostado',               3, 8,  10,   1.5,  NULL, 2, NULL),
    ('Fuerza base 4 días', 1, 4, 'Elevación de talones de pie',         3, 10, 12,   1,    NULL, 2, NULL),
    ('Fuerza base 4 días', 1, 5, 'Rueda abdominal',                     3, 8,  10,   1.5,  NULL, NULL, NULL),
    ('Fuerza base 4 días', 2, 1, 'Press plano con barra',               5, 5,  NULL, 3,    60,   2, 'Pausa de 1 segundo en el pecho'),
    ('Fuerza base 4 días', 2, 2, 'Press inclinado con mancuernas',      3, 8,  NULL, 2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 2, 3, 'Fondos en paralelas',                 3, 6,  8,    2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 2, 4, 'Press francés con barra Z',           3, 8,  10,   1.5,  NULL, 2, NULL),
    ('Fuerza base 4 días', 2, 5, 'Face pull',                           3, 12, 15,   1,    NULL, 2, NULL),
    ('Fuerza base 4 días', 3, 1, 'Peso muerto convencional',            5, 5,  NULL, 3,    NULL, 2, 'Apoyar la barra en el piso en cada repetición'),
    ('Fuerza base 4 días', 3, 2, 'Remo con barra',                      3, 8,  NULL, 2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 3, 3, 'Dominadas',                           3, 6,  8,    2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 3, 4, 'Hip thrust con barra',                3, 8,  NULL, 2,    NULL, 2, NULL),
    ('Fuerza base 4 días', 3, 5, 'Plancha frontal',                     3, 45, NULL, 1,    NULL, NULL, '45 segundos por serie'),
    ('Fuerza base 4 días', 4, 1, 'Press militar con barra',             5, 5,  NULL, 3,    NULL, 2, NULL),
    ('Fuerza base 4 días', 4, 2, 'Remo con mancuerna a una mano',       3, 8,  NULL, 1.5,  NULL, 2, 'Por brazo'),
    ('Fuerza base 4 días', 4, 3, 'Elevaciones laterales con mancuernas',3, 10, 12,   1,    NULL, 2, NULL),
    ('Fuerza base 4 días', 4, 4, 'Curl con barra',                      3, 8,  10,   1.5,  NULL, 2, NULL),
    ('Fuerza base 4 días', 4, 5, 'Pallof press',                        3, 10, NULL, 1,    NULL, NULL, '10 por lado'),
    -- Push / Pull / Legs + Torso / Pierna (avanzado, hipertrofia): 6 ejercicios por sesión, RIR 0-1
    ('Push / Pull / Legs + Torso / Pierna', 1, 1, 'Press plano con barra',                4, 6,  8,  2.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 1, 2, 'Press inclinado con mancuernas',       4, 8,  10, 2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 1, 3, 'Press militar con barra',              3, 6,  8,  2.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 1, 4, 'Elevaciones laterales con mancuernas', 4, 12, 15, 1,   NULL, 1, 'Última serie con drop set'),
    ('Push / Pull / Legs + Torso / Pierna', 1, 5, 'Cruce de poleas',                      3, 12, 15, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 1, 6, 'Extensión de tríceps en polea',        3, 10, 12, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 2, 1, 'Dominadas',                            4, 6,  8,  2.5, NULL, 1, 'Con lastre si se superan las 8'),
    ('Push / Pull / Legs + Torso / Pierna', 2, 2, 'Remo con barra',                       4, 6,  8,  2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 2, 3, 'Remo en polea baja',                   3, 10, 12, 1.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 2, 4, 'Pullover en polea',                    3, 12, 15, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 2, 5, 'Vuelos posteriores',                   3, 12, 15, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 2, 6, 'Curl con barra',                       4, 8,  10, 1.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 3, 1, 'Sentadilla libre con barra',           4, 6,  8,  3,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 3, 2, 'Prensa 45°',                           4, 10, 12, 2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 3, 3, 'Peso muerto rumano',                   3, 8,  10, 2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 3, 4, 'Extensión de cuádriceps en máquina',   3, 12, 15, 1,   NULL, 0, 'Última serie al fallo'),
    ('Push / Pull / Legs + Torso / Pierna', 3, 5, 'Curl femoral acostado',                3, 10, 12, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 3, 6, 'Elevación de talones de pie',          4, 12, 15, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 1, 'Press Arnold',                         4, 8,  10, 2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 2, 'Remo en barra T',                      4, 8,  10, 2,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 3, 'Pec deck',                             3, 12, 15, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 4, 'Jalón al pecho',                       3, 10, 12, 1.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 5, 'Curl martillo',                        3, 10, 12, 1,   16,   1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 4, 6, 'Press francés con barra Z',            3, 10, 12, 1,   NULL, 1, 'Superserie con el anterior'),
    ('Push / Pull / Legs + Torso / Pierna', 5, 1, 'Hip thrust con barra',                 4, 8,  10, 2,   100,  1, 'Pausa de 2 segundos arriba'),
    ('Push / Pull / Legs + Torso / Pierna', 5, 2, 'Sentadilla búlgara',                   3, 8,  10, 2,   NULL, 1, 'Por pierna'),
    ('Push / Pull / Legs + Torso / Pierna', 5, 3, 'Peso muerto sumo',                     4, 6,  8,  2.5, NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 5, 4, 'Abducción de cadera en máquina',       3, 15, 20, 1,   NULL, 1, NULL),
    ('Push / Pull / Legs + Torso / Pierna', 5, 5, 'Patada de glúteo en polea',            3, 12, 15, 1,   NULL, 1, 'Por pierna'),
    ('Push / Pull / Legs + Torso / Pierna', 5, 6, 'Elevación de piernas colgado',         3, 12, 15, 1,   NULL, NULL, NULL),
    -- Resistencia muscular 5 días (avanzado, resistencia): 3-4 x 15-20 con 45 segundos de descanso
    ('Resistencia muscular 5 días', 1, 1, 'Press plano con barra',                 4, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 1, 2, 'Aperturas con mancuernas',              3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 1, 3, 'Cruce de poleas',                       3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 1, 4, 'Flexiones de brazos',                   3, 15, 20, 0.75, NULL, 2, 'Si no se llega a 15, seguir hasta el fallo'),
    ('Resistencia muscular 5 días', 1, 5, 'Fondos en banco',                       3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 1, 6, 'Extensión de tríceps tras nuca',        3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 2, 1, 'Jalón al pecho',                        4, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 2, 2, 'Remo en polea baja',                    3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 2, 3, 'Remo con mancuerna a una mano',         3, 15, 20, 0.75, NULL, 2, 'Por brazo'),
    ('Resistencia muscular 5 días', 2, 4, 'Pullover en polea',                     3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 2, 5, 'Curl concentrado con mancuerna',        3, 15, 20, 0.75, NULL, 2, 'Por brazo'),
    ('Resistencia muscular 5 días', 2, 6, 'Curl en banco Scott',                   3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 3, 1, 'Prensa 45°',                            4, 20, NULL, 1,  80,   2, NULL),
    ('Resistencia muscular 5 días', 3, 2, 'Estocadas con mancuernas',              3, 15, NULL, 0.75, NULL, 2, 'Caminando, 15 por pierna'),
    ('Resistencia muscular 5 días', 3, 3, 'Extensión de cuádriceps en máquina',    3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 3, 4, 'Curl femoral acostado',                 3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 3, 5, 'Aducción de cadera en máquina',         3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 3, 6, 'Elevación de talones de pie',           4, 20, 25, 0.5,  NULL, 2, NULL),
    ('Resistencia muscular 5 días', 4, 1, 'Press Arnold',                          3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 4, 2, 'Elevaciones laterales con mancuernas',  3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 4, 3, 'Elevaciones frontales con mancuernas',  3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 4, 4, 'Encogimientos de hombros',              3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 4, 5, 'Plancha frontal',                       3, 45, NULL, 0.75, NULL, NULL, '45 segundos por serie'),
    ('Resistencia muscular 5 días', 4, 6, 'Russian twist',                         3, 20, NULL, 0.75, NULL, NULL, 'Contar cada giro'),
    ('Resistencia muscular 5 días', 5, 1, 'Hip thrust con barra',                  4, 15, 20, 1,    NULL, 2, NULL),
    ('Resistencia muscular 5 días', 5, 2, 'Puente de glúteos',                     3, 20, NULL, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 5, 3, 'Step-up al cajón',                      3, 15, NULL, 0.75, NULL, 2, 'Por pierna'),
    ('Resistencia muscular 5 días', 5, 4, 'Face pull',                             3, 15, 20, 0.75, NULL, 2, NULL),
    ('Resistencia muscular 5 días', 5, 5, 'Rueda abdominal',                       3, 10, 15, 1,    NULL, NULL, NULL),
    ('Resistencia muscular 5 días', 5, 6, 'Bicho muerto',                          3, 12, NULL, 0.75, NULL, NULL, 'Por lado'),
    -- Circuito express 2 días (inactiva): incluye "Sit-up con disco", también dado de baja
    ('Circuito express 2 días', 1, 1, 'Sentadilla libre con barra',   3, 15, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 1, 2, 'Flexiones de brazos',          3, 12, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 1, 3, 'Remo en polea baja',           3, 15, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 1, 4, 'Sit-up con disco',             3, 15, NULL, 0.5, NULL, NULL, NULL),
    ('Circuito express 2 días', 1, 5, 'Plancha frontal',              3, 30, NULL, 0.5, NULL, NULL, '30 segundos por serie'),
    ('Circuito express 2 días', 2, 1, 'Prensa 45°',                   3, 15, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 2, 2, 'Jalón al pecho',               3, 15, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 2, 3, 'Press Arnold',                 3, 15, NULL, 0.5, NULL, 3, NULL),
    ('Circuito express 2 días', 2, 4, 'Puente de glúteos',            3, 20, NULL, 0.5, NULL, 3, NULL)
) AS x(plantilla, sesion, orden, ejercicio, series, reps, reps_max, descanso, peso, rir, indicaciones)
JOIN plantillas_plan p ON p.nombre = x.plantilla
JOIN sesiones_plantilla s ON s.plantilla_id = p.id AND s.numero = x.sesion
JOIN ejercicios e ON e.nombre = x.ejercicio;
