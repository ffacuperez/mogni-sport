import { request } from '../../shared/services/httpClient';
import { clearToken, getDebeCambiarPasswordFromToken, getRolFromToken, getToken, saveToken } from '../../shared/services/tokenStorage';

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
}

export function hasSession() {
  return getToken() !== null;
}

export function getRol() {
  return getRolFromToken();
}

export function debeCambiarPassword() {
  return getDebeCambiarPasswordFromToken();
}

/** HU-248: el backend devuelve un token nuevo sin la marca de cambio pendiente. */
export async function cambiarPassword(passwordActual, passwordNueva) {
  const data = await request('/auth/cambiar-password', {
    method: 'POST',
    body: { passwordActual, passwordNueva },
  });
  saveToken(data.token);
  return data;
}
