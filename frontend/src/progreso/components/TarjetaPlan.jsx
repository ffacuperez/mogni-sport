import { Link } from 'react-router';
import { Calendar, Check, ChevronRight, Layers, Target } from 'lucide-react';

import { textoNivel, textoObjetivo } from '../../plantilla/services/plantillasService';
import { cx, FOCUS } from '../../shared/components/clases';
import { Panel } from '../../shared/components/ui';
import { textoRangoPlan, textoSemanas, tituloPlan } from '../services/progresoService';
import { useProgreso } from '../services/ProgresoContext';
import BadgeEstadoPlan from './BadgeEstadoPlan';

function Dato({ icono: Icono, children }) {
  return (
    <span className="flex items-center gap-1.5 text-sm text-ink-soft">
      <Icono className="h-3.5 w-3.5 shrink-0" />
      {children}
    </span>
  );
}

/** Un plan del listado: el activo lleva al tab "Plan actual" y va destacado; uno anterior abre su detalle. */
export default function TarjetaPlan({ plan }) {
  const { rutaBase } = useProgreso();
  const activo = plan.estado === 'ACTIVO';
  return (
    <Panel
      as={Link}
      to={activo ? rutaBase : `${rutaBase}/planes/${plan.id}`}
      className={cx('flex items-center gap-2.5 py-3.5 pl-4 pr-3 transition-colors hover:bg-row-hover', activo && 'border-l-4 border-l-primary', FOCUS)}
    >
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-sm font-bold text-ink">{tituloPlan(plan)}</h3>
          <BadgeEstadoPlan estado={plan.estado} />
        </div>
        <Dato icono={Calendar}>{textoRangoPlan(plan)}</Dato>
        {/* MOGNI-268: nivel y objetivo del plan */}
        {(plan.nivel || plan.objetivo) && (
          <Dato icono={Target}>{[plan.nivel && textoNivel(plan.nivel), plan.objetivo && textoObjetivo(plan.objetivo)].filter(Boolean).join(' · ')}</Dato>
        )}
        <div className="flex flex-wrap gap-x-3.5 gap-y-1">
          <Dato icono={Layers}>{textoSemanas(plan.cantidadSemanas)}</Dato>
          <Dato icono={Check}>{plan.sesionesRegistradas} de {plan.sesionesTotales} sesiones</Dato>
        </div>
      </div>
      <ChevronRight className="h-5 w-5 shrink-0 text-ink-muted" />
    </Panel>
  );
}
