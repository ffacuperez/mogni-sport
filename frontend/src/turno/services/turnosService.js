import { request } from '../../shared/services/httpClient';

// HU-259: turnos disponibles del alumno logueado (hoy y mañana). El backend decide fechas, estados y
// si cada turno se puede elegir; acá solo se piden y se muestran.

/** fecha: "2026-10-13" (hoy o mañana). Sin fecha, el backend devuelve hoy. */
export async function consultarTurnosDisponibles(fecha) {
  return request(fecha ? `/turnos/disponibles?fecha=${fecha}` : '/turnos/disponibles');
}

const DIAS_CORTOS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

/** "2026-10-13" -> { dia: 'Mar', numero: 13 }. Sale de la fecha que manda el backend, no del reloj del navegador. */
export function partesFecha(fechaISO) {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return { dia: DIAS_CORTOS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()], numero: d };
}

export function textoTurnosRestantes(cantidad) {
  if (cantidad === 0) return 'No te quedan turnos esta semana';
  if (cantidad === 1) return 'Te queda 1 turno esta semana';
  return `Te quedan ${cantidad} turnos esta semana`;
}

export const textoLugaresLibres = (lugares) => (lugares === 1 ? '1 lugar libre' : `${lugares} lugares libres`);
