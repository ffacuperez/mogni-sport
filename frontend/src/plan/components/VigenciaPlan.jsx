import { CalendarDays } from 'lucide-react';

import { Field, ReadOnly } from '../../shared/components/ui';
import { cx, inputClass } from '../../shared/components/clases';
import { sumarDias, textoFecha } from '../services/planesService';

/**
 * HU-266: fecha de inicio (solo lectura) y fecha fin (obligatoria, inclusiva) de un plan asignado.
 * minimo: primera fecha fin que se puede elegir. error: mensaje a mostrar debajo del campo.
 */
export default function VigenciaPlan({ inicio, fechaFin, onCambiarFechaFin, minimo, error, idPrefijo = 'plan' }) {
  const idFin = `${idPrefijo}-fecha-fin`;
  return (
    <div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Fecha de inicio">
          <ReadOnly>{textoFecha(inicio)}</ReadOnly>
        </Field>
        <Field label="Fecha fin" htmlFor={idFin}>
          <input
            id={idFin}
            type="date"
            required
            value={fechaFin}
            min={minimo}
            onChange={(ev) => onCambiarFechaFin(ev.target.value)}
            aria-invalid={error ? true : undefined}
            aria-describedby={`${idFin}-ayuda`}
            className={cx(inputClass, error && 'border-error')}
          />
        </Field>
      </div>
      <p id={`${idFin}-ayuda`} className={cx('mt-2 flex items-start gap-1.5 text-xs', error ? 'font-medium text-error' : 'text-ink-soft')}>
        <CalendarDays className="mt-px h-3.5 w-3.5 shrink-0" aria-hidden="true" />
        {error ?? (fechaFin
          ? `El plan está activo hasta el ${textoFecha(fechaFin)} inclusive. Desde el ${textoFecha(sumarDias(fechaFin, 1))} pasa a Finalizado solo.`
          : 'Elegí hasta qué día está activo el plan (inclusive).')}
      </p>
    </div>
  );
}
