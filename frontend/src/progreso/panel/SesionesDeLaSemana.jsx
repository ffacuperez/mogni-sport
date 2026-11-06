import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { textoDiaMes } from '../services/progresoService';
import TarjetaSesionPanel from './TarjetaSesionPanel';

// Clases fijas para que Tailwind las genere: una columna por sesión, hasta cuatro
const COLUMNAS = { 1: 'xl:grid-cols-1', 2: 'xl:grid-cols-2', 3: 'xl:grid-cols-3' };

function Flecha({ label, icono: Icono, disabled, onClick }) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={cx(
        'flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-lg border border-line bg-card text-ink transition-colors hover:bg-row-hover',
        'disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-card',
        FOCUS,
      )}
    >
      <Icono className="h-4 w-4" />
    </button>
  );
}

/**
 * "Sesiones de la semana": el selector de semana a la derecha del título y una columna por sesión. Arranca en la
 * semana que se le indica; la key del padre la reinicia si cambia el plan.
 */
export default function SesionesDeLaSemana({ plan, semanaInicial, onVerRegistro }) {
  const [numero, setNumero] = useState(semanaInicial);
  const semana = plan.semanas[numero - 1];
  const columnas = COLUMNAS[semana.sesiones.length] ?? 'xl:grid-cols-4';

  return (
    <section aria-label="Sesiones de la semana" className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-ink">Sesiones de la semana</h2>
        <div className="flex items-center gap-3">
          <Flecha label="Semana anterior" icono={ChevronLeft} disabled={numero <= 1} onClick={() => setNumero(numero - 1)} />
          <div className="flex min-w-24 flex-col items-center" aria-live="polite">
            <p className="text-sm font-semibold text-ink">Semana {semana.numero}</p>
            <p className="text-xs tabular-nums text-ink-soft">{textoDiaMes(semana.desde)} – {textoDiaMes(semana.hasta)}</p>
          </div>
          <Flecha label="Semana siguiente" icono={ChevronRight} disabled={numero >= plan.cantidadSemanas} onClick={() => setNumero(numero + 1)} />
        </div>
      </div>

      <div className={cx('grid grid-cols-1 items-stretch gap-4 md:grid-cols-2', columnas)}>
        {semana.sesiones.map((sesion) => (
          <TarjetaSesionPanel key={`${semana.numero}-${sesion.numero}`} sesion={sesion} onVerRegistro={onVerRegistro} />
        ))}
      </div>
    </section>
  );
}
