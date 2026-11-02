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
    <nav aria-label="Mi progreso" className="flex gap-1 rounded-[10px] bg-prog-tabs p-1">
      {TABS.map(({ to, texto, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => cx(
            'flex min-h-11 flex-1 items-center justify-center rounded-lg text-[14px] transition-colors',
            isActive ? 'bg-card font-bold text-primary shadow-prog-tab' : 'font-medium text-prog-soft hover:text-prog-ink',
            FOCUS,
          )}
        >
          {texto}
        </NavLink>
      ))}
    </nav>
  );
}
