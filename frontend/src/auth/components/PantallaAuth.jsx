import { ArrowLeft } from 'lucide-react';

import { Button, Logo } from '../../shared/components/ui';

/** Marco de las pantallas sin sesión (recuperar y restablecer contraseña): card centrada como el login. */
export default function PantallaAuth({ icono: Icono, titulo, descripcion, children, onVolver, textoVolver = 'Volver a iniciar sesión' }) {
  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-page p-4 text-ink sm:p-6">
      <main className="w-full max-w-md animate-fade-in">
        <div className="rounded-xl border border-line bg-card p-6 shadow-card sm:p-8">
          <div className="mb-6 flex justify-center">
            <Logo size="lg" />
          </div>
          <div className="mb-6 flex flex-col items-center text-center">
            {Icono && (
              <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-error-bg">
                <Icono className="h-6 w-6 text-primary" aria-hidden="true" />
              </div>
            )}
            <h1 className="text-xl font-semibold text-ink">{titulo}</h1>
            {descripcion && <p className="mt-2 text-sm text-ink-soft">{descripcion}</p>}
          </div>
          {children}
        </div>
        {onVolver && (
          <div className="mt-5 text-center">
            <Button variant="ghost" size="sm" onClick={onVolver}>
              <ArrowLeft className="h-3.5 w-3.5" /> {textoVolver}
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
