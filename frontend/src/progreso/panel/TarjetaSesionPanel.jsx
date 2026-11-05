import { Check, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Panel } from '../../shared/components/ui';
import { textoRepeticiones } from '../../plantilla/services/plantillasService';
import { partesFecha } from '../../turno/services/turnosService';
import { ESTADOS } from '../services/estadosSesion';
import { textoDescansoSegundos, textoDiaMes } from '../services/progresoService';

function Chip({ children }) {
  return <span className="rounded bg-row-hover px-2 py-0.5 text-xs font-medium tabular-nums text-ink-soft">{children}</span>;
}

/** "Lun 26/10 · Tren inferior A": la fecha si ya se empezó, y el nombre de la sesión si tiene. */
function textoSubtitulo(sesion) {
  const fecha = sesion.fechaRegistro && `${partesFecha(sesion.fechaRegistro).dia} ${textoDiaMes(sesion.fechaRegistro)}`;
  return [fecha, sesion.nombre].filter(Boolean).join(' · ');
}

/**
 * Una sesión de la semana como columna: estado y lo indicado en cada ejercicio. Si está registrada, abre lo que hizo
 * el alumno en el drawer. En curso, marca los ejercicios ya cargados.
 */
export default function TarjetaSesionPanel({ sesion, onVerRegistro }) {
  const { tono, texto, icono: Icono } = ESTADOS[sesion.estado];
  const enCurso = sesion.estado === 'EN_CURSO';
  const hechos = sesion.ejercicios.filter((e) => e.completo).length;
  const subtitulo = textoSubtitulo(sesion);

  return (
    <Panel as="article" className="flex flex-col p-4">
      <header className="flex items-start justify-between gap-2 border-b border-line pb-3">
        <div className="min-w-0">
          <h3 className="text-base font-semibold text-ink">Sesión {sesion.numero}</h3>
          {subtitulo && <p className="text-sm text-ink-soft">{subtitulo}</p>}
        </div>
        <Badge tone={tono} className="shrink-0 gap-1">
          {Icono && <Icono className="h-3 w-3" />}
          {enCurso ? `${texto} · ${hechos}/${sesion.ejercicios.length}` : texto}
        </Badge>
      </header>

      <ul className="flex flex-col gap-3 pt-3">
        {sesion.ejercicios.map((ejercicio, i) => (
          <li key={ejercicio.orden ?? i}>
            <p className="flex items-center gap-1.5 text-sm font-semibold text-ink">
              {enCurso && ejercicio.completo && <Check aria-label="Cargado" className="h-3.5 w-3.5 shrink-0 text-success" />}
              {ejercicio.nombre}
            </p>
            <div className="mt-1 flex flex-wrap gap-1.5">
              <Chip>{ejercicio.series}×{textoRepeticiones(ejercicio.repeticiones, ejercicio.repeticionesMax)}</Chip>
              {ejercicio.rirObjetivo != null && <Chip>RIR {ejercicio.rirObjetivo}</Chip>}
              {ejercicio.descansoMinutos != null && <Chip>{textoDescansoSegundos(ejercicio.descansoMinutos)}</Chip>}
            </div>
            {ejercicio.indicaciones && <p className="mt-1 text-xs text-ink-muted">{ejercicio.indicaciones}</p>}
          </li>
        ))}
      </ul>

      {sesion.estado === 'REGISTRADA' && sesion.registroId && (
        <button
          type="button"
          onClick={() => onVerRegistro(sesion.registroId)}
          className={cx('mt-auto flex cursor-pointer items-center justify-end gap-0.5 pt-4 text-sm font-semibold text-primary hover:underline', FOCUS)}
        >
          Ver registro <ChevronRight className="h-4 w-4" />
        </button>
      )}
    </Panel>
  );
}
