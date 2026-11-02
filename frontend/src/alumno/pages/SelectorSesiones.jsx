import { Check } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';

export default function SelectorSesiones({ sesiones, selectedId, onSelect }) {
  return (
    <div className="mb-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-base font-semibold text-ink">Sesiones</h2>
      </div>
      <div className="flex gap-2 overflow-x-auto pb-1">
        {sesiones.map((sesion) => {
          const isSelected = String(sesion.id) === String(selectedId);
          return (
            <button
              key={sesion.id}
              type="button"
              onClick={() => onSelect(sesion.id)}
              className={cx(
                'shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors',
                isSelected
                  ? 'border-primary bg-error-bg text-primary shadow-card'
                  : 'border-line bg-card text-ink-soft hover:bg-row-hover',
                FOCUS,
              )}
              aria-pressed={isSelected}
            >
              <span className="inline-flex items-center gap-2">
                {isSelected && <Check className="h-4 w-4" />}
                Sesión {sesion.numero}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
