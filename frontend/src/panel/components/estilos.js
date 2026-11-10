// Estilos compartidos por las tarjetas de Inicio: así "Planes por vencer" se ve igual que los indicadores.

/** Grilla de las tarjetas: una columna en el celular, dos desde 420px y tres en pantallas grandes. */
export const GRILLA_TARJETAS = 'grid grid-cols-1 gap-3 min-[420px]:grid-cols-2 lg:grid-cols-3';

/** Caja de cada tarjeta (guía 4.4). */
export const CLASE_TARJETA = 'rounded-lg border border-line bg-card p-4 shadow-card';

/** Colores del ícono de una tarjeta según su tono. */
export const TONOS_ICONO = {
  neutral: 'bg-row-hover text-ink-soft',
  primary: 'bg-error-bg text-primary',
  success: 'bg-success-bg text-success',
  warning: 'bg-warning-bg text-warning-text',
  error: 'bg-error-bg text-error',
};
