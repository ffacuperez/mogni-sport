const TOKEN_KEY = 'mogni.token';

function leerPayload(token) {
  const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(atob(payload));
}

function isExpired(token) {
  try {
    const { exp } = leerPayload(token);
    return !exp || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/** Devuelve el token guardado, o null si no hay o ya expiró (en ese caso lo descarta). */
export function getToken() {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) return null;
  if (isExpired(token)) {
    clearToken();
    return null;
  }
  return token;
}

/** Rol que el backend firmó en el token (PROPIETARIO, ENTRENADOR, NUTRICIONISTA o ALUMNO), o null. */
export function getRolFromToken() {
  const token = getToken();
  if (!token) return null;
  try {
    return leerPayload(token).rol ?? null;
  } catch {
    return null;
  }
}
