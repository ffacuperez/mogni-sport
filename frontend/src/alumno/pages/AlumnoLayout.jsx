import { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router';
import { Bell, CalendarDays, ChartLine, House, User } from 'lucide-react';

import { Logo } from '../../shared/components/ui';
import BotonInstalarApp from '../../shared/components/BotonInstalarApp';
import { cx, FOCUS } from '../../shared/components/clases';
import { contarSinLeer, listarNotificaciones } from '../../notificacion/services/notificacionesService';
const SECCIONES = [
  { to: 'inicio', label: 'Inicio', icono: House },
  { to: 'turnos', label: 'Turnos', icono: CalendarDays },
  { to: 'progreso', label: 'Progreso', icono: ChartLine },
  { to: 'perfil', label: 'Perfil', icono: User },
];

/**
 * Estructura de la app del alumno: barra superior con la campana de notificaciones, la sección actual
 * y la barra de navegación inferior. Las notificaciones se cargan acá para que la campana muestre el contador
 * en cualquier sección; las pantallas las reciben por el contexto del Outlet.
 */
export default function AlumnoLayout({ onLogout }) {
  const [notificaciones, setNotificaciones] = useState([]);
  const [errorNotificaciones, setErrorNotificaciones] = useState(null);
  const [cargandoNotificaciones, setCargandoNotificaciones] = useState(true);
  const [sinLeer, setSinLeer] = useState(0);

  // Al recargar se mantiene la lista anterior a la vista hasta que llega la nueva (no vuelve a "Cargando")
  const cargarNotificaciones = useCallback(() => {
    return listarNotificaciones()
      .then((lista) => {
        setNotificaciones(lista);
        setSinLeer(contarSinLeer(lista));
        setErrorNotificaciones(null);
        return lista;
      })
      .catch((err) => {
        setErrorNotificaciones(err.message);
        return [];
      })
      .finally(() => setCargandoNotificaciones(false));
  }, []);

  useEffect(() => { cargarNotificaciones(); }, [cargarNotificaciones]);

  const contexto = {
    onLogout,
    notificaciones,
    cargandoNotificaciones,
    errorNotificaciones,
    cargarNotificaciones,
    // La pantalla de notificaciones avisa cuando el alumno ya las vio, para apagar el contador de la campana
    alVerNotificaciones: () => setSinLeer(0),
  };

  return (
    <div className="flex min-h-screen flex-col bg-page text-ink">
      <header className="sticky top-0 z-30 bg-sidebar">
        <div className="mx-auto flex h-16 max-w-lg items-center justify-between gap-4 px-4">
          <Logo onDark />
          <div className="flex items-center gap-2">
            <BotonInstalarApp />
            <Link
              to="notificaciones"
              aria-label={sinLeer > 0 ? `Notificaciones, ${sinLeer} sin leer` : 'Notificaciones'}
              className={`relative rounded-lg p-2 text-on-dark transition-colors hover:bg-sidebar-hover ${FOCUS}`}
            >
              <Bell className="h-6 w-6" />
              {sinLeer > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-bold tabular-nums text-on-dark ring-2 ring-sidebar">
                  {sinLeer}
                </span>
              )}
            </Link>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-lg flex-1 px-4 pb-24 pt-6">
        <Outlet context={contexto} />
      </main>

      <nav
        aria-label="Secciones"
        className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-card pb-[env(safe-area-inset-bottom)]"
      >
        <ul className="mx-auto grid max-w-lg grid-cols-4">
          {SECCIONES.map(({ to, label, icono: Icono }) => (
            <li key={to}>
              <NavLink
                to={to}
                className={({ isActive }) => cx(
                  'flex flex-col items-center gap-1 py-2.5 text-xs transition-colors',
                  isActive ? 'font-semibold text-primary' : 'font-medium text-ink-muted hover:text-ink-soft',
                  FOCUS,
                )}
              >
                <Icono className="h-6 w-6" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}
