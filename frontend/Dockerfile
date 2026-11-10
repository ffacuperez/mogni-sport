# Imagen del front: se compila con Node y queda servido por Caddy (ver Caddyfile en la raíz del repo),
# que además pasa /api al backend.

# --- 1. Compilar ---
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
# Mismo dominio que el back: Caddy recibe /api/... y se lo pasa al backend sin el /api
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# --- 2. Imagen final ---
FROM caddy:2-alpine
COPY --from=build /app/dist /srv
