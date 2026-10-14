import { Construction } from 'lucide-react';

/** Secciones de la app del alumno que todavía no están desarrolladas (Inicio, Turnos, Rutinas). */
export default function SeccionProximamente({ titulo }) {
  return (
    <div className="animate-fade-in">
      <h1 className="mb-6 text-2xl font-bold text-ink">{titulo}</h1>
      <div className="mt-12 flex flex-col items-center gap-3 text-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
          <Construction className="h-6 w-6" />
        </span>
        <p className="text-sm font-semibold text-ink">Próximamente</p>
        <p className="max-w-60 text-xs text-ink-soft">Esta sección todavía está en desarrollo.</p>
      </div>
    </div>
  );
}
