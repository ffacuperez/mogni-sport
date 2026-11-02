import { textoDescansoSegundos, textoSeriesRepeticiones } from '../services/progresoService';

function Etiqueta({ children }) {
  return <span className="whitespace-nowrap rounded-md bg-line-soft px-2 py-[3px] text-[12px] font-semibold text-prog-tag-text">{children}</span>;
}

/** Un ejercicio de la sesión: nombre, lo que pidió el profe y sus indicaciones. RIR, descanso e indicaciones son opcionales. */
export default function EjercicioIndicado({ ejercicio }) {
  return (
    <div className="flex flex-col gap-1.5 border-t border-prog-line px-4 py-3">
      <p className="text-[15px] font-semibold text-prog-ink">{ejercicio.nombre}</p>
      <div className="flex flex-wrap gap-1.5">
        <Etiqueta>{textoSeriesRepeticiones(ejercicio)}</Etiqueta>
        {ejercicio.rirObjetivo != null && <Etiqueta>RIR {ejercicio.rirObjetivo}</Etiqueta>}
        {ejercicio.descansoMinutos != null && <Etiqueta>{textoDescansoSegundos(ejercicio.descansoMinutos)}</Etiqueta>}
      </div>
      {ejercicio.indicaciones && <p className="text-[12px] font-medium text-prog-soft">{ejercicio.indicaciones}</p>}
    </div>
  );
}
