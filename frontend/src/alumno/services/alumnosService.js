import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/alumnos';

const ESTADOS_INSCRIPCION = { ACTIVA: 'Activa', VENCIDA: 'Vencida', CANCELADA: 'Cancelada' };

// Inscripción activa o, si no tiene, la última (vencida o cancelada). null si nunca tuvo.
const inscripcionFromApi = (i) => (i ? {
  membresiaId: i.membresiaId,
  membresia: i.membresiaNombre,
  estado: ESTADOS_INSCRIPCION[i.estado] ?? i.estado,
  fechaInicio: i.fechaInicio ?? '',
  fechaFin: i.fechaFin ?? '',
} : null);

/** Texto de la membresía para mostrar: el nombre, con el estado si la inscripción no está activa. */
export function describirMembresia(alumno) {
  const inscripcion = alumno.inscripcion;
  if (!inscripcion) return 'Sin membresía';
  return inscripcion.estado === 'Activa' ? inscripcion.membresia : `${inscripcion.membresia} (${inscripcion.estado})`;
}

// Backend -> forma que usan las pantallas
const fromApi = (a) => ({
  id: a.id,
  nombre: a.nombre,
  apellido: a.apellido,
  edad: a.edad,
  telefono: a.telefono ?? '',
  sexo: a.sexo ?? '',
  dni: a.dni,
  email: a.email,
  fechaNacimiento: a.fechaNacimiento ?? '',
  // Fecha de alta = inicio de la inscripción activa (la usa el cálculo de cuotas)
  fechaIngreso: a.fechaInicioInscripcion ?? '',
  fechaVencimiento: a.fechaFinInscripcion ?? '',
  objetivo: a.objetivo ?? '',
  antecedentes: a.cuestionarioSalud ?? '',
  membresiaId: a.membresiaId ?? '',
  membresia: a.membresiaNombre ?? 'Sin membresía',
  estado: a.activo ? 'Activo' : 'Inactivo',
  inscripcion: inscripcionFromApi(a.inscripcionActual),
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

/** Sin parámetro el backend devuelve solo activos; { activo: false } trae los dados de baja. */
export async function listarAlumnos({ activo } = {}) {
  const path = activo === undefined ? BASE_PATH : `${BASE_PATH}?activo=${activo}`;
  const data = await request(path);
  return data.map(fromApi);
}

/** Datos del alumno logueado (solo para cuentas con rol ALUMNO). */
export async function obtenerMiPerfil() {
  return fromApi(await request(`${BASE_PATH}/me`));
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
