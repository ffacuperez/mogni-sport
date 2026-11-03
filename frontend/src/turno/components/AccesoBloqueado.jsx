import { Lock } from 'lucide-react';

/** 403 en las pantallas del alumno (turnos, mi progreso): alumno inactivo. El mensaje con el motivo lo manda el backend. */
export default function AccesoBloqueado({ titulo = 'No podés acceder a los turnos', mensaje }) {
  return (
    <div className="mt-12 flex animate-fade-in flex-col items-center gap-3 text-center">
      <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
        <Lock className="h-6 w-6" />
      </span>
      <p className="text-sm font-semibold text-ink">{titulo}</p>
      <p className="max-w-64 text-xs text-ink-soft">{mensaje}</p>
    </div>
  );
}
