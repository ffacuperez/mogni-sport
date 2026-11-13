import { LIMITES, parsearRepeticiones, textoRepeticiones } from '../services/plantillasService';

// Estado y validación del formulario de armado de un plan: { nombre, nivel, objetivo, semanas }. Cada semana tiene su
// nota (opcional) y sus propias sesiones. Lo usan el alta/modificación de plantillas (HU-265), la asignación de un plan
// desde cero (HU-266) y la modificación de un plan asignado (HU-267).

export const mayuscula = (texto) => texto.charAt(0).toUpperCase() + texto.slice(1);

// Claves locales para las listas del formulario (las semanas, sesiones y ejercicios nuevos todavía no tienen id)
let ultimaClave = 0;
const nuevaClave = () => `n${++ultimaClave}`;

export const ejercicioVacio = () => ({
  clave: nuevaClave(), ejercicioId: '', series: '', repeticiones: '', descanso: '', peso: '', rir: '', indicaciones: '',
});
export const sesionVacia = () => ({ clave: nuevaClave(), nombre: '', ejercicios: [ejercicioVacio()] });
export const semanaVacia = () => ({ clave: nuevaClave(), nota: '', sesiones: [sesionVacia()] });

/** Copia de sesiones con claves nuevas: la copia se edita sin tocar la original. */
export const copiarSesiones = (sesiones) => sesiones.map(s => ({
  ...s,
  clave: nuevaClave(),
  ejercicios: s.ejercicios.map(e => ({ ...e, clave: nuevaClave() })),
}));

/** Copia de una semana (sesiones, ejercicios y nota) con claves nuevas. */
export const copiarSemana = (semana, { conNota = true } = {}) => ({
  clave: nuevaClave(),
  nota: conNota ? semana.nota : '',
  sesiones: copiarSesiones(semana.sesiones),
});

/** Formulario vacío para un alta: una semana con una sesión. */
export const formularioVacio = () => ({ nombre: '', nivel: '', objetivo: '', semanas: [semanaVacia()] });

const aTexto = (numero) => (numero == null ? '' : String(numero).replace('.', ','));

/** Plantilla (o plan) del backend -> estado del formulario. */
export const aFormulario = (plantilla) => ({
  nombre: plantilla.nombre,
  nivel: plantilla.nivel,
  objetivo: plantilla.objetivo,
  semanas: plantilla.semanas.map(semana => ({
    clave: nuevaClave(),
    nota: semana.nota ?? '',
    sesiones: semana.sesiones.map(s => ({
      clave: nuevaClave(),
      nombre: s.nombre ?? '',
      ejercicios: s.ejercicios.map(e => ({
        clave: nuevaClave(),
        ejercicioId: String(e.ejercicioId),
        series: String(e.series),
        repeticiones: textoRepeticiones(e.repeticiones, e.repeticionesMax),
        descanso: aTexto(e.descansoMinutos),
        peso: aTexto(e.pesoSugerido),
        rir: e.rirObjetivo == null ? '' : String(e.rirObjetivo),
        indicaciones: e.indicaciones ?? '',
      })),
    })),
  })),
});

// Los campos enteros no filtran lo que se escribe: si no, "8.5" quedaría como 85 sin ningún aviso. Se valida al confirmar.
const esEntero = (texto) => /^\d+$/.test(texto.trim());
export const PESO = /^\d{0,3}([.,]\d{0,2})?$/;
export const DESCANSO = /^\d{0,2}([.,]\d{0,2})?$/;

/**
 * Primer error del formulario, o null. Mismos límites que el backend.
 * sujeto: 'la plantilla' o 'el plan', para los mensajes.
 */
export function validarFormulario(form, sujeto = 'la plantilla') {
  return primerError(form, sujeto)?.mensaje ?? null;
}

/**
 * Dónde está el primer error del formulario ({ semana, sesion }: sus claves), para mostrar esa semana, abrir la sesión
 * y llevar la pantalla hasta ella. Null si el error está en el nombre, el nivel o el objetivo, o si no hay errores.
 */
export function ubicacionDelError(form, sujeto = 'la plantilla') {
  const error = primerError(form, sujeto);
  return error?.semana ? { semana: error.semana, sesion: error.sesion ?? null } : null;
}

function primerError(form, sujeto) {
  if (!form.nombre.trim()) return { mensaje: `El nombre de ${sujeto} es obligatorio.` };
  if (!form.nivel) return { mensaje: `Elegí el nivel de ${sujeto}.` };
  if (!form.objetivo) return { mensaje: `Elegí el objetivo de ${sujeto}.` };
  if (form.semanas.length === 0) return { mensaje: `${mayuscula(sujeto)} tiene que tener al menos una semana.` };
  if (form.semanas.length > LIMITES.semanasMax) {
    return { mensaje: `${mayuscula(sujeto)} puede tener como máximo ${LIMITES.semanasMax} semanas.` };
  }
  for (const [i, semana] of form.semanas.entries()) {
    const nSemana = `Semana ${i + 1}`;
    if (semana.nota.trim().length > LIMITES.notaMax) {
      return { mensaje: `${nSemana}: la nota puede tener hasta ${LIMITES.notaMax} caracteres.`, semana: semana.clave };
    }
    if (semana.sesiones.length === 0) return { mensaje: `${nSemana}: agregá al menos una sesión.`, semana: semana.clave };
    if (semana.sesiones.length > LIMITES.sesionesPorSemanaMax) {
      return { mensaje: `${nSemana}: puede tener como máximo ${LIMITES.sesionesPorSemanaMax} sesiones.`, semana: semana.clave };
    }
    for (const [k, sesion] of semana.sesiones.entries()) {
      const mensaje = errorDeSesion(sesion, `${nSemana}, sesión ${k + 1}`);
      if (mensaje) return { mensaje, semana: semana.clave, sesion: sesion.clave };
    }
  }
  return null;
}

function errorDeSesion(sesion, nSesion) {
  if (sesion.ejercicios.length === 0) return `${nSesion}: agregá al menos un ejercicio.`;
  for (const [j, e] of sesion.ejercicios.entries()) {
    const donde = `${nSesion}, ejercicio ${j + 1}`;
    if (!e.ejercicioId) return `${donde}: elegí un ejercicio del catálogo.`;
    const series = Number(e.series);
    if (!esEntero(e.series) || series < 1 || series > LIMITES.seriesMax) return `${donde}: las series tienen que ser un número entero entre 1 y ${LIMITES.seriesMax}.`;
    const reps = parsearRepeticiones(e.repeticiones);
    if (!reps) return `${donde}: las repeticiones objetivo tienen que ser un número entero (ej. 8) o un rango (ej. 8-10).`;
    if (reps.min < 1 || reps.min > LIMITES.repeticionesMax || reps.max > LIMITES.repeticionesMax) return `${donde}: las repeticiones objetivo tienen que estar entre 1 y ${LIMITES.repeticionesMax}.`;
    if (reps.max != null && reps.min > reps.max) return `${donde}: en el rango de repeticiones, el mínimo no puede ser mayor al máximo.`;
    if (e.descanso.trim()) {
      const descanso = Number(e.descanso.replace(',', '.'));
      if (!(descanso > 0) || descanso > LIMITES.descansoMax) return `${donde}: el descanso sugerido tiene que ser mayor a 0 y hasta ${LIMITES.descansoMax} minutos.`;
    }
    if (e.peso.trim()) {
      const peso = Number(e.peso.replace(',', '.'));
      if (!(peso > 0) || peso > LIMITES.pesoMax) return `${donde}: el peso sugerido tiene que ser mayor a 0 y hasta ${LIMITES.pesoMax} kg.`;
    }
    if (e.rir.trim() && (!esEntero(e.rir) || Number(e.rir) > LIMITES.rirMax)) return `${donde}: el RIR objetivo tiene que ser un número entero entre 0 y ${LIMITES.rirMax}.`;
    if (e.indicaciones.trim().length > LIMITES.indicacionesMax) return `${donde}: las indicaciones pueden tener hasta ${LIMITES.indicacionesMax} caracteres.`;
  }
  return null;
}

/** Total de sesiones de todas las semanas. */
export const totalSesiones = (form) => form.semanas.reduce((total, semana) => total + semana.sesiones.length, 0);
