import { request } from '../../shared/services/httpClient';
import { clearToken, getRolFromToken, getToken, saveToken } from '../../shared/services/tokenStorage';

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
