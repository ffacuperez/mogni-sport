/** Compara sin mayúsculas ni tildes ("Lucía" coincide con "lucia"). */
export const normalizar = (texto) => (texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

/** "2026-10-06" -> "06/10/2026" */
export function formatearFecha(fechaISO) {
  if (!fechaISO) return '—';
  const [y, m, d] = fechaISO.split('-');
  return `${d}/${m}/${y}`;
}

export function formatearMonto(monto) {
  return `$${Number(monto).toLocaleString('es-AR')}`;
}
