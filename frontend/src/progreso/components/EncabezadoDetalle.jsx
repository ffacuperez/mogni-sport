import { ChevronLeft } from 'lucide-react';

import { FOCUS } from '../../shared/components/clases';

/** Encabezado de las pantallas de detalle: flecha de volver, título y subtítulo. */
export default function EncabezadoDetalle({ titulo, subtitulo, onVolver }) {
  return (
    <div className="mb-5 flex items-center gap-3">
      <button
        type="button"
        onClick={onVolver}
        aria-label="Volver"
        className={`flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full bg-card text-ink shadow-card transition-colors hover:bg-row-hover ${FOCUS}`}
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <div className="min-w-0">
        <h1 className="text-xl font-bold text-ink">{titulo}</h1>
        <p className="text-xs text-ink-soft">{subtitulo}</p>
      </div>
    </div>
  );
}
