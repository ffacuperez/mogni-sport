import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Search, X } from 'lucide-react';

import { cx, FOCUS, inputClass, labelClass } from './clases';

// Piezas visuales compartidas, según documentacion/MogniSystem-Design-Guide.md.
// Todos los colores salen de los tokens de index.css.

// Guía 4.1:
// primary: confirmar, crear, guardar, registrar pago, iniciar sesión
// secondary: acciones de edición (modificar, ver historial) — borde rojo sobre blanco
// ghost: cancelar, cerrar, volver
// danger: acciones destructivas (dar de baja, eliminar)
// success: acciones que restituyen (reactivar) — borde verde sobre blanco
const VARIANTES = {
  primary: 'bg-primary text-on-dark hover:bg-primary-hover',
  secondary: 'bg-card text-primary border border-primary hover:bg-error-bg',
  success: 'bg-card text-success border border-success hover:bg-success-bg',
  ghost: 'text-ink-soft hover:bg-row-hover',
  danger: 'bg-primary text-on-dark hover:bg-primary-hover',
};

const TAMANIOS = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-5 py-2.5 text-sm',
  lg: 'px-6 py-3 text-sm',
  // Barra de acciones de pantallas chicas: ícono arriba y texto abajo
  barra: 'min-h-14 flex-col px-1 py-1.5 text-center text-xs leading-tight',
};

export function Button({ variant = 'secondary', size = 'md', className, type = 'button', ...props }) {
  return (
    <button
      type={type}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors cursor-pointer',
        // Guía: deshabilitado con fondo #E0E0E0 y texto #9E9E9E, sin hover
        'disabled:cursor-not-allowed disabled:border-transparent disabled:bg-thead disabled:text-ink-muted',
        VARIANTES[variant], TAMANIOS[size], FOCUS, className,
      )}
      {...props}
    />
  );
}

/**
 * Logo: isotipo (logo.svg, cuadrado 600x600 con su propio fondo rojo redondeado) + wordmark.
 * onDark: sobre el sidebar oscuro (texto blanco).
 */
export function Logo({ size = 'md', onDark = false, className }) {
  const grande = size === 'lg';
  return (
    <div className={cx('flex items-center gap-2.5 select-none', className)}>
      <img
        src="/logo.svg"
        alt=""
        className={cx('shrink-0 object-contain', grande ? 'h-12 w-12' : 'h-9 w-9')}
      />
      <span className="leading-none">
        <span className={cx('block font-extrabold tracking-tight', onDark ? 'text-on-dark' : 'text-ink', grande ? 'text-2xl' : 'text-base')}>MOGNI</span>
        <span className={cx('block font-semibold tracking-[0.3em] text-primary', onDark && 'text-primary-light', grande ? 'mt-1 text-xs' : 'mt-0.5 text-[10px]')}>SPORT</span>
      </span>
    </div>
  );
}

/** Título de pantalla (H1 24px bold) con acciones a la derecha. */
export function PageHeader({ eyebrow, title, subtitle, actions, children }) {
  return (
    <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {eyebrow && <p className="mb-1 text-xs font-medium text-ink-soft">{eyebrow}</p>}
        <h1 className="text-2xl font-bold text-ink">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-ink-soft">{subtitle}</p>}
        {children}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  );
}

/** Card (guía 4.4): blanca, borde #E0E0E0, radio 8px, sombra suave. */
export function Panel({ className, children, as: Tag = 'section', ...props }) {
  return (
    <Tag className={cx('rounded-lg border border-line bg-card shadow-card', className)} {...props}>
      {children}
    </Tag>
  );
}

/** Botonera al pie de los formularios: Cancelar (ghost) y Confirmar (primary). */
export function FormActions({ onCancel, enviando = false }) {
  return (
    <div className="mt-8 flex flex-col-reverse gap-3 border-t border-line pt-6 sm:flex-row sm:justify-end">
      <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
      <Button type="submit" variant="primary" disabled={enviando}>{enviando ? 'Guardando...' : 'Confirmar'}</Button>
    </div>
  );
}

export function Field({ label, htmlFor, className, children }) {
  return (
    <div className={className}>
      <label htmlFor={htmlFor} className={labelClass}>{label}</label>
      {children}
    </div>
  );
}

/** Valor de solo lectura con la misma caja que un input. */
export function ReadOnly({ children, numerico = false }) {
  return (
    <p className={cx('w-full rounded-lg border border-line bg-row-hover px-3 py-2.5 text-sm font-medium text-ink', numerico && 'tabular-nums')}>
      {children}
    </p>
  );
}

export function SearchInput({ value, onChange, placeholder, label, className }) {
  return (
    <div className={cx('relative w-full sm:w-80', className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
      <input
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={label ?? placeholder}
        className={cx(inputClass, 'pl-9')}
      />
    </div>
  );
}

// Guía 4.2: badges sin borde, texto de color sobre fondo claro del mismo tono
const TONOS = {
  success: 'bg-success-bg text-success',
  warning: 'bg-warning-bg text-warning-text',
  alert: 'bg-alert-bg text-alert',
  error: 'bg-error-bg text-error',
  info: 'bg-info-bg text-info',
  neutral: 'bg-row-hover text-ink-soft',
};

export function Badge({ tone = 'neutral', className, children }) {
  return (
    <span className={cx('inline-flex items-center whitespace-nowrap rounded-full px-3 py-1 text-xs font-medium', TONOS[tone], className)}>
      {children}
    </span>
  );
}

const TONOS_STAT_FILTRO = {
  error: { icono: 'text-error', fondoIcono: 'bg-error-bg', activo: 'border-error bg-error-bg' },
  alert: { icono: 'text-alert', fondoIcono: 'bg-alert-bg', activo: 'border-alert bg-alert-bg' },
  warning: { icono: 'text-warning-text', fondoIcono: 'bg-warning-bg', activo: 'border-warning-text bg-warning-bg' },
  success: { icono: 'text-success', fondoIcono: 'bg-success-bg', activo: 'border-success bg-success-bg' },
};

/**
 * Tarjeta de resumen (guía 5.7) que además es un botón de filtro: ícono + número + etiqueta. Al tocarla se pinta y
 * filtra; al tocarla de nuevo saca el filtro.
 * compacto: para cuatro columnas angostas, sin ícono y con el número del color del tono.
 */
export function StatFiltro({ label, value, tone, icon: Icon, activo = false, compacto = false, onClick }) {
  const tono = TONOS_STAT_FILTRO[tone];
  const base = cx(
    'min-w-0 cursor-pointer rounded-lg border-2 shadow-card transition-colors',
    activo ? tono.activo : 'border-line bg-card hover:bg-row-hover',
    FOCUS,
  );
  if (compacto) {
    return (
      <button type="button" onClick={onClick} aria-pressed={activo} className={cx(base, 'min-h-15 px-0.5 py-1.5 text-center')}>
        <span className={cx('block text-xl font-bold tabular-nums', tono.icono)}>{value}</span>
        <span className="block text-[11px] leading-tight text-ink-soft">{label}</span>
      </button>
    );
  }
  return (
    <button type="button" onClick={onClick} aria-pressed={activo} className={cx(base, 'flex items-center gap-4 p-4 text-left')}>
      <span className={cx('flex h-10 w-10 shrink-0 items-center justify-center rounded-lg', tono.icono, activo ? 'bg-card' : tono.fondoIcono)}>
        <Icon className="h-5 w-5" />
      </span>
      <span className="min-w-0">
        <span className="block text-2xl font-bold tabular-nums text-ink">{value}</span>
        <span className="block text-xs text-ink-soft">{label}</span>
      </span>
    </button>
  );
}

/**
 * Tabla (guía 4.6). columns: [{ label, className }]. Opcionales: key, si label no es un texto único (por ejemplo, un
 * botón para ordenar), y ariaSort, el orden de esa columna.
 */
export function Table({ columns, children, cargando = false }) {
  return (
    <div className="relative overflow-x-auto rounded-lg border border-line">
      {cargando && <div className="loading-bar absolute inset-x-0 top-0 z-10 h-0.5" role="progressbar" aria-label="Cargando" />}
      <table className="w-full whitespace-nowrap text-left text-sm">
        <thead className="bg-thead">
          <tr>
            {columns.map((col, i) => (
              <th key={col.key ?? (col.label || i)} scope="col" aria-sort={col.ariaSort} className={cx('h-12 px-3 text-[13px] font-semibold text-ink-soft', col.className)}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-line-soft bg-card">{children}</tbody>
      </table>
    </div>
  );
}

/** Fila clickeable/seleccionable de una tabla, operable también con teclado. */
export function Row({ selected = false, onActivate, title, children }) {
  return (
    <tr
      onClick={onActivate}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onActivate?.();
        }
      }}
      tabIndex={onActivate ? 0 : undefined}
      aria-selected={onActivate ? selected : undefined}
      title={title}
      className={cx(
        'h-12 text-ink transition-colors',
        onActivate && 'cursor-pointer hover:bg-row-hover focus-visible:bg-row-hover focus-visible:outline-none',
        // Guía: primary-light como acento de la fila seleccionada
        selected && 'bg-error-bg/60 shadow-[inset_3px_0_0_var(--color-primary-light)] hover:bg-error-bg',
      )}
    >
      {children}
    </tr>
  );
}

export function EmptyRow({ colSpan, children }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-3 py-12 text-center text-sm text-ink-muted">{children}</td>
    </tr>
  );
}

/** Indicador de selección de fila. */
export function SelectDot({ selected }) {
  return (
    <span
      aria-hidden="true"
      className={cx(
        'mx-auto flex h-4 w-4 items-center justify-center rounded border transition-colors',
        selected ? 'border-primary bg-primary' : 'border-ink-muted bg-card',
      )}
    >
      {selected && <span className="h-1.5 w-1.5 rounded-sm bg-on-dark" />}
    </span>
  );
}

/**
 * Modal (guía 4.7): overlay negro al 50 %, card blanca, radio 12px. Cierra con Escape o clic afuera.
 * size: sm (formularios cortos), lg (560px) o xl (880px, tablas como el historial de pagos).
 */
export function Modal({ title, subtitle, onClose, size = 'sm', children, cerrable = true }) {
  useEffect(() => {
    if (!cerrable) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') onClose?.(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose, cerrable]);

  // Portal a body: si una página ancestro tiene transform (p. ej. animate-fade-in), el fixed quedaría encerrado en ella
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-fade-in"
      onMouseDown={(e) => { if (cerrable && e.target === e.currentTarget) onClose?.(); }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={cx(
          'w-full rounded-xl bg-card p-6 shadow-modal animate-pop-in',
          { sm: 'max-w-md', lg: 'max-w-[560px]', xl: 'max-w-[880px]' }[size] ?? 'max-w-md',
        )}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="text-xl font-semibold text-ink">{title}</h3>
            {subtitle && <p className="mt-1 text-sm text-ink-soft">{subtitle}</p>}
          </div>
          {cerrable && (
            <button type="button" onClick={onClose} aria-label="Cerrar" className={cx('-mr-2 -mt-1 rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-row-hover hover:text-ink cursor-pointer', FOCUS)}>
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
        {children}
      </div>
    </div>,
    document.body,
  );
}

/**
 * Panel lateral que entra desde la derecha sobre un overlay. Cierra con Escape, clic afuera o la X. En celular ocupa
 * todo el ancho. header: título y lo que va debajo; footer: queda fijo al pie mientras el contenido scrollea.
 */
export function Drawer({ label, header, footer, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose?.(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/40 animate-fade-in"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose?.(); }}
    >
      <div role="dialog" aria-modal="true" aria-label={label} className="flex h-full w-full max-w-[560px] flex-col bg-card shadow-modal animate-slide-in-right">
        <div className="flex items-start justify-between gap-4 px-6 pb-4 pt-6">
          <div className="min-w-0">{header}</div>
          <button type="button" onClick={onClose} aria-label="Cerrar" className={cx('-mr-2 -mt-1 shrink-0 cursor-pointer rounded-full p-2 text-ink-soft transition-colors hover:bg-row-hover hover:text-ink', FOCUS)}>
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto px-6 pb-6">{children}</div>
        {footer && <div className="px-6 pb-6">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}

/** Mensaje centrado de carga para pantallas completas. */
export function Cargando({ texto = 'Cargando...', className = 'mt-16' }) {
  return (
    <div className={cx('mx-auto flex max-w-xs flex-col items-center gap-3', className)} role="status">
      <div className="h-0.5 w-40 overflow-hidden rounded-full bg-line">
        <div className="loading-bar h-full w-full" />
      </div>
      <p className="text-xs text-ink-muted">{texto}</p>
    </div>
  );
}
