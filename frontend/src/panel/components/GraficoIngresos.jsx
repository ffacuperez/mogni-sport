import { cx } from '../../shared/components/clases';
import { formatearMonto } from '../../shared/services/texto';
import { mesCorto } from '../services/panelService';

/** "$1,2 M", "$850 mil" o "$900": el monto completo va en el texto para lectores de pantalla y en el title. */
function montoCorto(monto) {
  if (monto >= 1_000_000) return `$${(monto / 1_000_000).toLocaleString('es-AR', { maximumFractionDigits: 1 })} M`;
  if (monto >= 1_000) return `$${Math.round(monto / 1_000).toLocaleString('es-AR')} mil`;
  return formatearMonto(monto);
}

/**
 * Barras con los ingresos de cada mes (sin librería: son pocas barras y así usa la paleta del proyecto). La última es
 * el mes en curso y va en el color principal. Un mes en $0 se ve como una línea en la base.
 */
export default function GraficoIngresos({ meses }) {
  const maximo = Math.max(...meses.map((m) => m.total), 0);
  return (
    <ol className="flex h-48 items-end gap-2 sm:gap-4" aria-label="Ingresos por mes">
      {meses.map(({ anio, mes, total }, i) => {
        const actual = i === meses.length - 1;
        const alto = maximo > 0 ? Math.max((total / maximo) * 100, total > 0 ? 3 : 0) : 0;
        return (
          <li key={`${anio}-${mes}`} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1">
            <span className="sr-only">{mesCorto(mes)} {anio}: {formatearMonto(total)}</span>
            <span aria-hidden="true" className={cx('truncate text-[10px] tabular-nums sm:text-xs', actual ? 'font-semibold text-ink' : 'text-ink-soft')}>
              {montoCorto(total)}
            </span>
            <div aria-hidden="true" className="flex w-full flex-1 items-end">
              <div
                title={formatearMonto(total)}
                className={cx('w-full rounded-t-md', total === 0 ? 'min-h-0.5 bg-line' : actual ? 'bg-primary' : 'bg-ink-muted/40')}
                style={{ height: `${alto}%` }}
              />
            </div>
            <span aria-hidden="true" className={cx('text-xs', actual ? 'font-semibold text-ink' : 'text-ink-soft')}>
              {mesCorto(mes)}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
