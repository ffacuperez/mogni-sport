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

// Backend -> forma que usan las pantallas: una fila por alumno activo con su última cuota
const fromApi = (c) => ({
  alumnoId: c.alumnoId,
  nombre: c.alumnoNombre,
  apellido: c.alumnoApellido,
  dni: c.alumnoDni,
  cuotaId: c.cuotaId,
  membresia: c.membresiaNombre,
  inicio: c.fechaInicio,
  vencimiento: c.fechaVencimiento,
  // Precio actual de la membresía: lo que va a pagar al renovar
  precio: c.precioMembresia != null ? Number(c.precioMembresia) : null,
  situacion: c.situacion,
  estado: estadoVisible(c),
  diasEnMora: c.diasEnMora,
  puedeRenovar: c.puedeRenovar,
  renovableDesde: c.renovableDesde,
});

/** MOGNI-46: una fila por alumno activo, ya ordenadas por el backend (en mora, vencidas y por vencimiento). */
export async function listarCuotas() {
  const data = await request('/cuotas');
  return data.map(fromApi);
}

const numeroONull = (valor) => (valor != null ? Number(valor) : null);

/**
 * MOGNI-45: vista previa de la renovación (período que va a cubrir, precio, recargo si está en mora y total).
 * El cálculo lo hace el backend; si no se puede renovar, motivo dice por qué.
 */
export async function obtenerRenovacion(alumnoId) {
  const r = await request(`/alumnos/${alumnoId}/renovacion`);
  return {
    ...r,
    precio: numeroONull(r.precio),
    recargo: numeroONull(r.recargo),
    total: numeroONull(r.total),
  };
}

/** Renovación del alumno: el backend crea la cuota del período siguiente y su pago (monto y fecha automáticos). */
export function registrarPago({ alumnoId, medioDePago }) {
  return request('/pagos', { method: 'POST', body: { alumnoId, medioDePago } });
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
