import { request } from '../../shared/services/httpClient';

const DIAS_PROXIMO_A_VENCER = 5; // Criterio de aceptación HU-46.

export const MEDIOS_DE_PAGO = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
];

function hoySinHora() {
  return new Date(new Date().toDateString());
}

/** Días que faltan para el vencimiento (negativo si ya venció). */
function diasHasta(fechaISO) {
  const fecha = new Date(`${fechaISO}T00:00:00`);
  return Math.round((fecha - hoySinHora()) / 86400000);
}

/**
 * Estado que se muestra en la pantalla, a partir del estado del backend:
 * ATRASADA -> "Vencida"; PENDIENTE que vence en 5 días o menos -> "Próxima a vencer";
 * PAGADA o PENDIENTE con más de 5 días por delante -> "Al día" (no requiere ninguna acción todavía).
 * Si una cuota "Al día" está paga o no, lo indica `pagable` (y el aviso bajo la fecha).
 * También lo usa la agenda (MOGNI-257) con la cuota cruda que manda el backend ({ estado, fechaVencimiento }).
 */
export function estadoVisible(cuota) {
  if (cuota.estado === 'ATRASADA') return 'Vencida';
  if (cuota.estado === 'PENDIENTE' && diasHasta(cuota.fechaVencimiento) <= DIAS_PROXIMO_A_VENCER) return 'Próxima a vencer';
  return 'Al día';
}

// Backend -> forma que usan las pantallas
const fromApi = (c) => ({
  id: c.id,
  alumnoId: c.alumnoId,
  nombre: c.alumnoNombre,
  apellido: c.alumnoApellido,
  dni: c.alumnoDni,
  membresia: c.membresiaNombre,
  periodo: c.periodo,
  monto: Number(c.montoEsperado),
  vencimiento: c.fechaVencimiento,
  estadoApi: c.estado,
  estado: estadoVisible(c),
  diasParaVencer: diasHasta(c.fechaVencimiento),
  // HU-45: solo se pueden pagar las cuotas que todavía no están pagas
  pagable: c.estado === 'PENDIENTE' || c.estado === 'ATRASADA',
});

/** Cuotas de los alumnos activos. Filtros opcionales: estado del backend (PENDIENTE/PAGADA/ATRASADA) y alumnoId. */
export async function listarCuotas({ estado, alumnoId } = {}) {
  const params = new URLSearchParams();
  if (estado) params.set('estado', estado);
  if (alumnoId) params.set('alumnoId', alumnoId);
  const query = params.toString();
  const data = await request(`/cuotas${query ? `?${query}` : ''}`);
  return data.map(fromApi);
}

/** El monto no lo ingresa el propietario: el backend siempre usa el monto esperado de la cuota, igual que la fecha (siempre hoy). */
export function registrarPago({ cuotaId, medioDePago }) {
  return request('/pagos', { method: 'POST', body: { cuotaId, medioDePago } });
}

/** HU-48: historial de pagos de un alumno, ya ordenado por el backend del más reciente al más antiguo. */
export async function obtenerHistorialDePagos(alumnoId) {
  const data = await request(`/alumnos/${alumnoId}/pagos`);
  return data.map((p) => ({
    id: p.pagoId,
    monto: Number(p.monto),
    medioDePago: p.medioDePago,
    fecha: p.fechaPago,
    periodo: p.periodo,
  }));
}

/** Etiqueta legible de un medio de pago (EFECTIVO/TRANSFERENCIA -> Efectivo/Transferencia). */
export function labelMedioDePago(medio) {
  return MEDIOS_DE_PAGO.find((m) => m.value === medio)?.label ?? medio;
}

/** Aviso corto bajo la fecha: pagada, cuántos días faltan o cuántos pasaron. null si no hace falta. */
export function avisoVencimiento(cuota) {
  if (!cuota.pagable) return 'Pagada';
  const d = cuota.diasParaVencer;
  if (cuota.estado === 'Próxima a vencer') {
    if (d <= 0) return 'Vence hoy';
    return d === 1 ? 'Vence mañana' : `Vence en ${d} días`;
  }
  if (cuota.estado === 'Vencida') {
    const atraso = Math.max(-d, 1);
    return atraso === 1 ? 'Venció hace 1 día' : `Venció hace ${atraso} días`;
  }
  return null;
}

/** Tono del badge de estado (ver Badge en shared/components/ui). */
export function tonoEstadoCuota(estado) {
  switch (estado) {
    case 'Al día':
      return 'success';
    case 'Próxima a vencer':
      return 'warning';
    case 'Vencida':
      return 'error';
    default:
      return 'neutral';
  }
}

export { formatearFecha, formatearMonto } from '../../shared/services/texto';

/** "2026-10" -> "10/2026" */
export function formatearPeriodo(periodo) {
  if (!periodo) return '—';
  const [y, m] = periodo.split('-');
  return `${m}/${y}`;
}
