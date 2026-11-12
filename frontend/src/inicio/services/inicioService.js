import { request } from '../../shared/services/httpClient';

// MOGNI-281: el inicio del alumno logueado. El backend decide el estado de la cuota, si puede reservar y por qué no;
// acá solo se pide y se muestra.

const cuotaFromApi = (c) => ({
  // null: no tiene cuotas (datos viejos); no hay aviso
  situacion: c?.situacion ?? null,
  fechaVencimiento: c?.fechaVencimiento ?? null,
  diasParaVencer: c?.diasParaVencer ?? null,
});

const inicioFromApi = (d) => ({
  hoy: d.hoy,
  nombre: d.nombre,
  membresia: d.membresia ?? null,
  diasPorSemana: d.diasPorSemana,
  activo: d.activo,
  cuota: cuotaFromApi(d.cuota),
  proximoTurno: d.proximoTurno ?? null,
  turnosSemana: {
    usados: d.turnosSemana.usados,
    total: d.turnosSemana.total,
    paraRecuperar: d.turnosSemana.paraRecuperar ?? 0,
  },
  puedeReservar: d.puedeReservar,
  motivoNoReserva: d.motivoNoReserva ?? null,
  planActivo: d.planActivo ?? null,
});

/** Todo lo que muestra el inicio, en una sola llamada. */
export async function consultarMiInicio() {
  return inicioFromApi(await request('/mi-inicio'));
}

/** Por qué no puede reservar, como se le explica al alumno debajo del botón. */
export const TEXTO_MOTIVO_NO_RESERVA = {
  EN_MORA: 'Tenés la cuota en mora: regularizá el pago para volver a reservar.',
  CUOTA_VENCIDA: 'Tu cuota venció: pagala hoy para poder reservar los turnos de mañana.',
  CUPO_SEMANAL_AGOTADO: 'Ya usaste todos tus turnos de esta semana.',
  SIN_MEMBRESIA_ACTIVA: 'No tenés una membresía activa para reservar turnos.',
  SIN_DIAS_DISPONIBLES: 'Ya tenés un turno hoy y otro mañana.',
};

/** "Te queda 1 de 3" / "Te quedan 2 de 3" / "Te quedan 0 de 3". */
export function textoTurnosQueQuedan(usados, total) {
  const quedan = Math.max(0, total - usados);
  return `${quedan === 1 ? 'Te queda' : 'Te quedan'} ${quedan} de ${total}`;
}

/** El aviso de la cuota según su situación; null si no corresponde (al día o sin cuotas). */
export function avisoCuota({ situacion, diasParaVencer }) {
  if (situacion === 'PROXIMA_A_VENCER') {
    if (diasParaVencer === 0) return { tono: 'warning', texto: 'Tu cuota vence hoy' };
    if (diasParaVencer === 1) return { tono: 'warning', texto: 'Tu cuota vence mañana' };
    return { tono: 'warning', texto: `Tu cuota vence en ${diasParaVencer} días` };
  }
  if (situacion === 'VENCIDA') return { tono: 'alert', texto: 'Tu cuota venció ayer: renovala para no entrar en mora' };
  if (situacion === 'EN_MORA') return { tono: 'error', texto: 'Tu cuota está en mora: regularizá el pago' };
  return null;
}
