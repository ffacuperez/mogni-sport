import { request } from '../../shared/services/httpClient';
import { formatearFecha } from '../../shared/services/texto';
import { textoRepeticiones } from '../../plantilla/services/plantillasService';
import { partesFecha } from '../../turno/services/turnosService';

// HU-270: progreso del alumno logueado. El backend decide estados, totales y la semana en curso; acá solo se piden y se muestran.

const BASE_PATH = '/mi-progreso';

const METODOS = { DEFICIT: 'Déficit', ULTRA_MASS: 'Ultra Mass', VITALITY: 'Vitality' };

const ejercicioFromApi = (e) => ({
  nombre: e.nombre,
  series: e.series,
  repeticiones: e.repeticiones,
  repeticionesMax: e.repeticionesMax ?? null,
  rirObjetivo: e.rirObjetivo ?? null,
  descansoMinutos: e.descansoMinutos ?? null,
  indicaciones: e.indicaciones ?? null,
});

const sesionFromApi = (s) => ({
  numero: s.numero,
  estado: s.estado,
  fechaRegistro: s.fechaRegistro ?? null,
  ejercicios: s.ejercicios.map(ejercicioFromApi),
});

const semanaFromApi = (s) => ({
  numero: s.numero,
  desde: s.desde,
  hasta: s.hasta,
  sesiones: s.sesiones.map(sesionFromApi),
});

const planFromApi = (p) => ({
  id: p.id,
  nombre: p.nombre,
  metodo: p.metodo ?? null,
  mesociclo: p.mesociclo ?? null,
  objetivoMesociclo: p.objetivoMesociclo ?? null,
  estado: p.estado,
  fechaInicio: p.fechaInicio,
  fechaFin: p.fechaFin,
  cantidadSemanas: p.cantidadSemanas,
  semanaActual: p.semanaActual,
  sesionesRegistradas: p.sesionesRegistradas,
  sesionesTotales: p.sesionesTotales,
  semanas: p.semanas.map(semanaFromApi),
});

const resumenFromApi = (p) => ({
  id: p.id,
  nombre: p.nombre,
  metodo: p.metodo ?? null,
  mesociclo: p.mesociclo ?? null,
  estado: p.estado,
  fechaInicio: p.fechaInicio,
  fechaFin: p.fechaFin,
  cantidadSemanas: p.cantidadSemanas,
  sesionesRegistradas: p.sesionesRegistradas,
  sesionesTotales: p.sesionesTotales,
});

/** El plan activo (o null) y los finalizados, del más reciente al más antiguo. */
export async function consultarPlanes() {
  const data = await request(`${BASE_PATH}/planes`);
  return { activo: data.activo ? resumenFromApi(data.activo) : null, anteriores: data.anteriores.map(resumenFromApi) };
}

/** El plan activo del alumno, o null si no tiene (el backend responde 204). */
export async function consultarPlanActivo() {
  const data = await request(`${BASE_PATH}/plan-activo`);
  return data ? planFromApi(data) : null;
}

/** Un plan del alumno, con sus semanas y sesiones. */
export async function consultarPlan(id) {
  return planFromApi(await request(`${BASE_PATH}/planes/${id}`));
}

/** "Ultra Mass · Hipertrofia". Los planes sin método ni mesociclo cargados muestran su nombre. */
export function tituloPlan(plan) {
  return [METODOS[plan.metodo], plan.mesociclo].filter(Boolean).join(' · ') || plan.nombre;
}

/** "2026-10-26" -> "26/10" */
export function textoDiaMes(fechaISO) {
  const [, m, d] = fechaISO.split('-');
  return `${d}/${m}`;
}

/** "14/09 – 18/10/2026" */
export const textoRangoPlan = (plan) => `${textoDiaMes(plan.fechaInicio)} – ${formatearFecha(plan.fechaFin)}`;

export const textoSemanas = (cantidad) => (cantidad === 1 ? '1 semana' : `${cantidad} semanas`);

/** "Lun 26/10 · 5 ejercicios" si está registrada; si no, "5 ejercicios". */
export function textoDetalleSesion(sesion) {
  const cantidad = sesion.ejercicios.length === 1 ? '1 ejercicio' : `${sesion.ejercicios.length} ejercicios`;
  if (!sesion.fechaRegistro) return cantidad;
  return `${partesFecha(sesion.fechaRegistro).dia} ${textoDiaMes(sesion.fechaRegistro)} · ${cantidad}`;
}

/** "4x15-18" */
export const textoSeriesRepeticiones = (e) => `${e.series}x${textoRepeticiones(e.repeticiones, e.repeticionesMax)}`;

/** 1.25 -> "75 seg" */
export const textoDescansoSegundos = (minutos) => `${Math.round(Number(minutos) * 60)} seg`;
