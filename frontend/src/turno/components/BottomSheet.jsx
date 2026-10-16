import { useEffect } from 'react';
import { createPortal } from 'react-dom';

/** Panel que sube desde abajo (reservar y cancelar turnos). Cierra con Escape o tocando afuera, salvo mientras se envía. */
export default function BottomSheet({ label, onClose, cerrable = true, children }) {
  useEffect(() => {
    if (!cerrable) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose, cerrable]);

  // En el body: la pantalla tiene una animación de entrada y, adentro de ella, "fixed" no cubriría toda la ventana
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 animate-fade-in"
      onMouseDown={(e) => { if (cerrable && e.target === e.currentTarget) onClose(); }}
    >
      <div role="dialog" aria-modal="true" aria-label={label}
        className="w-full max-w-md rounded-t-3xl bg-card px-5 pb-6 pt-3 shadow-modal animate-slide-up">
        <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-line" aria-hidden="true" />
        {children}
      </div>
    </div>,
    document.body,
  );
}
