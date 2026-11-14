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
        'flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full bg-card text-ink shadow-card transition-colors hover:bg-row-hover',
        'disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:bg-card',
        FOCUS,
      )}
    >
      <Icono className="h-5 w-5" />
    </button>
  );
}

/**
 * "‹ Semana 2 ›" con su rango de fechas y la nota del entrenador, si tiene ("Semana de descarga"). La flecha izquierda
 * se deshabilita en la primera semana y la derecha en la última.
 */
export default function SelectorSemana({ semana, cantidadSemanas, onCambiar }) {
  return (
    <div className="flex items-center justify-between">
      <Flecha label="Semana anterior" icono={ChevronLeft} disabled={semana.numero <= 1} onClick={() => onCambiar(semana.numero - 1)} />
      <div className="flex flex-col items-center" aria-live="polite">
        <p className="text-base font-bold text-ink">Semana {semana.numero}</p>
        <p className="text-xs text-ink-soft">{textoDiaMes(semana.desde)} – {textoDiaMes(semana.hasta)}</p>
        {semana.nota && <p className="mt-0.5 text-center text-xs font-semibold text-info">{semana.nota}</p>}
      </div>
      <Flecha label="Semana siguiente" icono={ChevronRight} disabled={semana.numero >= cantidadSemanas} onClick={() => onCambiar(semana.numero + 1)} />
    </div>
  );
}
