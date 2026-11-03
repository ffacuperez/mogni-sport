import { MessageSquareText } from 'lucide-react';

import { Badge, Panel } from '../../shared/components/ui';
import { cx } from '../../shared/components/clases';
import RegistroSeries from './RegistroSeries';
import { esEjercicioCompleto, obtenerRMMaximo } from './entrenamientoUtils';

export default function EjercicioCard({ ejercicio, onUpdateSerie, onUpdateNota }) {
  const completo = esEjercicioCompleto(ejercicio);
  const rmMaximo = obtenerRMMaximo(ejercicio);

  return (
    <Panel className={cx('mb-5 overflow-hidden bg-card p-0', completo ? 'border-success bg-success-bg/20' : 'border-line')}>
      <div className={cx('border-b px-4 py-3', completo ? 'border-success bg-success-bg/30' : 'border-line bg-row-hover')}>
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-soft">Ejercicio</p>
            <h3 className="mt-1 text-lg font-bold text-ink">{ejercicio.nombre}</h3>
          </div>
          <Badge tone={completo ? 'success' : 'neutral'}>{completo ? 'Completo' : 'Pendiente'}</Badge>
        </div>

        <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-ink-soft">
          <span className="rounded-full bg-card px-2 py-1">{ejercicio.cantidadSeries} series</span>
          <span className="rounded-full bg-card px-2 py-1">{ejercicio.repeticionesObjetivo} reps</span>
          <span className="rounded-full bg-card px-2 py-1">RIR {ejercicio.rirObjetivo}</span>
          <span className="rounded-full bg-card px-2 py-1">Descanso {ejercicio.descanso}</span>
        </div>
      </div>

      <div className="px-4 pb-4 pt-3">
        <div className="mb-3 flex items-center justify-between rounded-lg bg-row-hover px-3 py-2 text-sm">
          <span className="text-ink-soft">1RM estimado</span>
          <span className="font-bold tabular-nums text-ink">{rmMaximo != null ? `${rmMaximo.toFixed(2)} kg` : 'Sin dato'}</span>
        </div>

        <RegistroSeries ejercicio={ejercicio} onChangeSerie={onUpdateSerie} />

        <div className="mt-4">
          <label className="mb-2 block text-xs font-medium text-ink-soft" htmlFor={`nota-${ejercicio.id ?? ejercicio.ejercicioPlanId}`}>
            Nota opcional
          </label>
          <div className="relative">
            <MessageSquareText className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-ink-muted" />
            <textarea
              id={`nota-${ejercicio.id ?? ejercicio.ejercicioPlanId}`}
              rows="2"
              value={ejercicio.nota ?? ''}
              onChange={(event) => onUpdateNota(event.target.value)}
              placeholder="Ej.: Sentí molestias en la última serie"
              className="w-full rounded-lg border border-line bg-card px-3 py-2.5 pl-9 text-sm text-ink placeholder-ink-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
        </div>
      </div>
    </Panel>
  );
}
