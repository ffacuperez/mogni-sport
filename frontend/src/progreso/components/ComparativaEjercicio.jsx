import { MessageSquare } from 'lucide-react';

import { Badge, Panel } from '../../shared/components/ui';
import { textoDescansoSegundos, textoDiferenciaRir, textoKg, textoSeriesRepeticiones } from '../services/progresoService';

/**
 * Un ejercicio de la sesión con lo indicado y lo realizado lado a lado. Si el RIR real difiere del objetivo se avisa en
 * azul: es información para el alumno, no un error. HU-269: con lo hecho en cada serie y un único 1RM estimado del ejercicio (MOGNI-269).
 */
export default function ComparativaEjercicio({ ejercicio }) {
  const hayDiferencia = ejercicio.diferenciaRir != null;
  const indicado = [textoSeriesRepeticiones(ejercicio), ejercicio.rirObjetivo != null && `RIR ${ejercicio.rirObjetivo}`].filter(Boolean).join(' · ');
  const descanso = [ejercicio.descansoMinutos != null && textoDescansoSegundos(ejercicio.descansoMinutos), ejercicio.indicaciones].filter(Boolean).join(' · ');

  return (
    <Panel as="article" className="flex flex-col gap-2.5 px-4 py-3.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-base font-bold text-ink">{ejercicio.nombre}</h2>
        {hayDiferencia && <Badge tone="info" className="shrink-0">{textoDiferenciaRir(ejercicio.diferenciaRir)}</Badge>}
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div className="flex flex-col gap-1 rounded-lg bg-row-hover px-3 py-2.5">
          <p className="text-[10px] font-bold uppercase tracking-wide text-ink-muted">Indicado</p>
          <p className="text-sm font-semibold text-ink-soft">{indicado}</p>
          {descanso && <p className="text-xs text-ink-muted">{descanso}</p>}
        </div>
        <div className="flex flex-col gap-0.5 rounded-lg bg-error-bg px-3 py-2.5">
          <p className="text-[10px] font-bold uppercase tracking-wide text-primary">Realizado</p>
          <p className="text-xl font-bold text-ink">{ejercicio.kgUsados != null ? textoKg(ejercicio.kgUsados) : '—'}</p>
          {Number(ejercicio.rmEstimado) > 0 && <p className="text-sm font-semibold text-ink">1RM {textoKg(ejercicio.rmEstimado)}</p>}
          {ejercicio.rirReal != null && (
            <p className="flex items-center gap-1.5 text-sm font-semibold text-ink">
              RIR {ejercicio.rirReal}
              {hayDiferencia && <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-info" />}
            </p>
          )}
        </div>
      </div>

      {/* HU-269: el detalle por serie; los registros anteriores no lo tienen */}
      {ejercicio.seriesRealizadas.length > 0 && (
        <ol className="flex flex-col gap-1 text-sm text-ink-soft tabular-nums">
          {ejercicio.seriesRealizadas.map((serie) => (
            <li key={serie.orden}>Serie {serie.orden}: {textoKg(serie.peso)} × {serie.repeticiones} · RIR {serie.rir}</li>
          ))}
        </ol>
      )}

      {ejercicio.notas && (
        <p className="flex items-start gap-2 text-sm italic text-ink-soft">
          <MessageSquare className="mt-0.5 h-4 w-4 shrink-0" />
          “{ejercicio.notas}”
        </p>
      )}
    </Panel>
  );
}
