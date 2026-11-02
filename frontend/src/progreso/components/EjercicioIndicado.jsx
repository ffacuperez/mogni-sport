import { Badge } from '../../shared/components/ui';
import { textoDescansoSegundos, textoSeriesRepeticiones } from '../services/progresoService';

/** Un ejercicio de la sesión: nombre, lo que pidió el profe y sus indicaciones. RIR, descanso e indicaciones son opcionales. */
export default function EjercicioIndicado({ ejercicio }) {
  return (
    <div className="flex flex-col gap-1.5 border-t border-line px-4 py-3">
      <p className="text-sm font-semibold text-ink">{ejercicio.nombre}</p>
      <div className="flex flex-wrap gap-1.5">
        <Badge>{textoSeriesRepeticiones(ejercicio)}</Badge>
        {ejercicio.rirObjetivo != null && <Badge>RIR {ejercicio.rirObjetivo}</Badge>}
        {ejercicio.descansoMinutos != null && <Badge>{textoDescansoSegundos(ejercicio.descansoMinutos)}</Badge>}
      </div>
      {ejercicio.indicaciones && <p className="text-xs text-ink-soft">{ejercicio.indicaciones}</p>}
    </div>
  );
}
