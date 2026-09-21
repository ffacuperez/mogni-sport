import { ApiError, request } from '../../shared/services/httpClient';
import { clearToken, getToken, saveToken } from '../../shared/services/tokenStorage';

// Este panel es para el personal del gimnasio; los alumnos no tienen acceso a la gestión.
const ROLES_PERMITIDOS = ['PROPIETARIO', 'ENTRENADOR', 'NUTRICIONISTA'];

export async function login(email, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  });

  if (!ROLES_PERMITIDOS.includes(data.rol)) {
    throw new ApiError('Esta cuenta no tiene acceso al panel de gestión.', 403);
  }

  saveToken(data.token);
  return data;
}

export function logout() {
  clearToken();
}

export function hasSession() {
  return getToken() !== null;
}
