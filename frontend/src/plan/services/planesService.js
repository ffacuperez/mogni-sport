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
  // HU-266: último día del plan (inclusiva). null en los planes asignados antes de esta regla
  fechaFin: p.fechaFin ?? null,
  cantidadSemanas: p.cantidadSemanas ?? null,
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

/** Copia la plantilla para el alumno. fechaFin: "2026-12-31". El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdePlantilla(alumnoId, plantillaId, fechaFin) {
  return fromApi(await request(`${BASE_PATH}/desde-plantilla`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), plantillaId: Number(plantillaId), fechaFin },
  }));
}

/** Plan armado con el mismo formulario que una plantilla. El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdeCero(alumnoId, form, fechaFin) {
  return fromApi(await request(`${BASE_PATH}/desde-cero`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), ...formularioAApi(form), fechaFin },
  }));
}

/**
 * Reemplaza los datos, sesiones y fecha fin del plan; la plantilla de la que salió no cambia.
 * El backend responde 409 si está finalizado o vencido.
 */
export async function actualizarPlan(id, form, fechaFin) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: { ...formularioAApi(form), fechaFin } }));
}

// --- HU-266: fecha fin ---

/** Hoy en Argentina, "2026-11-03" (la fecha de inicio de un plan que se asigna ahora). */
export const hoyIso = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Argentina/Buenos_Aires' });

/** "2026-11-03" + n días → "2026-11-04". */
export function sumarDias(fechaIso, dias) {
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

/** Último día de la última semana de un plan de n semanas que empieza ese día. */
export const fechaFinPorSemanas = (inicioIso, semanas) => sumarDias(inicioIso, semanas * 7 - 1);

/**
 * Mismas reglas que el backend, para avisar antes de enviar. Al asignar, el inicio es hoy, así que la fecha fin va
 * de mañana en adelante; al modificar puede ser hoy, pero siempre posterior al inicio del plan.
 */
export function validarFechaFin(fechaFin, { inicio, hoy, alModificar = false }) {
  if (!fechaFin) return 'La fecha fin es obligatoria.';
  if (fechaFin <= inicio) return 'La fecha fin tiene que ser posterior a la fecha de inicio.';
  if (alModificar && fechaFin < hoy) return 'La fecha fin no puede ser anterior a hoy.';
  return null;
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
