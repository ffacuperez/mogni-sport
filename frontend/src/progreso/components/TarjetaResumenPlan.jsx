import { Calendar } from 'lucide-react';

import { cx } from '../../shared/components/clases';
import { formatearFecha } from '../../shared/services/texto';
import iconoObjetivo from '../assets/icono-objetivo.svg';
import { tituloPlan } from '../services/progresoService';
import BarraSemanas from './BarraSemanas';
import Chip from './Chip';
import { TARJETA } from './clases';

function Dato({ icono, children }) {
  return (
    <p className="flex items-center gap-1.5 text-[13px] font-medium text-prog-soft">
      {icono}
      {children}
    </p>
  );
}

/** Resumen del plan: método y mesociclo, objetivo, fecha de inicio y avance por semanas. */
export default function TarjetaResumenPlan({ plan }) {
  return (
    <section aria-label="Resumen del plan" className={cx(TARJETA, 'flex flex-col gap-2.5 p-4')}>
      <div className="flex items-start justify-between gap-2">
        <div className="flex min-w-0 flex-col gap-0.5 font-bold">
          <p className="text-[10px] tracking-[0.8px] text-primary">MÉTODO MOGNI SPORT</p>
          <h2 className="text-[19px] text-prog-ink">{tituloPlan(plan)}</h2>
        </div>
        <Chip tono="ok">Activo</Chip>
      </div>

      {plan.objetivoMesociclo && (
        <Dato icono={<img src={iconoObjetivo} alt="" className="h-3.5 w-3.5 shrink-0" />}>Objetivo: {plan.objetivoMesociclo}</Dato>
      )}
      <Dato icono={<Calendar className="h-3.5 w-3.5 shrink-0" />}>Inicio: {formatearFecha(plan.fechaInicio)}</Dato>

      <div className="h-px bg-prog-line" />

      <div className="flex items-center justify-between gap-2">
        <p className="whitespace-nowrap text-[14px] font-bold text-prog-ink">Semana {plan.semanaActual} de {plan.cantidadSemanas}</p>
        <p className="text-right text-[12px] font-medium text-ink-muted">{plan.sesionesRegistradas} de {plan.sesionesTotales} sesiones registradas</p>
      </div>
      <BarraSemanas total={plan.cantidadSemanas} completas={plan.semanaActual} />
    </section>
  );
}
