import { Check, ChevronRight, Pencil } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge } from '../../shared/components/ui';
import { textoKg, textoSeriesRepeticiones } from '../services/progresoService';

/**
 * HU-269: un ejercicio de una sesión que se puede registrar. Pendiente muestra lo indicado; hecho muestra su 1RM y se
 * puede volver a abrir para corregirlo mientras la sesión no esté finalizada.
 */
export default function EjercicioRegistrable({ ejercicio, onElegir }) {
  return (
    <button
      type="button"
      onClick={onElegir}
      className={cx('flex w-full cursor-pointer items-center gap-3 border-t border-line px-4 py-3 text-left transition-colors hover:bg-row-hover', FOCUS)}
    >
      <span className="flex min-w-0 flex-1 flex-col gap-1.5">
        <span className="text-sm font-semibold text-ink">{ejercicio.nombre}</span>
        <span className="flex flex-wrap gap-1.5">
          {ejercicio.completo ? (
            <>
              <Badge tone="success" className="gap-1"><Check className="h-3 w-3" />Completado</Badge>
              {Number(ejercicio.rmEstimado) > 0 && <Badge>1RM {textoKg(ejercicio.rmEstimado)}</Badge>}
            </>
          ) : (
            <>
              <Badge>{textoSeriesRepeticiones(ejercicio)}</Badge>
              {ejercicio.rirObjetivo != null && <Badge>RIR {ejercicio.rirObjetivo}</Badge>}
            </>
          )}
        </span>
      </span>
      {/* Lo que falta hacer se destaca; corregir algo ya hecho queda como acción secundaria */}
      {ejercicio.completo ? (
        <span className="flex shrink-0 items-center gap-1 text-xs font-medium text-ink-muted">
          <Pencil className="h-3.5 w-3.5" />
          Editar
        </span>
      ) : (
        <span className="flex shrink-0 items-center gap-0.5 rounded-full bg-primary py-1 pl-3 pr-2 text-xs font-semibold text-on-dark">
          Registrar
          <ChevronRight className="h-3.5 w-3.5" />
        </span>
      )}
    </button>
  );
}
