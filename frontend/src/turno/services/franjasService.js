import { request } from '../../shared/services/httpClient';

// HU-252: franjas horarias semanales y cupo máximo por turno (solo propietario)

/** Días como los manda el backend (java.time.DayOfWeek), de lunes a domingo, con sus textos. */
export const DIAS_SEMANA = [
  { valor: 'MONDAY', corto: 'Lun', nombre: 'Lunes', singular: 'lunes', plural: 'lunes' },
  { valor: 'TUESDAY', corto: 'Mar', nombre: 'Martes', singular: 'martes', plural: 'martes' },
  { valor: 'WEDNESDAY', corto: 'Mié', nombre: 'Miércoles', singular: 'miércoles', plural: 'miércoles' },
  { valor: 'THURSDAY', corto: 'Jue', nombre: 'Jueves', singular: 'jueves', plural: 'jueves' },
  { valor: 'FRIDAY', corto: 'Vie', nombre: 'Viernes', singular: 'viernes', plural: 'viernes' },
  { valor: 'SATURDAY', corto: 'Sáb', nombre: 'Sábado', singular: 'sábado', plural: 'sábados' },
  { valor: 'SUNDAY', corto: 'Dom', nombre: 'Domingo', singular: 'domingo', plural: 'domingos' },
];

export const diaSemana = (valor) => DIAS_SEMANA.find(d => d.valor === valor);

/** "16:30" -> 990 */
export const aMinutos = (hora) => {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
};

const aHora = (minutos) => `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`;

/** Turnos de 1 hora en los que se divide una franja (mismo cálculo que el backend). */
export function calcularTurnos(horaInicio, horaFin) {
  const inicio = aMinutos(horaInicio);
  const cantidad = Math.floor((aMinutos(horaFin) - inicio) / 60);
  return Array.from({ length: Math.max(cantidad, 0) }, (_, i) => ({
    horaInicio: aHora(inicio + i * 60),
    horaFin: aHora(inicio + (i + 1) * 60),
  }));
}

/** Texto corto de un turno: "16–17" si es en punto, "16:30–17:30" si no. */
export function etiquetaTurno({ horaInicio, horaFin }) {
  const corta = (hora) => (hora.endsWith(':00') ? String(Number(hora.slice(0, 2))) : hora);
  return `${corta(horaInicio)}–${corta(horaFin)}`;
}

/**
 * Mismas validaciones que el backend, para avisar mientras se completa el formulario.
 * Devuelve el mensaje de error o null. idAExcluir: la franja que se está modificando.
 */
export function validarFranja({ diaSemana: dia, horaInicio, horaFin }, franjas, idAExcluir) {
  const inicio = aMinutos(horaInicio);
  const fin = aMinutos(horaFin);
  if (fin <= inicio) return 'La hora de fin debe ser posterior a la de inicio.';
  if ((fin - inicio) % 60 !== 0) return 'La franja tiene que durar horas completas (los turnos son de 1 hora).';
  const superpuesta = franjas.find(f => f.diaSemana === dia && f.id !== idAExcluir
    && inicio < aMinutos(f.horaFin) && aMinutos(f.horaInicio) < fin);
  if (superpuesta) {
    return `Esta franja se superpone con ${superpuesta.horaInicio} – ${superpuesta.horaFin} del ${diaSemana(dia).singular}.`;
  }
  return null;
}

const toApi = (form) => ({ diaSemana: form.diaSemana, horaInicio: form.horaInicio, horaFin: form.horaFin });

export async function listarFranjas() {
  return request('/franjas');
}

export async function crearFranja(form) {
  return request('/franjas', { method: 'POST', body: toApi(form) });
}

export async function actualizarFranja(id, form) {
  return request(`/franjas/${id}`, { method: 'PUT', body: toApi(form) });
}

/** Siempre se permite: los turnos ya generados se mantienen. */
export async function eliminarFranja(id) {
  return request(`/franjas/${id}`, { method: 'DELETE' });
}

export async function obtenerConfiguracionGimnasio() {
  return request('/configuracion-gimnasio');
}

export async function cambiarCupoMaximo(cupoMaximo) {
  return request('/configuracion-gimnasio/cupo-maximo', { method: 'PUT', body: { cupoMaximo } });
}
