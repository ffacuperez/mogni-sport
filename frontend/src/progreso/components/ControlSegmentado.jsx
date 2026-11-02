import { NavLink } from 'react-router';

import { cx, FOCUS } from '../../shared/components/clases';

const TABS = [
  { to: '/mi-cuenta/progreso', texto: 'Plan actual', end: true },
  { to: '/mi-cuenta/progreso/planes', texto: 'Planes' },
  { to: '/mi-cuenta/progreso/sesiones', texto: 'Sesiones' },
];

/** Selector de tabs de Mi progreso. Cada tab es una ruta, así volver desde un detalle deja el tab donde estaba. */
export default function ControlSegmentado() {
  return (
    <nav aria-label="Mi progreso" className="mb-4 grid grid-cols-3 gap-1 rounded-xl bg-thead/70 p-1">
      {TABS.map(({ to, texto, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => cx(
            'flex min-h-11 items-center justify-center rounded-lg text-sm transition-colors',
            isActive ? 'bg-card font-semibold text-primary shadow-card' : 'font-medium text-ink-muted hover:text-ink',
            FOCUS,
          )}
        >
          {texto}
        </NavLink>
      ))}
    </nav>
  );
}
