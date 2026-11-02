import { ChevronLeft, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { textoDiaMes } from '../services/progresoService';

function Flecha({ label, icono: Icono, disabled, onClick }) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={cx(
        'flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full border border-prog-line bg-card text-prog-ink',
        'disabled:cursor-not-allowed disabled:text-ink-muted disabled:opacity-60',
        FOCUS,
      )}
    >
      <Icono className="h-[22px] w-[22px]" />
    </button>
  );
}

/** "‹ Semana 2 ›" con su rango de fechas. La flecha izquierda se deshabilita en la primera semana y la derecha en la última. */
export default function SelectorSemana({ semana, cantidadSemanas, onCambiar }) {
  return (
    <div className="flex items-center justify-between">
      <Flecha label="Semana anterior" icono={ChevronLeft} disabled={semana.numero <= 1} onClick={() => onCambiar(semana.numero - 1)} />
      <div className="flex flex-col items-center gap-px" aria-live="polite">
        <p className="text-[16px] font-bold text-prog-ink">Semana {semana.numero}</p>
        <p className="text-[12px] font-medium text-prog-soft">{textoDiaMes(semana.desde)} – {textoDiaMes(semana.hasta)}</p>
      </div>
      <Flecha label="Semana siguiente" icono={ChevronRight} disabled={semana.numero >= cantidadSemanas} onClick={() => onCambiar(semana.numero + 1)} />
    </div>
  );
}
