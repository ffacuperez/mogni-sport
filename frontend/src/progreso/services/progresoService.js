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

const serieFromApi = (s) => ({
  orden: s.orden,
  peso: s.peso,
  repeticiones: s.repeticiones,
  rir: s.rir,
});

// HU-269: en una sesión ya empezada, cada ejercicio trae también lo que cargó el alumno
const ejercicioSesionFromApi = (e) => ({
  ...ejercicioFromApi(e),
  orden: e.orden,
  completo: e.completo,
  rmEstimado: e.rmEstimado ?? null,
  notas: e.notas ?? null,
  seriesRealizadas: (e.seriesRealizadas ?? []).map(serieFromApi),
});

const sesionFromApi = (s) => ({
  numero: s.numero,
  estado: s.estado,
  fechaRegistro: s.fechaRegistro ?? null,
  registroId: s.registroId ?? null,
  registrable: s.registrable,
  ejercicios: s.ejercicios.map(ejercicioSesionFromApi),
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

/** HU-269: empieza una sesión de la semana actual o, si ya estaba empezada, la devuelve. */
export async function iniciarSesion(planId, semana, numeroSesion) {
  return sesionFromApi(await request(`${BASE_PATH}/planes/${planId}/sesiones`, { method: 'POST', body: { semana, numeroSesion } }));
}

/** HU-269: carga o corrige un ejercicio con todas sus series. Devuelve la sesión actualizada. */
export async function registrarEjercicio(registroId, orden, { series, notas }) {
  return sesionFromApi(await request(`${BASE_PATH}/sesiones/${registroId}/ejercicios/${orden}`, { method: 'PUT', body: { series, notas } }));
}

/** HU-269: finaliza la sesión; el backend lo rechaza si falta algún ejercicio. */
export async function completarSesion(registroId) {
  return sesionFromApi(await request(`${BASE_PATH}/sesiones/${registroId}/completar`, { method: 'POST' }));
}

/** HU-269: 1RM estimado (Epley con RIR), la misma cuenta que hace el backend. Solo para la vista previa. */
export function calcularRm(peso, repeticiones, rir) {
  const posibles = repeticiones + rir;
  return posibles <= 1 ? peso : peso * (1 + posibles / 30);
}

/**
 * MOGNI-269: la serie de la que sale el único 1RM del ejercicio: la de más peso; si empatan, la de más repeticiones; si
 * también, la primera. La misma regla que el backend. Recibe series con peso, repeticiones y rir numéricos.
 */
export function serieDeReferencia(series) {
  return series.reduce((mejor, serie) => (
    serie.peso > mejor.peso || (serie.peso === mejor.peso && serie.repeticiones > mejor.repeticiones) ? serie : mejor
  ));
}

/** Las sesiones registradas del plan, ya ordenadas por el backend. */
export async function consultarSesiones(planId) {
  return request(`${BASE_PATH}/planes/${planId}/sesiones`);
}

/** Una sesión registrada, con lo indicado y lo realizado en cada ejercicio. */
export async function consultarSesion(id) {
  const s = await request(`${BASE_PATH}/sesiones/${id}`);
  return {
    semana: s.semana,
    numeroSesion: s.numeroSesion,
    fecha: s.fecha,
    plan: { nombre: s.nombrePlan, metodo: s.metodo ?? null, mesociclo: s.mesociclo ?? null },
    ejercicios: s.ejercicios.map((e) => ({
      ...ejercicioFromApi(e),
      kgUsados: e.kgUsados ?? null,
      rirReal: e.rirReal ?? null,
      notas: e.notas ?? null,
      rmEstimado: e.rmEstimado ?? null,
      seriesRealizadas: (e.seriesRealizadas ?? []).map(serieFromApi),
      diferenciaRir: e.diferenciaRir ?? null,
    })),
  };
}

/** Agrupa las sesiones por semana, respetando el orden en que vienen. */
export function agruparPorSemana(sesiones) {
  const grupos = [];
  sesiones.forEach((sesion) => {
    const ultimo = grupos[grupos.length - 1];
    if (ultimo?.semana === sesion.semana) ultimo.sesiones.push(sesion);
    else grupos.push({ semana: sesion.semana, sesiones: [sesion] });
  });
  return grupos;
}

/** "Hipertrofia": el mesociclo o, en los planes que no lo tienen cargado, el título. */
export const nombreCortoPlan = (plan) => plan.mesociclo ?? tituloPlan(plan);

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

/** "Mié 28/10/2026" */
export const textoFechaCompleta = (fechaISO) => `${partesFecha(fechaISO).dia} ${formatearFecha(fechaISO)}`;

/** 12.5 -> "12,5 kg" */
export const textoKg = (kg) => `${Number(kg).toLocaleString('es-AR', { maximumFractionDigits: 2 })} kg`;

/** -1 -> "RIR −1 vs. indicado" */
export const textoDiferenciaRir = (diferencia) => `RIR ${diferencia > 0 ? '+' : '−'}${Math.abs(diferencia)} vs. indicado`;
