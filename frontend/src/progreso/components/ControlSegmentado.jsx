import { NavLink } from 'react-router';

import { cx, FOCUS } from '../../shared/components/clases';
import { useProgreso } from '../services/ProgresoContext';

const tabs = (rutaBase) => [
  { to: rutaBase, texto: 'Plan actual', end: true },
  { to: `${rutaBase}/planes`, texto: 'Planes' },
  { to: `${rutaBase}/sesiones`, texto: 'Sesiones' },
];

/** Selector de tabs de Mi progreso. Cada tab es una ruta, así volver desde un detalle deja el tab donde estaba. */
export default function ControlSegmentado() {
  const { rutaBase, textos } = useProgreso();
  return (
    <nav aria-label={textos.navegacion} className="mb-4 grid grid-cols-3 gap-1 rounded-xl bg-thead/70 p-1">
      {tabs(rutaBase).map(({ to, texto, end }) => (
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
