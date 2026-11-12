import { request } from '../../shared/services/httpClient';

// HU-259, HU-254, HU-256 y HU-255: turnos disponibles del alumno logueado (hoy y mañana). El backend decide fechas, estados y
// si cada turno se puede elegir; acá solo se piden y se muestran.

/** fecha: "2026-10-13" (hoy o mañana). Sin fecha, el backend devuelve hoy. */
export async function consultarTurnosDisponibles(fecha) {
  return request(fecha ? `/turnos/disponibles?fecha=${fecha}` : '/turnos/disponibles');
}

/** HU-256: turnos reservados del alumno logueado que todavía no terminaron, del más cercano al más lejano. */
export async function consultarMisTurnos() {
  return request('/turnos/mis-turnos');
}

/** HU-255: cancela una reserva del alumno (hasta 2 horas antes). Si el backend la rechaza, el error trae el motivo. */
export async function cancelarReserva(reservaId) {
  return request(`/turnos/reservas/${reservaId}/cancelar`, { method: 'POST' });
}

/** HU-254: reserva el turno para el alumno logueado. Si el backend la rechaza, el error trae el motivo en español. */
export async function reservarTurno(turnoId) {
  return request(`/turnos/${turnoId}/reservar`, { method: 'POST' });
}

const DIAS_CORTOS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

/** "2026-10-13" -> { dia: 'Mar', numero: 13 }. Sale de la fecha que manda el backend, no del reloj del navegador. */
export function partesFecha(fechaISO) {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return { dia: DIAS_CORTOS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()], numero: d };
}

const DIAS_LARGOS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

/** "2026-10-14" -> "Miércoles 14/10" */
export function textoDiaCompleto(fechaISO) {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return `${DIAS_LARGOS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]} ${d}/${m}`;
}

/** total: turnos por semana de la membresía; con 0 restantes se muestra "Usaste tus N turnos". */
export function textoTurnosRestantes(cantidad, total = 0) {
  if (cantidad === 0 && total > 0) return total === 1 ? 'Usaste tu turno de esta semana' : `Usaste tus ${total} turnos de esta semana`;
  if (cantidad === 0) return 'No te quedan turnos esta semana';
  if (cantidad === 1) return 'Te queda 1 turno esta semana';
  return `Te quedan ${cantidad} turnos esta semana`;
}

/** Clases de turnos que el gimnasio le canceló en semanas anteriores: van aparte del contador. "" si no tiene. */
export const textoParaRecuperar = (cantidad) => (cantidad > 0 ? ` · +${cantidad} para recuperar` : '');

export const textoLugaresLibres = (lugares) => (lugares === 1 ? '1 lugar libre' : `${lugares} lugares libres`);

const sumarDias = (fechaISO, dias) => {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + dias)).toISOString().slice(0, 10);
};

/** "Hoy · Mar 13/10", "Mañana · Mié 14/10" o, para otro día, "Miércoles 14/10". hoy sale del backend. */
export function etiquetaFechaTurno(fechaISO, hoy) {
  if (fechaISO !== hoy && fechaISO !== sumarDias(hoy, 1)) return textoDiaCompleto(fechaISO);
  const [, m, d] = fechaISO.split('-').map(Number);
  return `${fechaISO === hoy ? 'Hoy' : 'Mañana'} · ${partesFecha(fechaISO).dia} ${d}/${m}`;
}
