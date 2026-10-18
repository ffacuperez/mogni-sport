import { request } from '../../shared/services/httpClient';
import { estadoVisible, tonoEstadoCuota } from '../../cuota/services/cuotasService';

// MOGNI-257: agenda de turnos del personal (propietario y entrenador). El backend decide estados, ocupación y
// qué días están bloqueados; acá solo se piden y se muestran.

/** fecha: "2026-10-15" (cualquier fecha). Sin fecha, el backend devuelve hoy. */
export async function obtenerAgendaDia(fecha) {
  return request(fecha ? `/agenda/dia?fecha=${fecha}` : '/agenda/dia');
}

/** Semana de lunes a domingo que contiene la fecha. Sin fecha, la semana actual. */
export async function obtenerAgendaSemana(fecha) {
  return request(fecha ? `/agenda/semana?fecha=${fecha}` : '/agenda/semana');
}

export async function resumenCancelarTurno(fecha, horaInicio) {
  return request(`/agenda/cancelacion/resumen?fecha=${fecha}&horaInicio=${encodeURIComponent(horaInicio)}`);
}

export async function resumenCancelarDia(fecha) {
  return request(`/agenda/cancelacion/dia/resumen?fecha=${fecha}`);
}

export async function cancelarTurnoAgenda(fecha, horaInicio) {
  return request('/agenda/cancelar-turno', {
    method: 'POST',
    body: { fecha, horaInicio },
  });
}

export async function cancelarDiaAgenda(fecha) {
  return request('/agenda/cancelar-dia', {
    method: 'POST',
    body: { fecha },
  });
}

// --- Fechas (siempre a partir del "hoy" que manda el backend, no del reloj del navegador) ---

const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

const partes = (fechaISO) => {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return { y, m, d, diaSemana: new Date(Date.UTC(y, m - 1, d)).getUTCDay() };
};

/** "2026-10-15" + 3 -> "2026-10-18" */
export function sumarDias(fechaISO, dias) {
  const { y, m, d } = partes(fechaISO);
  return new Date(Date.UTC(y, m - 1, d + dias)).toISOString().slice(0, 10);
}

/** Lunes de la semana de la fecha. */
export function lunesDe(fechaISO) {
  return sumarDias(fechaISO, -((partes(fechaISO).diaSemana + 6) % 7));
}

/** "2026-10-15" -> "Jueves 15 de octubre" */
export function textoFechaLarga(fechaISO) {
  const { m, d, diaSemana } = partes(fechaISO);
  return `${DIAS[diaSemana]} ${d} de ${MESES[m - 1]}`;
}

/** "2026-10-15" -> "Jue 15" */
export function textoDiaCorto(fechaISO) {
  const { d, diaSemana } = partes(fechaISO);
  return `${DIAS[diaSemana].slice(0, 3)} ${d}`;
}

/** "2026-10-18" -> "domingo 18/10" */
export function textoDiaYFecha(fechaISO) {
  const { m, d, diaSemana } = partes(fechaISO);
  return `${DIAS[diaSemana].toLowerCase()} ${d}/${m}`;
}

/** "12 al 18 de octubre" o, si cambia el mes, "28 de septiembre al 4 de octubre". */
export function textoSemana(desdeISO, hastaISO) {
  const desde = partes(desdeISO);
  const hasta = partes(hastaISO);
  const inicio = desde.m === hasta.m ? `${desde.d}` : `${desde.d} de ${MESES[desde.m - 1]}`;
  return `${inicio} al ${hasta.d} de ${MESES[hasta.m - 1]}`;
}

/** Turnos de la mañana (antes de las 12) y de la tarde. */
export const esDeManiana = (turno) => turno.horaInicio < '12:00';

// --- Estados ---

/**
 * Cuota del alumno en la agenda: mismo cálculo que la pantalla de Cuotas (HU-46), a partir de la cuota cruda que
 * manda el backend. En la agenda "Próxima a vencer" se muestra como "Por vencer". null si no tiene cuota.
 */
export function estadoCuotaAgenda(cuota) {
  if (!cuota) return null;
  const estado = estadoVisible(cuota);
  return { texto: estado === 'Próxima a vencer' ? 'Por vencer' : estado, tono: tonoEstadoCuota(estado) };
}

/** Colores de cada estado del turno: celda de la vista Semana y barra de ocupación de la vista Día. */
export const ESTILO_TURNO = {
  CON_LUGARES: { celda: 'bg-success-bg text-success', barra: 'bg-success', leyenda: 'Con lugares' },
  CASI_LLENO: { celda: 'bg-warning-bg text-warning-text', barra: 'bg-warning', leyenda: 'Casi lleno (80 % del cupo)' },
  COMPLETO: { celda: 'bg-error-bg text-error', barra: 'bg-error', leyenda: 'Completo' },
  CANCELADO: { celda: 'bg-row-hover text-ink-muted', barra: 'bg-line', leyenda: 'Cancelado' },
  PASADO: { celda: 'bg-thead/60 text-ink-soft', barra: 'bg-ink-muted/60', leyenda: 'Pasado' },
  BLOQUEADO: { celda: 'border border-dashed border-ink-muted/50 bg-card text-ink-muted', barra: 'bg-line', leyenda: 'Bloqueado' },
};

/** "6 de 10 lugares ocupados · 4 libres" */
export function textoOcupacion(turno) {
  const libres = turno.libres === 1 ? '1 libre' : `${turno.libres} libres`;
  return `${turno.ocupados} de ${turno.cupoMaximo} lugares ocupados · ${libres}`;
}

/** Porcentaje del cupo ocupado (para las barras). */
export const porcentajeOcupado = (turno) => (turno.cupoMaximo > 0 ? Math.min(100, (turno.ocupados / turno.cupoMaximo) * 100) : 0);
