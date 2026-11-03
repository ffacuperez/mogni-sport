import { Badge } from '../../shared/components/ui';
import RegistroSerie from './RegistroSerie';

export default function RegistroSeries({ ejercicio, onChangeSerie }) {
  return (
    <div className="overflow-hidden rounded-lg border border-line bg-row-hover">
      <div className="flex items-center justify-between gap-3 border-b border-line bg-card px-3 py-2.5">
        <span className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-soft">Series</span>
        <Badge tone="neutral" className="font-medium">{ejercicio.cantidadSeries} series</Badge>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[330px] text-left text-sm">
          <thead className="bg-row-hover text-ink-soft">
            <tr>
              <th className="px-2 py-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em]">Serie</th>
              <th className="px-2 py-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em]">Peso</th>
              <th className="px-2 py-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em]">Reps</th>
              <th className="px-2 py-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em]">RIR</th>
              <th className="px-2 py-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em]">1RM</th>
            </tr>
          </thead>
          <tbody>
            {ejercicio.series.map((serie) => (
              <RegistroSerie
                key={serie.numero}
                index={serie.numero}
                serie={serie}
                onChange={(campo, valor) => onChangeSerie(serie.numero, campo, valor)}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
