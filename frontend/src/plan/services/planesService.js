import { request } from '../../shared/services/httpClient';
import { formularioAApi, sesionesFromApi } from '../../plantilla/services/plantillasService';

// HU-266: planes de entrenamiento asignados a alumnos (propietario y entrenador)

const BASE_PATH = '/planes-asignados';

const fromApi = (p) => ({
  id: p.id,
  alumnoId: p.alumnoId,
  alumnoNombre: p.alumnoNombre,
  nombre: p.nombre,
  nivel: p.nivel,
  objetivo: p.objetivo,
  estado: p.estado,
  plantillaOrigenId: p.plantillaOrigenId ?? null,
  fechaAsignacion: p.fechaAsignacion,
  fechaFinalizacion: p.fechaFinalizacion ?? null,
  sesiones: sesionesFromApi(p.sesiones),
});

/** El plan activo del alumno, o null si no tiene (el backend responde 204). */
export async function obtenerPlanActivo(alumnoId) {
  const data = await request(`${BASE_PATH}/activo?alumnoId=${alumnoId}`);
  return data ? fromApi(data) : null;
}

export async function obtenerPlan(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

/** Copia la plantilla para el alumno. El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdePlantilla(alumnoId, plantillaId) {
  return fromApi(await request(`${BASE_PATH}/desde-plantilla`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), plantillaId: Number(plantillaId) },
  }));
}

/** Plan armado con el mismo formulario que una plantilla. El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdeCero(alumnoId, form) {
  return fromApi(await request(`${BASE_PATH}/desde-cero`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), ...formularioAApi(form) },
  }));
}

/** Reemplaza los datos y sesiones del plan; la plantilla de la que salió no cambia. El backend responde 409 si está finalizado. */
export async function actualizarPlan(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: formularioAApi(form) }));
}

export async function finalizarPlan(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/finalizar`, { method: 'PUT' }));
}

/** "2026-10-12" → "12 de octubre de 2026". */
export function textoFecha(fechaIso) {
  if (!fechaIso) return '';
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-AR', { day: 'numeric', month: 'long', year: 'numeric' });
}
