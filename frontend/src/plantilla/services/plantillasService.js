import { request } from '../../shared/services/httpClient';

// HU-265: plantillas de plan de entrenamiento (propietario y entrenador)

const BASE_PATH = '/plantillas';

/** Valores como los manda el backend, con su texto. */
export const NIVELES = [
  { valor: 'PRINCIPIANTE', texto: 'Principiante' },
  { valor: 'INTERMEDIO', texto: 'Intermedio' },
  { valor: 'AVANZADO', texto: 'Avanzado' },
];

export const OBJETIVOS = [
  { valor: 'HIPERTROFIA', texto: 'Hipertrofia' },
  { valor: 'FUERZA', texto: 'Fuerza' },
  { valor: 'RESISTENCIA', texto: 'Resistencia' },
  { valor: 'PERDIDA_DE_GRASA', texto: 'Pérdida de grasa' },
];

export const textoNivel = (valor) => NIVELES.find(n => n.valor === valor)?.texto ?? valor;

/** Tono del Badge según la dificultad: verde, amarillo, rojo. */
const TONO_NIVEL = { PRINCIPIANTE: 'success', INTERMEDIO: 'warning', AVANZADO: 'error' };
export const tonoNivel = (valor) => TONO_NIVEL[valor] ?? 'neutral';
export const textoObjetivo = (valor) => OBJETIVOS.find(o => o.valor === valor)?.texto ?? valor;

/** Límites del formulario (los mismos que valida el backend). */
export const LIMITES = {
  seriesMax: 20, repeticionesMax: 100, pesoMax: 999.99, nombreMax: 100, nombreSesionMax: 100, rirMax: 10, descansoMax: 10,
  semanasMax: 24, sesionesPorSemanaMax: 7, notaMax: 60, indicacionesMax: 200,
};

/** "8" → { min: 8, max: null }; "8-10" → { min: 8, max: 10 }; otro formato → null. No valida límites. */
export function parsearRepeticiones(texto) {
  const partes = /^\s*(\d+)\s*(?:-\s*(\d+)\s*)?$/.exec(texto ?? '');
  if (!partes) return null;
  return { min: Number(partes[1]), max: partes[2] == null ? null : Number(partes[2]) };
}

/** 8 / 8-10, como se muestra y se escribe en el formulario. */
export const textoRepeticiones = (min, max) => (max == null ? String(min) : `${min}-${max}`);

/** 1.5 → "1,5 min". */
export const textoDescanso = (minutos) => `${Number(minutos).toLocaleString('es-AR', { maximumFractionDigits: 2 })} min`;

const fromApi = (p) => ({
  id: p.id,
  nombre: p.nombre,
  nivel: p.nivel,
  objetivo: p.objetivo,
  asignacionesActivas: p.asignacionesActivas ?? 0,
  fechaActualizacion: p.fechaActualizacion,
  semanas: semanasFromApi(p.semanas),
});

/** Semanas con sus sesiones y ejercicios; mismo formato en plantillas y en planes asignados (HU-266). */
export const semanasFromApi = (semanas) => (semanas ?? []).map(w => ({
  id: w.id,
  numero: w.numero,
  nota: w.nota ?? '',
  sesiones: sesionesFromApi(w.sesiones),
}));

const sesionesFromApi = (sesiones) => (sesiones ?? []).map(s => ({
    id: s.id,
    numero: s.numero,
    nombre: s.nombre ?? '',
    ejercicios: (s.ejercicios ?? []).map(e => ({
      id: e.id,
      ejercicioId: e.ejercicioId,
      nombre: e.nombre,
      grupoMuscular: e.grupoMuscular,
      ejercicioActivo: e.ejercicioActivo,
      series: e.series,
      repeticiones: e.repeticiones,
      repeticionesMax: e.repeticionesMax ?? null,
      descansoMinutos: e.descansoMinutos ?? null,
      pesoSugerido: e.pesoSugerido,
      rirObjetivo: e.rirObjetivo,
      indicaciones: e.indicaciones ?? '',
    })),
}));

// Formulario -> body que espera el backend (PlantillaRequest). El peso y el descanso aceptan coma decimal.
export const formularioAApi = (form) => ({
  nombre: form.nombre.trim(),
  nivel: form.nivel,
  objetivo: form.objetivo,
  semanas: form.semanas.map(semana => ({
    nota: semana.nota.trim() || null,
    sesiones: semana.sesiones.map(s => ({
      nombre: s.nombre.trim() || null,
      ejercicios: s.ejercicios.map(e => ({
        ejercicioId: Number(e.ejercicioId),
        series: Number(e.series),
        repeticiones: parsearRepeticiones(e.repeticiones).min,
        repeticionesMax: parsearRepeticiones(e.repeticiones).max,
        descansoMinutos: String(e.descanso ?? '').trim() ? Number(String(e.descanso).replace(',', '.')) : null,
        pesoSugerido: String(e.peso ?? '').trim() ? Number(String(e.peso).replace(',', '.')) : null,
        rirObjetivo: String(e.rir ?? '').trim() ? Number(e.rir) : null,
        indicaciones: String(e.indicaciones ?? '').trim() || null,
      })),
    })),
  })),
});

export async function listarPlantillas() {
  const data = await request(BASE_PATH);
  return data.map(fromApi);
}

/** Plantillas inactivas, para poder reactivarlas. */
export async function listarPlantillasInactivas() {
  const data = await request(`${BASE_PATH}/inactivas`);
  return data.map(fromApi);
}

/** Vuelve a activar una plantilla; el backend responde 409 si ya hay una activa con el mismo nombre. */
export async function reactivarPlantilla(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/reactivar`, { method: 'PUT' }));
}

export async function obtenerPlantilla(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearPlantilla(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: formularioAApi(form) }));
}

export async function actualizarPlantilla(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: formularioAApi(form) }));
}

/** Baja lógica; el backend responde 409 si la plantilla está asignada a algún alumno. */
export async function eliminarPlantilla(id) {
  await request(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}

export async function duplicarPlantilla(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/duplicar`, { method: 'POST' }));
}

/** Sesiones de todas las semanas. */
export const cantidadSesiones = (plantilla) => plantilla.semanas.reduce((total, w) => total + w.sesiones.length, 0);

/** Ejercicios de todas las sesiones de todas las semanas. */
export const cantidadEjercicios = (plantilla) => plantilla.semanas
  .reduce((total, w) => total + w.sesiones.reduce((suma, s) => suma + s.ejercicios.length, 0), 0);

/** Los ids de todos los ejercicios del catálogo que usa (para conservar los que se dieron de baja). */
export const idsDeEjercicios = (plantilla) => new Set(plantilla.semanas
  .flatMap(w => w.sesiones.flatMap(s => s.ejercicios.map(e => String(e.ejercicioId)))));
