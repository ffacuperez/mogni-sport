import { getToken } from './tokenStorage';

// Backend deployado en Railway (fijo: no depende de variables de entorno)
export const API_URL = 'https://mogni-sport-production.up.railway.app';

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

export async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = auth ? getToken() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Intente nuevamente.', 0);
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
