import { Check } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { obtenerEstadoSesion } from './entrenamientoUtils';

export default function SelectorSesiones({ sesiones, selectedId, onSelect }) {
  return (
    <div className="mb-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-ink-soft">Sesiones</h2>
      </div>
      <div className="flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {sesiones.map((sesion) => {
          const isSelected = String(sesion.id) === String(selectedId);
          const estado = obtenerEstadoSesion(sesion);
          const estadoColor = estado === 'Completa' ? 'text-success' : estado === 'Sin iniciar' ? 'text-warning-text' : 'text-ink-soft';

          return (
            <button
              key={sesion.id}
              type="button"
              onClick={() => onSelect(sesion.id)}
              className={cx(
                'shrink-0 rounded-full border px-4 py-2.5 text-sm font-semibold transition-all duration-200',
                isSelected
                  ? 'border-primary bg-primary text-on-dark shadow-card'
                  : 'border-line bg-card text-ink-soft hover:border-primary/50 hover:bg-error-bg',
                FOCUS,
              )}
              aria-pressed={isSelected}
            >
              <span className="inline-flex items-center gap-2">
                {isSelected && <Check className="h-4 w-4" />}
                <span>{`Sesión ${sesion.numero}`}</span>
                <span className={cx('text-[10px] font-medium uppercase tracking-[0.08em]', estadoColor)}>{estado}</span>
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
