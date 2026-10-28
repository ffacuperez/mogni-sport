import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/ejercicios';

const fromApi = (e) => ({
  id: e.id,
  nombre: e.nombre,
  grupoMuscular: e.grupoMuscular,
  descripcion: e.descripcion ?? '',
  activo: e.activo,
});

const toApi = (form) => ({
  nombre: form.nombre,
  grupoMuscular: form.grupoMuscular,
  descripcion: form.descripcion ?? null,
  activo: form.activo ?? true,
});

export async function listarEjercicios() {
  const data = await request(BASE_PATH);
  return data.map(fromApi);
}

export async function obtenerEjercicio(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearEjercicio(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: toApi(form) }));
}

export async function actualizarEjercicio(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: toApi(form) }));
}

export async function toggleEstadoEjercicio(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/estado`, { method: 'PATCH' }));
}

export async function eliminarEjercicio(id) {
  await request(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}