# Backend — Mogni Sport API

## Probar en la compu (localhost)

Sin Neon ni Railway: una base Postgres local y el front apuntando a `localhost:8080`.

**Requisitos:** Docker (para la base), Java 21 y Node.

1. **Base de datos** (desde `backend/system-backend`):
   ```bash
   docker compose up -d
   ```
2. **Backend, la primera vez** (crea las tablas y carga `data.sql`; **borra todo lo que haya en esa base**):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,localhost,seed
   ```
   Las siguientes veces, **sin `seed`** (si no, cada arranque vuelve a borrar todo):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,localhost
   ```
   En Windows: `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,localhost"`.
3. **Frontend** (desde `frontend`):
   ```bash
   npm install
   npm run dev:local
   ```
   y abrir http://localhost:5173.

**Usuarios** (de `data.sql`):

| Perfil | Email | Contraseña |
| --- | --- | --- |
| Propietario | admin@mogni.com | admin123 |
| Entrenador | entrenador@mogni.com | entrenador123 |

### Probar la HU-262 «Registrar asistencia»

Con el perfil `localhost`, al arrancar el backend se crean turnos de prueba con 4 alumnos anotados
(`DatosDemoAsistencia`, solo existe en este perfil):

- **Hoy, turno de la hora actual** (en curso): un presente, un ausente y dos sin marcar → se puede marcar y corregir.
- **Hoy, turno anterior** (ya terminó): todavía se puede marcar hasta el final del día.
- **Hoy, turno siguiente**: todavía no comenzó → no deja marcar.
- **Ayer a las 18:00**: un presente y tres sin marcar → al abrir la agenda pasan a «Ausente» (cierre del día).

Si se reinicia el backend en otra hora, se agregan los turnos de esa hora; los que ya tienen reservas no se tocan.
Para empezar de cero: `docker compose down -v` y volver al paso 1.
