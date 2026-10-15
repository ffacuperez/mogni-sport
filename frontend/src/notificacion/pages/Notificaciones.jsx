import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useOutletContext } from 'react-router';
import { Bell, BellOff, CalendarDays, ChevronLeft, CircleAlert, CreditCard, Info, TriangleAlert } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { formatearFecha } from '../../shared/services/texto';
import { clavesSinLeer, marcarTodasLeidas } from '../services/notificacionesService';
import AvisosCelular from './AvisosCelular';

const FILTROS = [
  { id: 'todas', label: 'Todas', icono: Bell },
  { id: 'turnos', label: 'Turnos', icono: CalendarDays },
  { id: 'cuota', label: 'Cuota', icono: CreditCard },
  { id: 'avisos', label: 'Avisos', icono: Info },
];

const ESTILOS = {
  CUOTA_POR_VENCER: { icono: TriangleAlert, tarjeta: 'border-warning bg-warning-bg', burbuja: 'bg-card text-warning-text' },
  CUOTA_VENCIDA: { icono: CircleAlert, tarjeta: 'border-error bg-error-bg', burbuja: 'bg-card text-error' },
};

// Fecha local en formato "2026-10-14", como las manda el backend
const aISO = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

function textoFecha(fechaISO) {
  const hoy = new Date();
  if (fechaISO === aISO(hoy)) return 'Hoy';
  hoy.setDate(hoy.getDate() - 1);
  if (fechaISO === aISO(hoy)) return 'Ayer';
  return formatearFecha(fechaISO);
}

function TarjetaNotificacion({ notificacion, nueva }) {
  const estilo = ESTILOS[notificacion.tipo] ?? { icono: Info, tarjeta: 'border-line bg-card', burbuja: 'bg-row-hover text-ink-soft' };
  const Icono = estilo.icono;
  return (
    <li className={cx('flex gap-3 rounded-xl border p-4', estilo.tarjeta)}>
      <span className={cx('flex h-10 w-10 shrink-0 items-center justify-center rounded-full', estilo.burbuja)}>
        <Icono className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <p className="text-xs text-ink-soft">{textoFecha(notificacion.fecha)}</p>
          {nueva && <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-primary" aria-label="Sin leer" />}
        </div>
        <h2 className="mt-0.5 text-sm font-bold text-ink">{notificacion.titulo}</h2>
        <p className="mt-1 text-sm leading-relaxed text-ink-soft">{notificacion.mensaje}</p>
      </div>
    </li>
  );
}

export default function Notificaciones() {
  const navigate = useNavigate();
  const location = useLocation();
  const { notificaciones, cargandoNotificaciones, errorNotificaciones, cargarNotificaciones, alVerNotificaciones } = useOutletContext();
  const [filtro, setFiltro] = useState('todas');
  // Cuáles eran nuevas al entrar: se siguen marcando mientras el alumno está en la pantalla
  const [nuevas, setNuevas] = useState(null);

  // Al entrar se vuelven a pedir (pudo haberse registrado un pago) y quedan marcadas como vistas
  useEffect(() => {
    cargarNotificaciones().then((lista) => {
      setNuevas(clavesSinLeer(lista));
      marcarTodasLeidas(lista);
      alVerNotificaciones();
    });
  }, []);

  // Si se entró directo a la URL no hay a dónde volver dentro de la app
  const volver = () => (location.key === 'default' ? navigate('/mi-cuenta') : navigate(-1));

  const visibles = notificaciones.filter(n => filtro === 'todas' || n.categoria === filtro);
  const cantidadNuevas = nuevas?.size ?? 0;

  return (
    <div className="animate-fade-in">
      <div className="mb-5 flex items-center gap-3">
        <button
          type="button"
          onClick={volver}
          aria-label="Volver"
          className={`flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-card text-ink shadow-card transition-colors hover:bg-row-hover ${FOCUS}`}
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-ink">Notificaciones</h1>
          <p className="text-xs text-ink-soft">
            {cantidadNuevas > 0 ? `${cantidadNuevas} sin leer` : 'Estás al día'}
          </p>
        </div>
      </div>

      <AvisosCelular />

      <div className="-mx-4 mb-5 flex gap-2 overflow-x-auto px-4 pb-1" role="tablist" aria-label="Filtrar notificaciones">
        {FILTROS.map(({ id, label, icono: Icono }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={filtro === id}
            onClick={() => setFiltro(id)}
            className={cx(
              'flex shrink-0 cursor-pointer items-center gap-1.5 rounded-full border px-4 py-2 text-sm font-medium transition-colors',
              filtro === id ? 'border-primary bg-error-bg text-primary' : 'border-line bg-card text-ink-soft hover:bg-row-hover',
              FOCUS,
            )}
          >
            <Icono className="h-4 w-4" /> {label}
          </button>
        ))}
      </div>

      {errorNotificaciones ? (
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{errorNotificaciones}</span>
        </div>
      ) : cargandoNotificaciones && notificaciones.length === 0 ? (
        <Cargando texto="Cargando notificaciones" />
      ) : visibles.length === 0 ? (
        <div className="mt-12 flex flex-col items-center gap-3 text-center">
          <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
            <BellOff className="h-6 w-6" />
          </span>
          <p className="text-sm font-semibold text-ink">No tenés notificaciones</p>
          <p className="max-w-60 text-xs text-ink-soft">Te avisamos acá cuando tu cuota esté por vencer.</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {visibles.map(n => (
            <TarjetaNotificacion key={n.clave} notificacion={n} nueva={nuevas?.has(n.clave)} />
          ))}
        </ul>
      )}
    </div>
  );
}
