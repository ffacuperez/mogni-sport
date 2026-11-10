import { getToken, saveToken } from './tokenStorage';

// Por defecto, el back de la compu. En el servidor, el Dockerfile del front lo cambia por /api (Caddy lo pasa al back).
// Para apuntar a otro back, crear frontend/.env.local con VITE_API_URL=... (los *.local no se commitean).
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let onUnauthorized = null;

/** Registra qué hacer cuando el backend responde 401 en una ruta protegida (token vencido o inválido). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

const FALLBACK_MESSAGES = {
  401: 'Su sesión expiró. Vuelva a iniciar sesión.',
  403: 'No tiene permisos para realizar esta acción.',
  404: 'No se encontró el recurso solicitado.',
};

let renovacionEnCurso = null;

/**
 * Pide un token de acceso nuevo con la cookie de sesión (el token de acceso dura 15 minutos; la sesión, semanas).
 * Devuelve true si lo consiguió. Si varios pedidos lo necesitan a la vez, comparten la misma renovación.
 */
export function renovarSesion() {
  renovacionEnCurso ??= intentarRenovar().finally(() => { renovacionEnCurso = null; });
  return renovacionEnCurso;
}

async function intentarRenovar() {
  // Un segundo intento cubre a otra pestaña que renovó justo antes: el navegador ya tiene la cookie nueva
  for (let intento = 0; intento < 2; intento++) {
    try {
      const response = await fetch(`${API_URL}/auth/refresh`, {
        method: 'POST',
        headers: { Accept: 'application/json' },
        credentials: 'include',
      });
      if (response.ok) {
        saveToken((await response.json()).token);
        return true;
      }
      if (response.status !== 401) return false;
    } catch {
      return false;
    }
  }
  return false;
}

async function enviar(path, method, body, auth) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = auth ? getToken() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  try {
    return await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      // La cookie de sesión viaja también cuando el front corre en otro puerto que el back (npm run dev)
      credentials: 'include',
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Intente nuevamente.', 0);
  }
}

export async function request(path, { method = 'GET', body, auth = true } = {}) {
  // Sin token de acceso vigente se pide uno nuevo antes de llamar; si el back igual responde 401, se renueva y se
  // reintenta una vez. Recién si la sesión no se puede renovar se vuelve al login.
  if (auth && !getToken()) await renovarSesion();
  let response = await enviar(path, method, body, auth);
  if (response.status === 401 && auth && await renovarSesion()) {
    response = await enviar(path, method, body, auth);
  }

  const data = response.status === 204 ? null : await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401 && auth) onUnauthorized?.();
    throw new ApiError(
      data?.error ?? FALLBACK_MESSAGES[response.status] ?? 'Ocurrió un error inesperado.',
      response.status
    );
  }

  return data;
}
