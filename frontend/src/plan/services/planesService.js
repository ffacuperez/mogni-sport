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

/** Copia la plantilla para el alumno por esa cantidad de semanas. El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdePlantilla(alumnoId, plantillaId, semanas) {
  return fromApi(await request(`${BASE_PATH}/desde-plantilla`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), plantillaId: Number(plantillaId), cantidadSemanas: Number(semanas) },
  }));
}

/** Plan armado con el mismo formulario que una plantilla. El backend responde 409 si ya tiene un plan activo. */
export async function asignarDesdeCero(alumnoId, form, semanas) {
  return fromApi(await request(`${BASE_PATH}/desde-cero`, {
    method: 'POST',
    body: { alumnoId: Number(alumnoId), ...formularioAApi(form), cantidadSemanas: Number(semanas) },
  }));
}

/**
 * Reemplaza los datos, sesiones y semanas (y con ellas la fecha fin) del plan; la plantilla de la que salió no cambia.
 * El backend responde 409 si está finalizado o vencido.
 */
export async function actualizarPlan(id, form, semanas) {
  return fromApi(await request(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: { ...formularioAApi(form), cantidadSemanas: Number(semanas) },
  }));
}

// --- HU-266: vigencia en semanas (la fecha fin se calcula) ---

/** Mismo tope que el backend: un año. */
export const MAXIMO_SEMANAS = 52;

/** Hoy en Argentina, "2026-11-03" (la fecha de inicio de un plan que se asigna ahora). */
export const hoyIso = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Argentina/Buenos_Aires' });

/** "2026-11-03" + n días → "2026-11-04". */
export function sumarDias(fechaIso, dias) {
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

/** Días de una fecha a otra: "2026-11-03" → "2026-11-10" = 7. */
function diasEntre(desdeIso, hastaIso) {
  const aUtc = (iso) => { const [a, m, d] = iso.split('-').map(Number); return Date.UTC(a, m - 1, d); };
  return Math.round((aUtc(hastaIso) - aUtc(desdeIso)) / 86_400_000);
}

/** Último día de la última semana de un plan de n semanas que empieza ese día. */
export const fechaFinPorSemanas = (inicioIso, semanas) => sumarDias(inicioIso, semanas * 7 - 1);

/** En qué semana del plan cae ese día: la 1 va del inicio a seis días después. */
export const semanaEn = (inicioIso, diaIso) => Math.floor(diasEntre(inicioIso, diaIso) / 7) + 1;

/**
 * Semanas de un plan ya cargado. Los que se asignaron eligiendo una fecha fin suelta no las tienen: se redondea para
 * arriba, así la propuesta nunca lo acorta.
 */
export function semanasDelPlan(plan) {
  if (plan.cantidadSemanas) return plan.cantidadSemanas;
  if (plan.fechaFin) return Math.max(1, Math.ceil((diasEntre(plan.fechaAsignacion, plan.fechaFin) + 1) / 7));
  return null;
}

/**
 * Mismas reglas que el backend, para avisar antes de enviar. minimo: 1 al asignar; al modificar, la semana en curso
 * (no se puede recortar un plan a semanas que ya pasaron).
 */
export function validarSemanas(semanas, { minimo = 1 } = {}) {
  if (semanas === '' || semanas == null) return 'La cantidad de semanas es obligatoria.';
  const n = Number(semanas);
  if (!Number.isInteger(n)) return 'La cantidad de semanas tiene que ser un número entero.';
  if (n < 1) return 'El plan tiene que durar al menos una semana.';
  if (n < minimo) return `El plan ya va por la semana ${minimo}: no puede durar menos de ${minimo} semanas.`;
  if (n > MAXIMO_SEMANAS) return `El plan no puede durar más de ${MAXIMO_SEMANAS} semanas.`;
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
