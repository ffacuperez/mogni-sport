// Clases compartidas (los colores salen de los tokens de index.css).

export const cx = (...clases) => clases.filter(Boolean).join(' ');

export const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary';

// Guía 4.3: fondo #FAFAFA, borde #E0E0E0, foco rojo, radio 8px, padding 10px 12px, 14px
export const inputClass = cx(
  'w-full rounded-lg border border-line bg-input px-3 py-2.5 text-sm text-ink placeholder-ink-muted transition-colors',
  'focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary',
  'disabled:cursor-not-allowed disabled:bg-thead disabled:text-ink-muted',
);

// Guía: label 12px Medium, texto secundario
export const labelClass = 'mb-1.5 block text-xs font-medium text-ink-soft';
