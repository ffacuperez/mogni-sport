import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/membresias';

const formatCodigo = (id) => `MB${String(id).padStart(4, '0')}`;

// Backend -> forma que usan las pantallas (dias y precio se manejan como texto en los inputs).
// descripcion no se edita en pantalla, pero se conserva para devolverla al guardar.
const fromApi = (m) => ({
  id: m.id,
  codigo: formatCodigo(m.id),
  nombre: m.nombre,
  dias: String(m.frecuenciaSemanal),
  precio: String(m.precioActual),
  descripcion: m.descripcion ?? null,
});

// Formulario -> body que espera el backend (MembresiaRequest)
const toApi = (form) => ({
  nombre: form.nombre,
  frecuenciaSemanal: Number(form.dias),
  precioActual: Number(String(form.precio).replace(',', '.')),
  descripcion: form.descripcion ?? null,
});

export async function listarMembresias() {
  const data = await request(BASE_PATH);
  return data.map(fromApi);
}

export async function obtenerMembresia(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearMembresia(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: toApi(form) }));
}

export async function actualizarMembresia(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: toApi(form) }));
}

export async function eliminarMembresia(id) {
  await request(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}
