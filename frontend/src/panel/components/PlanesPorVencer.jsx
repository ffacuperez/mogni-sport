import { CalendarClock, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge } from '../../shared/components/ui';
import { formatearFecha } from '../../shared/services/texto';
import { tituloPlan } from '../../progreso/services/progresoService';
import { textoVencePlan } from '../services/panelService';
import { CLASE_TARJETA, TONOS_ICONO } from './estilos';
import { ErrorSeccion } from './SeccionSegura';

const tonoVence = (dias) => (dias === 0 ? 'error' : dias <= 2 ? 'alert' : 'warning');

function Icono({ tono }) {
  return (
    <span className={cx('flex h-10 w-10 shrink-0 items-center justify-center rounded-lg', TONOS_ICONO[tono])}>
      <CalendarClock className="h-5 w-5" />
    </span>
  );
}

/**
 * Tarjeta de Inicio con los planes por vencer, en la misma grilla que los indicadores. Vacía ocupa una sola celda;
 * con planes ocupa todo el ancho. Si la carga falló, muestra el error en su lugar.
 * resultado: { datos: { diasAviso, planes }, error } (lo trae InicioPanel).
 */
export default function PlanesPorVencer({ resultado, onVerAlumno }) {
  if (resultado.error) {
    return (
      <div className="col-span-full">
        <ErrorSeccion titulo="No se pudieron cargar los planes por vencer" mensaje={resultado.error.message} />
      </div>
    );
  }

  const { diasAviso, planes } = resultado.datos;

  // Mismo formato que las demás tarjetas: cantidad grande, título y texto secundario
  const encabezado = (
    <div className="flex min-w-0 items-center gap-3">
      <Icono tono={planes.length === 0 ? 'neutral' : 'warning'} />
      <span className="min-w-0 flex-1">
        <span className="block text-2xl font-bold tabular-nums text-ink">{planes.length}</span>
        <span className="block text-xs text-ink-soft">Planes por vencer</span>
        <span className="mt-0.5 block text-[11px] text-ink-muted">en los próximos {diasAviso} días</span>
      </span>
    </div>
  );

  if (planes.length === 0) {
    return <div className={cx(CLASE_TARJETA, 'min-w-0')}>{encabezado}</div>;
  }

  return (
    <div className={cx(CLASE_TARJETA, 'col-span-full min-w-0')}>
      {encabezado}
      <ul className="mt-3 divide-y divide-line border-t border-line">
        {planes.map((plan) => (
          <li key={plan.planId}>
            <button
              type="button"
              onClick={() => onVerAlumno(plan.alumnoId)}
              className={cx('flex w-full cursor-pointer items-center gap-3 rounded py-2.5 text-left hover:bg-row-hover', FOCUS)}
            >
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-ink">
                  {plan.nombreAlumno} {plan.apellidoAlumno}
                </span>
                <span className="block truncate text-xs text-ink-soft">{tituloPlan(plan)}</span>
              </span>
              <span className="shrink-0 text-right">
                <Badge tone={tonoVence(plan.diasParaVencer)}>{textoVencePlan(plan.diasParaVencer)}</Badge>
                <span className="mt-1 block text-[11px] tabular-nums text-ink-muted">Fin {formatearFecha(plan.fechaFin)}</span>
              </span>
              <ChevronRight className="h-4 w-4 shrink-0 text-ink-muted" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
