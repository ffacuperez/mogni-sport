import { Calendar, Target } from 'lucide-react';

import { Panel } from '../../shared/components/ui';
import { formatearFecha } from '../../shared/services/texto';
import { tituloPlan } from '../services/progresoService';
import BadgeEstadoPlan from './BadgeEstadoPlan';
import BarraSemanas from './BarraSemanas';

function Dato({ icono: Icono, children }) {
  return (
    <p className="flex items-center gap-1.5 text-sm text-ink-soft">
      <Icono className="h-3.5 w-3.5 shrink-0" />
      {children}
    </p>
  );
}

/** Resumen del plan: método y mesociclo, objetivo, fecha de inicio y avance por semanas. */
export default function TarjetaResumenPlan({ plan }) {
  return (
    <Panel aria-label="Resumen del plan" className="flex flex-col gap-2.5 p-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-wide text-primary">Método Mogni Sport</p>
          <h2 className="text-lg font-bold text-ink">{tituloPlan(plan)}</h2>
        </div>
        <BadgeEstadoPlan estado={plan.estado} />
      </div>

      {plan.objetivoMesociclo && <Dato icono={Target}>Objetivo: {plan.objetivoMesociclo}</Dato>}
      <Dato icono={Calendar}>Inicio: {formatearFecha(plan.fechaInicio)}</Dato>

      <div className="flex items-center justify-between gap-2 border-t border-line pt-2.5">
        <p className="whitespace-nowrap text-sm font-bold text-ink">Semana {plan.semanaActual} de {plan.cantidadSemanas}</p>
        <p className="text-right text-xs text-ink-muted">{plan.sesionesRegistradas} de {plan.sesionesTotales} sesiones registradas</p>
      </div>
      <BarraSemanas total={plan.cantidadSemanas} completas={plan.semanaActual} />
    </Panel>
  );
}
