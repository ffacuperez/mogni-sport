import { request } from '../../shared/services/httpClient';
import { formatearFecha } from '../../shared/services/texto';

export const MEDIOS_DE_PAGO = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
];

/** Situación de pago que calcula el backend (no se guarda) -> texto de la pantalla (HU-46). */
export const SITUACIONES = {
  AL_DIA: 'Al día',
  PROXIMA_A_VENCER: 'Próxima a vencer',
  VENCIDA: 'Vencida',
  EN_MORA: 'En mora',
};

/**
 * Texto de la situación de pago del alumno, según su última cuota. La calcula el backend; también lo usa la agenda
 * (MOGNI-257) con la cuota que manda el backend ({ situacion, fechaVencimiento }). Sin cuotas (datos viejos): "Sin cuotas".
 */
export function estadoVisible(cuota) {
  return SITUACIONES[cuota?.situacion] ?? 'Sin cuotas';
}

const numeroONull = (valor) => (valor != null ? Number(valor) : null);

/**
 * MOGNI-45: vista previa de la renovación (período que va a cubrir, precio, recargo si está en mora y total).
 * Con membresiaId, renovando con otra membresía: dice además desde cuándo rige y si queda programada.
 * El cálculo lo hace el backend; si no se puede renovar, motivo dice por qué.
 */
export async function obtenerRenovacion(alumnoId, membresiaId = null) {
  const query = membresiaId ? `?membresiaId=${membresiaId}` : '';
  const r = await request(`/alumnos/${alumnoId}/renovacion${query}`);
  return {
    ...r,
    precio: numeroONull(r.precio),
    recargo: numeroONull(r.recargo),
    total: numeroONull(r.total),
  };
}

/**
 * Renovación del alumno: el backend crea la cuota del período siguiente y su pago (monto y fecha automáticos). Con
 * membresiaId renueva con otra membresía; sin él, con la actual.
 */
export function registrarPago({ alumnoId, medioDePago, membresiaId = null }) {
  return request('/pagos', { method: 'POST', body: { alumnoId, medioDePago, membresiaId } });
}

/** HU-48: historial de pagos de un alumno, ya ordenado por el backend del más reciente al más antiguo. */
export async function obtenerHistorialDePagos(alumnoId) {
  const data = await request(`/alumnos/${alumnoId}/pagos`);
  return data.map((p) => ({
    id: p.pagoId,
    fecha: p.fechaPago,
    medioDePago: p.medioDePago,
    inicio: p.fechaInicio,
    vencimiento: p.fechaVencimiento,
    membresia: p.membresiaNombre,
    precio: Number(p.precio),
    recargo: Number(p.recargo ?? 0),
    monto: Number(p.monto),
    cambioDeMembresia: p.cambioDeMembresia,
  }));
}

/** Etiqueta legible de un medio de pago (EFECTIVO/TRANSFERENCIA -> Efectivo/Transferencia). */
export function labelMedioDePago(medio) {
  return MEDIOS_DE_PAGO.find((m) => m.value === medio)?.label ?? medio;
}

/** "06/10/2026 al 05/11/2026" */
export function formatearPeriodo(inicio, fin) {
  if (!inicio || !fin) return '—';
  return `${formatearFecha(inicio)} al ${formatearFecha(fin)}`;
}

/** Aviso corto bajo la situación: hace cuánto está en mora. Las demás situaciones no llevan aviso (la fila ya muestra el vencimiento). */
export function avisoVencimiento(cuota) {
  if (cuota.situacion === 'EN_MORA') {
    if (cuota.diasEnMora === 0) return 'Desde hoy';
    return cuota.diasEnMora === 1 ? 'Hace 1 día' : `Hace ${cuota.diasEnMora} días`;
  }
  return null;
}

/** Texto que explica por qué no se puede registrar el pago de esta cuota. */
export function motivoSinRenovacion(cuota) {
  if (cuota?.renovableDesde) return `El pago se habilita desde el ${formatearFecha(cuota.renovableDesde)}.`;
  return 'El alumno no tiene una membresía activa.';
}

/** Tono del badge de estado (ver Badge en shared/components/ui). */
export function tonoEstadoCuota(estado) {
  switch (estado) {
    case 'Al día':
      return 'success';
    case 'Próxima a vencer':
      return 'warning';
    case 'Vencida':
      return 'alert';
    case 'En mora':
      return 'error';
    default:
      return 'neutral';
  }
}

export { formatearFecha, formatearMonto } from '../../shared/services/texto';
