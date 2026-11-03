import { CheckCircle2, CircleDashed, TimerReset } from 'lucide-react';

import { Badge, Panel } from '../../shared/components/ui';
import { esEjercicioCompleto, obtenerEstadoSesion } from './entrenamientoUtils';

export default function SesionResumen({ planNombre, sesion }) {
  const totalEjercicios = sesion?.ejercicios?.length ?? 0;
  const ejerciciosCompletados = sesion?.ejercicios?.filter((ejercicio) => esEjercicioCompleto(ejercicio)).length ?? 0;
  const porcentaje = totalEjercicios ? (ejerciciosCompletados / totalEjercicios) * 100 : 0;
  const estado = obtenerEstadoSesion(sesion);
  const estadoTone = estado === 'Completa' ? 'success' : estado === 'Sin iniciar' ? 'warning' : 'neutral';
  const icono = estado === 'Completa' ? <CheckCircle2 className="h-4 w-4 text-success" /> : estado === 'Sin iniciar' ? <TimerReset className="h-4 w-4 text-warning-text" /> : <CircleDashed className="h-4 w-4 text-ink-muted" />;

  return (
    <Panel className="mb-5 overflow-hidden rounded-xl border-primary/10 bg-card p-4 shadow-card">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-ink-soft">{planNombre || 'Plan de entrenamiento'}</p>
          <h2 className="mt-1 line-clamp-2 text-lg font-bold text-ink">{sesion ? `Sesión ${sesion.numero} · ${sesion.nombre}` : 'Sin sesión seleccionada'}</h2>
        </div>
        <Badge tone={estadoTone}>{estado}</Badge>
      </div>

      <div className="mb-3 flex items-center justify-between gap-2 text-sm">
        <span className="inline-flex items-center gap-1.5 font-medium text-ink-soft">
          {icono}
          {estado === 'Sin iniciar' ? 'Aún no comenzaste' : `${ejerciciosCompletados}/${totalEjercicios} ejercicios`}
        </span>
        <span className="text-xs font-semibold uppercase tracking-[0.08em] text-primary">{estado === 'Sin iniciar' ? '0%' : `${Math.round(porcentaje)}%`}</span>
      </div>

      <div className="h-2.5 overflow-hidden rounded-full bg-line-soft">
        <div
          className={estado === 'Sin iniciar' ? 'h-full w-0 rounded-full bg-gradient-to-r from-warning-bg to-warning-text' : 'h-full rounded-full bg-gradient-to-r from-primary to-primary-light transition-all duration-300'}
          style={{ width: estado === 'Sin iniciar' ? '0%' : `${porcentaje}%` }}
        />
      </div>
    </Panel>
  );
}
