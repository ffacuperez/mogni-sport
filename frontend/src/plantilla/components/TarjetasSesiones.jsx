import { useState } from 'react';

import { cx, FOCUS } from '../../shared/components/clases';
import { textoDescanso, textoRepeticiones } from '../services/plantillasService';
import { diasDeLaSemana, textoFecha } from '../../plan/services/planesService';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;
const formatoPeso = (peso) => `${Number(peso).toLocaleString('es-AR', { maximumFractionDigits: 2 })} kg`;

/** Tarjetas de las sesiones de una semana con sus ejercicios. */
export default function TarjetasSesiones({ sesiones }) {
  return (
    <ul className="grid grid-cols-1 items-start gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {sesiones.map(sesion => (
        <li key={sesion.id} className="rounded-lg bg-row-hover p-3">
          <div className="flex items-center gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-card text-sm font-bold tabular-nums text-ink shadow-card">
              {sesion.numero}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">
                Sesión {sesion.numero}{sesion.nombre && ` · ${sesion.nombre}`}
              </p>
              <p className="text-xs text-ink-soft">{plural(sesion.ejercicios.length, 'ejercicio', 'ejercicios')}</p>
            </div>
          </div>
          <ol className="mt-3 divide-y divide-line-soft rounded-md bg-card px-3 shadow-card">
            {sesion.ejercicios.map(ej => (
              <li key={ej.id} className="py-2.5">
                {/* Nombre a todo el ancho y los datos abajo: en columnas angostas, compartir fila lo aplastaba */}
                <p className="text-sm font-medium leading-snug text-ink">
                  {ej.nombre}
                  {!ej.ejercicioActivo && <span className="ml-1 text-xs font-normal text-ink-muted">(dado de baja)</span>}
                </p>
                <p className="mt-0.5 text-xs tabular-nums text-ink-soft">
                  <span className="font-semibold text-ink">{ej.series} × {textoRepeticiones(ej.repeticiones, ej.repeticionesMax)}</span>
                  {[
                    ej.pesoSugerido != null && formatoPeso(ej.pesoSugerido),
                    ej.rirObjetivo != null && `RIR ${ej.rirObjetivo}`,
                    ej.descansoMinutos != null && `${textoDescanso(ej.descansoMinutos)} desc.`,
                  ].filter(Boolean).map(dato => ` · ${dato}`).join('')}
                </p>
                {ej.indicaciones && <p className="mt-0.5 text-xs italic text-ink-soft">{ej.indicaciones}</p>}
              </li>
            ))}
          </ol>
        </li>
      ))}
    </ul>
  );
}

/**
 * Las semanas de una plantilla o de un plan, de solo lectura: se elige la semana y se ven sus sesiones.
 * inicio (opcional): fecha de la asignación, para mostrar las fechas de cada semana. semanaEnCurso: se marca.
 */
export function VistaSemanas({ semanas, inicio, semanaEnCurso }) {
  const [numero, setNumero] = useState(semanaEnCurso ?? 1);
  const semana = semanas.find(s => s.numero === numero) ?? semanas[0];
  if (!semana) return null;
  const fechas = inicio ? diasDeLaSemana(inicio, semana.numero) : null;

  return (
    <div>
      {semanas.length > 1 && (
        <div role="group" aria-label="Elegí la semana" className="mb-3 flex flex-wrap gap-1.5">
          {semanas.map(s => (
            <button key={s.numero} type="button" onClick={() => setNumero(s.numero)} aria-pressed={s.numero === semana.numero}
              title={s.nota || undefined}
              className={cx('cursor-pointer rounded-full border px-3 py-1 text-xs font-semibold tabular-nums transition-colors', FOCUS,
                s.numero === semana.numero ? 'border-primary bg-error-bg text-primary-hover' : 'border-line bg-card text-ink-soft hover:text-ink')}>
              Semana {s.numero}
              {s.numero === semanaEnCurso && <span className="ml-1 font-normal">· en curso</span>}
              {s.nota && <span className="ml-1.5 inline-block h-1.5 w-1.5 rounded-full bg-info align-middle" aria-label="Tiene nota" />}
            </button>
          ))}
        </div>
      )}
      <p className="mb-3 text-xs text-ink-soft">
        <span className="font-semibold text-ink">Semana {semana.numero}{semana.nota && ` · ${semana.nota}`}</span>
        {fechas && ` · del ${textoFecha(fechas.desde)} al ${textoFecha(fechas.hasta)}`}
        {' · '}{plural(semana.sesiones.length, 'sesión', 'sesiones')}
      </p>
      <TarjetasSesiones sesiones={semana.sesiones} />
    </div>
  );
}
