import { CheckCircle2, CircleDashed } from 'lucide-react';

import { Badge, Panel } from '../../shared/components/ui';
import { esEjercicioCompleto, obtenerEstadoSesion } from './entrenamientoUtils';

export default function SesionResumen({ planNombre, sesion }) {
  const totalEjercicios = sesion?.ejercicios?.length ?? 0;
  const ejerciciosCompletados = sesion?.ejercicios?.filter((ejercicio) => esEjercicioCompleto(ejercicio)).length ?? 0;
  const porcentaje = totalEjercicios ? (ejerciciosCompletados / totalEjercicios) * 100 : 0;

  return (
    <Panel className="mb-5 overflow-hidden rounded-xl p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-ink-soft">{planNombre}</p>
          <h2 className="mt-1 text-lg font-bold text-ink">{sesion ? `Sesión ${sesion.numero} · ${sesion.nombre}` : 'Sin sesión seleccionada'}</h2>
        </div>
        <Badge tone={obtenerEstadoSesion(sesion) === 'Completa' ? 'success' : 'neutral'}>{obtenerEstadoSesion(sesion)}</Badge>
      </div>

      <div className="flex items-center gap-2 text-sm text-ink-soft">
        <span className="inline-flex items-center gap-1.5">
          {obtenerEstadoSesion(sesion) === 'Completa' ? <CheckCircle2 className="h-4 w-4 text-success" /> : <CircleDashed className="h-4 w-4 text-ink-muted" />}
          {ejerciciosCompletados}/{totalEjercicios} ejercicios
        </span>
      </div>

      <div className="mt-4 h-2.5 rounded-full bg-line-soft">
        <div
          className="h-full rounded-full bg-success transition-all"
          style={{ width: `${porcentaje}%` }}
        />
      </div>
    </Panel>
  );
}
