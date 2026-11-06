import { NavLink } from 'react-router';

import { cx, FOCUS } from '../../shared/components/clases';
import { useProgreso } from '../services/ProgresoContext';

const tabs = (rutaBase) => [
  { to: rutaBase, texto: 'Plan actual', end: true },
  { to: `${rutaBase}/planes`, texto: 'Historial de planes' },
  { to: `${rutaBase}/sesiones`, texto: 'Historial de sesiones' },
];

/** Las vistas del progreso en el panel, a la derecha de las pestañas de la ficha. Cada una es una ruta. */
export default function ControlSegmentadoPanel() {
  const { rutaBase, textos } = useProgreso();
  return (
    <nav aria-label={textos.navegacion} className="mb-2 flex max-w-full gap-1 self-start overflow-x-auto rounded-xl bg-thead/70 p-1 md:self-auto">
      {tabs(rutaBase).map(({ to, texto, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => cx(
            'flex h-9 shrink-0 items-center whitespace-nowrap rounded-lg px-4 text-sm transition-colors',
            isActive ? 'bg-card font-semibold text-primary shadow-card' : 'font-medium text-ink-soft hover:text-ink',
            FOCUS,
          )}
        >
          {texto}
        </NavLink>
      ))}
    </nav>
  );
}
