import { LIMITES, parsearRepeticiones, textoRepeticiones } from '../services/plantillasService';

// Estado y validación del formulario de armado de un plan: { nombre, nivel, objetivo, sesiones }.
// Lo usan el alta/modificación de plantillas (HU-265) y la asignación de un plan desde cero (HU-266).

export const mayuscula = (texto) => texto.charAt(0).toUpperCase() + texto.slice(1);

// Claves locales para las listas del formulario (las sesiones y ejercicios nuevos todavía no tienen id)
let ultimaClave = 0;
const nuevaClave = () => `n${++ultimaClave}`;

export const ejercicioVacio = () => ({ clave: nuevaClave(), ejercicioId: '', series: '', repeticiones: '', descanso: '', peso: '', rir: '' });
export const sesionVacia = () => ({ clave: nuevaClave(), nombre: '', ejercicios: [ejercicioVacio()] });

/** Formulario vacío para un alta. */
export const formularioVacio = () => ({ nombre: '', nivel: '', objetivo: '', sesiones: [sesionVacia()] });

/** Plantilla (o plan) del backend -> estado del formulario. */
export const aFormulario = (plantilla) => ({
  nombre: plantilla.nombre,
  nivel: plantilla.nivel,
  objetivo: plantilla.objetivo,
  sesiones: plantilla.sesiones.map(s => ({
    clave: nuevaClave(),
    nombre: s.nombre,
    ejercicios: s.ejercicios.map(e => ({
      clave: nuevaClave(),
      ejercicioId: String(e.ejercicioId),
      series: String(e.series),
      repeticiones: textoRepeticiones(e.repeticiones, e.repeticionesMax),
      descanso: e.descansoMinutos == null ? '' : String(e.descansoMinutos).replace('.', ','),
      peso: e.pesoSugerido == null ? '' : String(e.pesoSugerido).replace('.', ','),
      rir: e.rirObjetivo == null ? '' : String(e.rirObjetivo),
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
  if (!form.nombre.trim()) return `El nombre de ${sujeto} es obligatorio.`;
  if (!form.nivel) return `Elegí el nivel de ${sujeto}.`;
  if (!form.objetivo) return `Elegí el objetivo de ${sujeto}.`;
  if (form.sesiones.length === 0) return `${mayuscula(sujeto)} tiene que tener al menos una sesión.`;
  for (const [i, sesion] of form.sesiones.entries()) {
    const nSesion = `Sesión ${i + 1}`;
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
    }
  }
  return null;
}
