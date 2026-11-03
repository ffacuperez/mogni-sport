import { CalendarDays } from 'lucide-react';

import { Field, ReadOnly } from '../../shared/components/ui';
import { cx, inputClass } from '../../shared/components/clases';
import { fechaFinPorSemanas, MAXIMO_SEMANAS, sumarDias, textoFecha, validarSemanas } from '../services/planesService';

/**
 * HU-266: fecha de inicio (solo lectura) y duración en semanas de un plan asignado. La fecha fin no se elige: es el
 * último día de la última semana y se muestra debajo.
 * minimo: menor cantidad de semanas que se puede elegir. error: mensaje a mostrar debajo del campo.
 */
export default function VigenciaPlan({ inicio, semanas, onCambiarSemanas, minimo = 1, error, idPrefijo = 'plan' }) {
  const idSemanas = `${idPrefijo}-semanas`;
  // Solo se calcula la fecha con un valor que el backend aceptaría
  const fechaFin = validarSemanas(semanas, { minimo }) ? null : fechaFinPorSemanas(inicio, Number(semanas));
  return (
    <div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Fecha de inicio">
          <ReadOnly>{textoFecha(inicio)}</ReadOnly>
        </Field>
        <Field label="Duración" htmlFor={idSemanas}>
          <div className="flex items-center gap-2">
            <input
              id={idSemanas}
              type="number"
              inputMode="numeric"
              required
              value={semanas}
              min={minimo}
              max={MAXIMO_SEMANAS}
              step={1}
              onChange={(ev) => onCambiarSemanas(ev.target.value)}
              aria-invalid={error ? true : undefined}
              aria-describedby={`${idSemanas}-ayuda`}
              className={cx(inputClass, 'w-24', error && 'border-error')}
            />
            <span className="text-sm text-ink-soft">{Number(semanas) === 1 ? 'semana' : 'semanas'}</span>
          </div>
        </Field>
      </div>
      <p id={`${idSemanas}-ayuda`} className={cx('mt-2 flex items-start gap-1.5 text-xs', error ? 'font-medium text-error' : 'text-ink-soft')}>
        <CalendarDays className="mt-px h-3.5 w-3.5 shrink-0" aria-hidden="true" />
        {error ?? (fechaFin
          ? `Termina el ${textoFecha(fechaFin)} inclusive. Desde el ${textoFecha(sumarDias(fechaFin, 1))} pasa a Finalizado solo.`
          : 'Elegí cuántas semanas dura el plan.')}
      </p>
    </div>
  );
}
