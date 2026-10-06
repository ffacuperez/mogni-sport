import { AlertCircle, CheckCircle2, Info } from 'lucide-react';

const ESTILOS = {
  success: { borde: 'border-l-success', icono: <CheckCircle2 className="h-5 w-5 shrink-0 text-success" /> },
  error: { borde: 'border-l-error', icono: <AlertCircle className="h-5 w-5 shrink-0 text-error" /> },
  info: { borde: 'border-l-info', icono: <Info className="h-5 w-5 shrink-0 text-info" /> },
};

/** Aviso flotante, el mismo en todas las pantallas. type: success | error | info */
export default function Toast({ message, type = 'success' }) {
  if (!message) return null;
  const { borde, icono } = ESTILOS[type] ?? ESTILOS.info;
  return (
    <div
      role={type === 'error' ? 'alert' : 'status'}
      aria-live="polite"
      className={`fixed left-1/2 top-5 z-[60] flex w-[calc(100%-2rem)] max-w-md items-center gap-3 rounded-lg border border-l-4 border-line bg-card px-5 py-3.5 shadow-modal animate-slide-down ${borde}`}
    >
      {icono}
      <span className="text-sm font-medium text-ink">{message}</span>
    </div>
  );
}
