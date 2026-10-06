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
 * ATRASADA -> "Vencida"; PENDIENTE que vence en 5 días o menos -> "Próxima a vencer".
 */
function estadoVisible(cuota) {
  if (cuota.estado === 'PAGADA') return 'Pagada';
  if (cuota.estado === 'ATRASADA') return 'Vencida';
  return diasHasta(cuota.fechaVencimiento) <= DIAS_PROXIMO_A_VENCER ? 'Próxima a vencer' : 'Pendiente';
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

/** Clases de Tailwind para el badge de estado (mismo patrón que ya usa AlumnoDashboard). */
export function clasesEstadoCuota(estado) {
  switch (estado) {
    case 'Pagada':
      return 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40';
    case 'Próxima a vencer':
      return 'bg-amber-950/60 text-amber-400 border border-amber-800/40';
    case 'Vencida':
      return 'bg-red-950/60 text-red-400 border border-red-800/40';
    default:
      return 'bg-slate-800 text-slate-400 border border-slate-700';
  }
}

export function formatearMonto(monto) {
  return `$${Number(monto).toLocaleString('es-AR')}`;
}

export function formatearFecha(fechaISO) {
  if (!fechaISO) return '—';
  const [y, m, d] = fechaISO.split('-');
  return `${d}/${m}/${y}`;
}

/** "2026-10" -> "10/2026" */
export function formatearPeriodo(periodo) {
  if (!periodo) return '—';
  const [y, m] = periodo.split('-');
  return `${m}/${y}`;
}
