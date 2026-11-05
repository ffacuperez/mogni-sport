import { CircleCheck, Pencil } from 'lucide-react';

import { Badge, Button, Panel } from '../../shared/components/ui';
import { formatearFecha } from '../../shared/services/texto';
import { textoNivel, textoObjetivo } from '../../plantilla/services/plantillasService';
import BadgeEstadoPlan from '../components/BadgeEstadoPlan';
import BarraSemanas from '../components/BarraSemanas';
import { sesionesEsperadasHastaEstaSemana, sesionesPorSemana, tituloPlan } from '../services/progresoService';

function Dato({ label, children }) {
  return (
    <div className="min-w-0">
      <p className="text-xs text-ink-soft">{label}</p>
      <div className="mt-1 text-[17px] font-semibold text-ink">{children}</div>
    </div>
  );
}

/**
 * Resumen del plan en el panel: método y mesociclo, nivel, objetivo, inicio, sesiones por semana y el avance por
 * semanas. onAjustar: solo en el plan activo, lleva a modificarlo.
 */
export default function ResumenPlanPanel({ plan, onAjustar }) {
  const activo = plan.estado === 'ACTIVO';
  const semana = activo ? plan.semanaActual : plan.cantidadSemanas;
  const esperadas = sesionesEsperadasHastaEstaSemana(plan);
  const objetivo = plan.objetivo ? textoObjetivo(plan.objetivo) : plan.objetivoMesociclo;

  return (
    <Panel aria-label="Resumen del plan" className="flex flex-col gap-5 px-6 py-5">
      <div className="flex flex-col items-start justify-between gap-x-10 gap-y-4 md:flex-row">
        <div className="grid w-full flex-1 grid-cols-1 gap-x-10 gap-y-4 sm:grid-cols-2 md:flex md:w-auto md:flex-wrap">
          <Dato label="Método · Mesociclo">
            <span className="flex flex-wrap items-center gap-2">
              {tituloPlan(plan)}
              <BadgeEstadoPlan estado={plan.estado} />
            </span>
          </Dato>
          {plan.nivel && <Dato label="Nivel"><Badge className="font-semibold">{textoNivel(plan.nivel)}</Badge></Dato>}
          {objetivo && <Dato label="Objetivo">{objetivo}</Dato>}
          <Dato label="Fecha de inicio"><span className="tabular-nums">{formatearFecha(plan.fechaInicio)}</span></Dato>
          <Dato label="Sesiones por semana"><span className="tabular-nums">{sesionesPorSemana(plan)}</span></Dato>
        </div>
        {activo && onAjustar && (
          <Button variant="secondary" onClick={onAjustar}>
            <Pencil className="h-4 w-4" /> Ajustar plan
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-2 md:flex-row md:items-center md:gap-4">
        <p className="shrink-0 whitespace-nowrap text-sm font-semibold text-ink">Semana {semana} de {plan.cantidadSemanas}</p>
        <div className="flex-1"><BarraSemanas total={plan.cantidadSemanas} completas={semana} /></div>
        <p className="flex shrink-0 items-center gap-1.5 text-sm font-medium text-success">
          <CircleCheck className="h-4 w-4" />
          {plan.sesionesRegistradas} de {esperadas} sesiones registradas{activo ? ' hasta esta semana' : ''}
        </p>
      </div>
    </Panel>
  );
}
