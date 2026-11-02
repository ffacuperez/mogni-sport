import { ChevronDown } from 'lucide-react';

import { nombreCortoPlan, tituloPlan } from '../services/progresoService';

/** Desplegable "Plan:". El activo se distingue con "(actual)"; los anteriores llevan el método porque pueden repetir mesociclo. */
export default function FiltroPlan({ planes, valor, onCambiar }) {
  return (
    <label className="relative flex items-center gap-1.5 rounded-lg border border-line bg-card pl-3 pr-9 shadow-card focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
      <span className="shrink-0 text-sm text-ink-soft">Plan:</span>
      <select
        value={valor}
        onChange={(e) => onCambiar(e.target.value)}
        className="min-h-11 min-w-0 flex-1 cursor-pointer appearance-none truncate bg-transparent text-sm font-bold text-ink focus:outline-none"
      >
        {planes.map((plan) => (
          <option key={plan.id} value={plan.id}>
            {plan.estado === 'ACTIVO' ? `${nombreCortoPlan(plan)} (actual)` : tituloPlan(plan)}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3 h-5 w-5 text-ink-muted" />
    </label>
  );
}
