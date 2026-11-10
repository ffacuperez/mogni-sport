# Imagen del backend en dos etapas: se compila con Maven y la imagen final solo lleva el JRE y el .jar.

# --- 1. Compilar ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Primero solo el pom: mientras no cambien las dependencias, Docker reutiliza esta capa y no las vuelve a bajar
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Los tests no corren acá: varios arrancan contra la base del perfil local. Se corren antes, con ./mvnw test.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/system-backend-*.jar app.jar

# --- 2. Imagen final ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sin root adentro del contenedor
RUN addgroup -S mogni && adduser -S mogni -G mogni
USER mogni

COPY --from=build /app/app.jar app.jar

# Solo afecta la hora de los logs: el código usa siempre la zona del gimnasio (ver ZonaHoraria)
ENV TZ=America/Argentina/Buenos_Aires
# El servidor tiene 4 GB para todo (base, Caddy y el back): el heap se limita al 40 % de la memoria
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
