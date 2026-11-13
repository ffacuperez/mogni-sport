import { request } from '../../shared/services/httpClient';
import { formularioAApi, semanasFromApi } from '../../plantilla/services/plantillasService';

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
  // HU-266: último día del plan (inclusiva): el último día de la última semana
  fechaFin: p.fechaFin,
  cantidadSemanas: p.cantidadSemanas,
  // La semana en curso; null si el plan ya no está vigente
  semanaEnCurso: p.semanaEnCurso ?? null,
  semanas: semanasFromApi(p.semanas),
  // Sesiones de la semana en curso que el alumno ya empezó o registró: en el editor son de solo lectura
  sesionesConRegistro: (p.sesionesConRegistro ?? []).map(r => ({
    semana: r.semana, numeroSesion: r.numeroSesion, completada: r.completada, fecha: r.fecha,
  })),
});

/** El plan activo del alumno, o null si no tiene (el backend responde 204). */
export async function obtenerPlanActivo(alumnoId) {
  const data = await request(`${BASE_PATH}/activo?alumnoId=${alumnoId}`);
  return data ? fromApi(data) : null;
}

export async function obtenerPlan(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

/** Copia la plantilla, con todas sus semanas, para el alumno. El backend responde 409 si ya tiene un plan activo. */
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

/**
 * Manda el plan completo; la plantilla de la que salió no cambia. El backend deja como estaban las semanas que ya
 * pasaron y las sesiones que el alumno ya empezó o registró, y recalcula la fecha fin con la cantidad de semanas.
 * Responde 409 si está finalizado o vencido.
 */
export async function actualizarPlan(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: formularioAApi(form) }));
}

// --- HU-266: fechas del plan (la semana 1 empieza el día de la asignación) ---

/** Hoy en Argentina, "2026-11-03" (la fecha de inicio de un plan que se asigna ahora). */
export const hoyIso = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Argentina/Buenos_Aires' });

/** "2026-11-03" + n días → "2026-11-04". */
export function sumarDias(fechaIso, dias) {
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

/** Último día de la última semana de un plan de n semanas que empieza ese día. */
export const fechaFinPorSemanas = (inicioIso, semanas) => sumarDias(inicioIso, semanas * 7 - 1);

/** Primer y último día de la semana n (desde 1) de un plan que empieza ese día. */
export const diasDeLaSemana = (inicioIso, numero) => {
  const desde = sumarDias(inicioIso, (numero - 1) * 7);
  return { desde, hasta: sumarDias(desde, 6) };
};

export async function finalizarPlan(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/finalizar`, { method: 'PUT' }));
}

/** "2026-10-12" → "12 de octubre de 2026". */
export function textoFecha(fechaIso) {
  if (!fechaIso) return '';
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-AR', { day: 'numeric', month: 'long', year: 'numeric' });
}
