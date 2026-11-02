import { cx, inputClass } from '../../shared/components/clases';

export default function RegistroSerie({ index, serie, onChange }) {
  return (
    <tr className="align-top">
      <td className="px-2 py-2 text-center text-sm font-semibold text-ink-soft">{index}</td>
      <td className="px-2 py-2">
        <input
          aria-label={`Peso de la serie ${index}`}
          type="number"
          min="0"
          step="0.5"
          value={serie.peso === '' ? '' : Number(serie.peso)}
          onChange={(event) => onChange('peso', event.target.value)}
          className={cx(inputClass, 'text-center tabular-nums')}
          placeholder="0"
        />
      </td>
      <td className="px-2 py-2">
        <input
          aria-label={`Repeticiones de la serie ${index}`}
          type="number"
          min="0"
          step="1"
          value={serie.repeticiones === '' ? '' : Number(serie.repeticiones)}
          onChange={(event) => onChange('repeticiones', event.target.value)}
          className={cx(inputClass, 'text-center tabular-nums')}
          placeholder="0"
        />
      </td>
      <td className="px-2 py-2">
        <input
          aria-label={`RIR de la serie ${index}`}
          type="number"
          min="0"
          step="1"
          value={serie.rirRealizado === '' ? '' : Number(serie.rirRealizado)}
          onChange={(event) => onChange('rirRealizado', event.target.value)}
          className={cx(inputClass, 'text-center tabular-nums')}
          placeholder="0"
        />
      </td>
      <td className="px-2 py-2 text-center text-sm font-semibold tabular-nums text-ink-soft">
        {serie.rmEstimado != null ? `${serie.rmEstimado.toFixed(2)} kg` : '—'}
      </td>
    </tr>
  );
}
