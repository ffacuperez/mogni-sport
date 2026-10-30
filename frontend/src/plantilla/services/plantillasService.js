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
export const LIMITES = { seriesMax: 20, repeticionesMax: 100, pesoMax: 999.99, nombreMax: 100, nombreSesionMax: 100, rirMax: 10 };

const fromApi = (p) => ({
  id: p.id,
  nombre: p.nombre,
  nivel: p.nivel,
  objetivo: p.objetivo,
  asignacionesActivas: p.asignacionesActivas ?? 0,
  fechaActualizacion: p.fechaActualizacion,
  sesiones: (p.sesiones ?? []).map(s => ({
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
      pesoSugerido: e.pesoSugerido,
      rirObjetivo: e.rirObjetivo,
    })),
  })),
});

// Formulario -> body que espera el backend (PlantillaRequest). El peso acepta coma decimal.
const toApi = (form) => ({
  nombre: form.nombre.trim(),
  nivel: form.nivel,
  objetivo: form.objetivo,
  sesiones: form.sesiones.map(s => ({
    nombre: s.nombre.trim() || null,
    ejercicios: s.ejercicios.map(e => ({
      ejercicioId: Number(e.ejercicioId),
      series: Number(e.series),
      repeticiones: Number(e.repeticiones),
      pesoSugerido: String(e.peso ?? '').trim() ? Number(String(e.peso).replace(',', '.')) : null,
      rirObjetivo: String(e.rir ?? '').trim() ? Number(e.rir) : null,
    })),
  })),
});

export async function listarPlantillas() {
  const data = await request(BASE_PATH);
  return data.map(fromApi);
}

export async function obtenerPlantilla(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearPlantilla(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: toApi(form) }));
}

export async function actualizarPlantilla(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: toApi(form) }));
}

/** Baja lógica; el backend responde 409 si la plantilla está asignada a algún alumno. */
export async function eliminarPlantilla(id) {
  await request(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}

export async function duplicarPlantilla(id) {
  return fromApi(await request(`${BASE_PATH}/${id}/duplicar`, { method: 'POST' }));
}

export const cantidadEjercicios = (plantilla) => plantilla.sesiones.reduce((total, s) => total + s.ejercicios.length, 0);
