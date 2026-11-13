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
  // "Tren inferior A"; null si la sesión no tiene nombre
  nombre: s.nombre ?? null,
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
  nivel: p.nivel ?? null,
  objetivo: p.objetivo ?? null,
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
  nivel: p.nivel ?? null,
  objetivo: p.objetivo ?? null,
  metodo: p.metodo ?? null,
  mesociclo: p.mesociclo ?? null,
  estado: p.estado,
  fechaInicio: p.fechaInicio,
  fechaFin: p.fechaFin,
  cantidadSemanas: p.cantidadSemanas,
  sesionesRegistradas: p.sesionesRegistradas,
  sesionesTotales: p.sesionesTotales,
});

const sesionDetalleFromApi = (s) => ({
  semana: s.semana,
  numeroSesion: s.numeroSesion,
  nombreSesion: s.nombreSesion ?? null,
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
    completo: e.completo ?? true,
  })),
});

/**
 * Las consultas del progreso contra una base: /mi-progreso para el alumno logueado (HU-270) o
 * /alumnos/{id}/progreso para el entrenador (MOGNI-268). Devuelven lo mismo en los dos casos.
 */
export function crearConsultasProgreso(base) {
  return {
    /** El plan activo (o null) y los finalizados, del más reciente al más antiguo. */
    consultarPlanes: async () => {
      const data = await request(`${base}/planes`);
      return { activo: data.activo ? resumenFromApi(data.activo) : null, anteriores: data.anteriores.map(resumenFromApi) };
    },
    /** El plan activo, o null si no tiene (el backend responde 204). */
    consultarPlanActivo: async () => {
      const data = await request(`${base}/plan-activo`);
      return data ? planFromApi(data) : null;
    },
    /** Un plan, con sus semanas y sesiones. */
    consultarPlan: async (id) => planFromApi(await request(`${base}/planes/${id}`)),
    /** Las sesiones registradas del plan, ya ordenadas por el backend (de la más reciente a la más antigua). */
    consultarSesiones: async (planId) => (await request(`${base}/planes/${planId}/sesiones`)).map((s) => ({
      id: s.id,
      semana: s.semana,
      numeroSesion: s.numeroSesion,
      nombreSesion: s.nombreSesion ?? null,
      fecha: s.fecha,
      ejerciciosRegistrados: s.ejerciciosRegistrados,
      // Los que tenía indicados al empezarla; si el backend no lo manda, se asume completa
      ejerciciosTotales: s.ejerciciosTotales ?? s.ejerciciosRegistrados,
    })),
    /** Una sesión registrada, con lo indicado y lo realizado en cada ejercicio. */
    consultarSesion: async (id) => sesionDetalleFromApi(await request(`${base}/sesiones/${id}`)),
  };
}

/** MOGNI-268: las mismas consultas, del alumno que elige el entrenador. */
export const consultasProgresoDeAlumno = (alumnoId) => crearConsultasProgreso(`/alumnos/${alumnoId}/progreso`);

export const { consultarPlanes, consultarPlanActivo, consultarPlan, consultarSesiones, consultarSesion } = crearConsultasProgreso(BASE_PATH);

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

/** Las sesiones de cada semana del plan (todas las semanas repiten las mismas). */
export const sesionesPorSemana = (plan) => plan.semanas[0]?.sesiones.length ?? 0;

/**
 * Las sesiones que el alumno ya podría haber hecho: todas las de las semanas hasta la actual, incluida. Un plan
 * finalizado ya las tuvo todas.
 */
export function sesionesEsperadasHastaEstaSemana(plan) {
  if (plan.estado !== 'ACTIVO') return plan.sesionesTotales;
  return plan.semanas.slice(0, plan.semanaActual).reduce((total, semana) => total + semana.sesiones.length, 0);
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
/** El nombre que el profe le puso a la sesión ("Pecho + tríceps"); si no tiene, "Sesión 2". */
export const tituloSesion = (nombre, numero) => nombre?.trim() || `Sesión ${numero}`;

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
