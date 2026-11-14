import { Calendar, Target } from 'lucide-react';

import { Panel } from '../../shared/components/ui';
import { formatearFecha } from '../../shared/services/texto';
import { textoObjetivo } from '../../plantilla/services/plantillasService';
import { textoRangoPlan } from '../services/progresoService';
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

/** Resumen del plan: nombre, objetivo, fechas y avance por semanas (completo si ya finalizó). */
export default function TarjetaResumenPlan({ plan }) {
  const activo = plan.estado === 'ACTIVO';
  return (
    <Panel aria-label="Resumen del plan" className="flex flex-col gap-2.5 p-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-wide text-primary">Método Mogni Sport</p>
          <h2 className="text-lg font-bold text-ink">{plan.nombre}</h2>
        </div>
        <BadgeEstadoPlan estado={plan.estado} />
      </div>

      {plan.objetivo && <Dato icono={Target}>Objetivo: {textoObjetivo(plan.objetivo)}</Dato>}
      {/* HU-266: el alumno ve hasta cuándo dura su plan */}
      <Dato icono={Calendar}>
        {activo ? `Inicio: ${formatearFecha(plan.fechaInicio)} · Fin: ${formatearFecha(plan.fechaFin)}` : textoRangoPlan(plan)}
      </Dato>

      <div className="flex items-center justify-between gap-2 border-t border-line pt-2.5">
        <p className="whitespace-nowrap text-sm font-bold text-ink">
          {activo ? `Semana ${plan.semanaActual} de ${plan.cantidadSemanas}` : `${plan.cantidadSemanas} de ${plan.cantidadSemanas} semanas`}
        </p>
        <p className="text-right text-xs text-ink-muted">{plan.sesionesRegistradas} de {plan.sesionesTotales} sesiones registradas</p>
      </div>
      <BarraSemanas total={plan.cantidadSemanas} completas={plan.semanaActual} />
    </Panel>
  );
}
