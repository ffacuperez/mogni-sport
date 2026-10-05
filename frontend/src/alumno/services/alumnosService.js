import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/alumnos';

const formatCodigo = (id) => `MS${String(id).padStart(5, '0')}`;

// Backend -> forma que usan las pantallas
const fromApi = (a) => ({
  id: a.id,
  codigo: formatCodigo(a.id),
  nombre: a.nombre,
  apellido: a.apellido,
  edad: a.edad,
  telefono: a.telefono ?? '',
  sexo: a.sexo ?? '',
  dni: a.dni,
  email: a.email,
  fechaNacimiento: a.fechaNacimiento ?? '',
  objetivo: a.objetivo ?? '',
  antecedentes: a.cuestionarioSalud ?? '',
  membresiaId: a.membresiaId ?? '',
  membresia: a.membresiaNombre ?? 'Sin membresía',
  estado: a.activo ? 'Activo' : 'Inactivo',
});

// Formulario -> body que espera el backend (Registrar/ActualizarAlumnoRequest)
const toApi = (form) => ({
  nombre: form.nombre,
  apellido: form.apellido,
  dni: form.dni,
  email: form.email,
  fechaNacimiento: form.fechaNacimiento,
  telefono: form.telefono || null,
  sexo: form.sexo || null,
  membresiaId: form.membresiaId ? Number(form.membresiaId) : null,
  objetivo: form.objetivo || null,
  cuestionarioSalud: form.antecedentes || null,
});

export async function listarAlumnos() {
  const data = await request(BASE_PATH);
  return data.map(fromApi);
}

export async function obtenerAlumno(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearAlumno(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: toApi(form) }));
}

export async function actualizarAlumno(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: toApi(form) }));
}

/** Baja lógica: el alumno queda inactivo, no se borra. */
export async function darDeBajaAlumno(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'DELETE' }));
}

export async function reactivarAlumno(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/reactivar`, { method: 'PUT' }));
}
