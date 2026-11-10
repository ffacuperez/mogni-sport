import { renovarSesion, request } from '../../shared/services/httpClient';
import { clearToken, getDebeCambiarPasswordFromToken, getEmailFromToken, getRolFromToken, getToken, saveToken } from '../../shared/services/tokenStorage';

export const ROL_ALUMNO = 'ALUMNO';
export const ROLES_GESTION = ['PROPIETARIO', 'ENTRENADOR', 'NUTRICIONISTA'];

/** Cualquier rol puede iniciar sesión; qué pantalla ve cada uno lo decide App según el rol. */
export async function login(email, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  });

  saveToken(data.token);
  return data;
}

export function logout() {
  clearToken();
  // Cierra la sesión también en el servidor (la cookie dice cuál es); sin conexión, igual queda cerrada en este dispositivo
  request('/auth/logout', { method: 'POST', auth: false }).catch(() => {});
}

export function hasSession() {
  return getToken() !== null;
}

/**
 * Al abrir la app sin token de acceso vigente (vence a los 15 minutos): si la sesión del dispositivo sigue abierta,
 * trae uno nuevo con la cookie y el usuario entra sin volver a iniciar sesión. Devuelve si quedó con sesión.
 */
export async function restaurarSesion() {
  return hasSession() || renovarSesion();
}

export function getRol() {
  return getRolFromToken();
}

export function debeCambiarPassword() {
  return getDebeCambiarPasswordFromToken();
}

export function getEmail() {
  return getEmailFromToken();
}

/** Reglas de la contraseña nueva (las mismas que valida el backend). */
export const LARGO_MINIMO_PASSWORD = 8;

/**
 * HU-248 (primer inicio) y MOGNI-263 (desde el perfil). El backend devuelve un token nuevo sin la marca de cambio
 * pendiente: se guarda y la sesión sigue abierta.
 */
export async function cambiarPassword(passwordActual, passwordNueva, passwordConfirmacion) {
  const data = await request('/auth/cambiar-password', {
    method: 'POST',
    body: { passwordActual, passwordNueva, passwordConfirmacion },
  });
  saveToken(data.token);
  return data;
}

// --- Recuperar contraseña (sin sesión) ---

/** Pide el mail con el link para restablecer la contraseña. El backend responde igual exista o no el email. */
export async function solicitarRecuperacion(email) {
  return request('/auth/recuperar-password', { method: 'POST', body: { email }, auth: false });
}

/** Comprueba si el link del mail todavía sirve (falla con el motivo si venció o ya se usó). */
export async function validarLinkRecuperacion(token) {
  return request(`/auth/recuperar-password/validar?token=${encodeURIComponent(token)}`, { auth: false });
}

export async function restablecerPassword(token, passwordNueva, passwordConfirmacion) {
  return request('/auth/restablecer-password', {
    method: 'POST',
    body: { token, passwordNueva, passwordConfirmacion },
    auth: false,
  });
}
