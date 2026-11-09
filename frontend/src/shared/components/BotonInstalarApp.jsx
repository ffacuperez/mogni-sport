import { useState } from 'react';
import { Download, Share, SquarePlus } from 'lucide-react';

import { Button, Modal } from './ui';
import { cx, FOCUS } from './clases';
import { useInstalarApp } from '../hooks/useInstalarApp';

/** Pasos para instalar en iPhone, donde no se puede abrir el diálogo de instalación desde un botón. */
export function InstruccionesIOS({ onClose }) {
  return (
    <Modal title="Instalar Mogni System" subtitle="En iPhone se instala desde Safari" onClose={onClose}>
      <ol className="mb-6 space-y-4 text-sm text-ink">
        <li className="flex items-center gap-3">
          <Share className="h-5 w-5 shrink-0 text-primary" />
          <span>Tocá el botón <strong>Compartir</strong> en la barra de Safari.</span>
        </li>
        <li className="flex items-center gap-3">
          <SquarePlus className="h-5 w-5 shrink-0 text-primary" />
          <span>Elegí <strong>Agregar a inicio</strong> y confirmá con <strong>Agregar</strong>.</span>
        </li>
      </ol>
      <div className="flex justify-end">
        <Button variant="primary" onClick={onClose}>Entendido</Button>
      </div>
    </Modal>
  );
}

/**
 * Botón "Descargar app" para las barras oscuras (Home y app del alumno).
 * No se muestra si la app ya está instalada o si el navegador no permite instalarla.
 */
export default function BotonInstalarApp({ className }) {
  const { modo, instalar } = useInstalarApp();
  const [verInstruccionesIOS, setVerInstruccionesIOS] = useState(false);

  if (!modo) return null;

  return (
    <>
      <button
        type="button"
        onClick={() => (modo === 'ios' ? setVerInstruccionesIOS(true) : instalar())}
        className={cx(
          'flex cursor-pointer items-center gap-2 rounded-lg border border-sidebar-hover px-3 py-2 text-sm font-medium text-on-dark transition-colors hover:bg-sidebar-hover',
          FOCUS, className,
        )}
      >
        <Download className="h-5 w-5" /> Descargar app
      </button>

      {verInstruccionesIOS && <InstruccionesIOS onClose={() => setVerInstruccionesIOS(false)} />}
    </>
  );
}
