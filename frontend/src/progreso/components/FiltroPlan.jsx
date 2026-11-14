import { useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import BottomSheet from '../../turno/components/BottomSheet';
import { textoRangoPlan } from '../services/progresoService';

/** El activo se distingue con "(actual)"; los anteriores, por sus fechas, porque pueden repetir nombre. */
const etiquetaPlan = (plan) => `${plan.nombre} (${plan.estado === 'ACTIVO' ? 'actual' : textoRangoPlan(plan)})`;

/** Filtro "Plan:". Las opciones se eligen en un panel que sube desde abajo, como en Turnos. */
export default function FiltroPlan({ planes, valor, onCambiar }) {
  const [abierto, setAbierto] = useState(false);
  const elegido = planes.find((plan) => plan.id === valor);

  const elegir = (id) => {
    setAbierto(false);
    onCambiar(id);
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setAbierto(true)}
        aria-haspopup="dialog"
        aria-expanded={abierto}
        className={cx('flex min-h-11 w-full cursor-pointer items-center gap-1.5 rounded-lg border border-line bg-card px-3 text-left shadow-card transition-colors hover:bg-row-hover', FOCUS)}
      >
        <span className="shrink-0 text-sm text-ink-soft">Plan:</span>
        <span className="min-w-0 flex-1 truncate text-sm font-bold text-ink">{etiquetaPlan(elegido)}</span>
        <ChevronDown className="h-5 w-5 shrink-0 text-ink-muted" />
      </button>

      {abierto && (
        <BottomSheet label="Elegí un plan" onClose={() => setAbierto(false)}>
          <h2 className="mb-4 text-xl font-bold text-ink">Elegí un plan</h2>
          <ul className="flex max-h-[60vh] flex-col gap-1 overflow-y-auto">
            {planes.map((plan) => {
              const seleccionado = plan.id === valor;
              return (
                <li key={plan.id}>
                  <button
                    type="button"
                    onClick={() => elegir(plan.id)}
                    aria-current={seleccionado ? 'true' : undefined}
                    className={cx(
                      'flex min-h-11 w-full cursor-pointer items-center justify-between gap-2 rounded-xl px-3.5 py-2.5 text-left text-sm transition-colors',
                      seleccionado ? 'bg-error-bg font-semibold text-primary' : 'font-medium text-ink hover:bg-row-hover',
                      FOCUS,
                    )}
                  >
                    {etiquetaPlan(plan)}
                    {seleccionado && <Check className="h-4 w-4 shrink-0" />}
                  </button>
                </li>
              );
            })}
          </ul>
        </BottomSheet>
      )}
    </>
  );
}
