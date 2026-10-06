import { useState } from 'react';
import { AlertCircle, Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react';

import { cambiarPassword } from '../services/authService';
import { Button } from '../../shared/components/ui';
import { inputClass, labelClass } from '../../shared/components/clases';

const LARGO_MINIMO = 8;

// HU-248: pantalla obligatoria en el primer inicio. No tiene forma de saltearla: solo cambiar la contraseña o cerrar sesión.
export default function CambiarPassword({ onSuccess, onLogout }) {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mostrar, setMostrar] = useState(false);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!actual || !nueva || !confirmacion) {
      setError('Complete todos los campos.');
      return;
    }
    if (nueva.length < LARGO_MINIMO) {
      setError(`La contraseña nueva debe tener al menos ${LARGO_MINIMO} caracteres.`);
      return;
    }
    if (nueva !== confirmacion) {
      setError('Las contraseñas nuevas no coinciden.');
      return;
    }
    if (nueva === actual) {
      setError('La contraseña nueva debe ser distinta de la actual.');
      return;
    }

    setIsLoading(true);
    try {
      await cambiarPassword(actual, nueva);
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const tipo = mostrar ? 'text' : 'password';

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-page p-4 text-ink sm:p-6">
      <main className="w-full max-w-md animate-fade-in">
        <div className="rounded-xl border border-line bg-card p-6 shadow-card sm:p-8">
          <div className="mb-6 flex flex-col items-center text-center">
            <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-error-bg">
              <ShieldCheck className="h-6 w-6 text-primary" />
            </div>
            <p className="mb-1 text-xs font-medium text-ink-soft">Primer ingreso</p>
            <h1 className="text-xl font-semibold text-ink">Cambiá tu contraseña</h1>
            <p className="mt-2 text-sm text-ink-soft">
              Antes de continuar, elegí una contraseña nueva de al menos {LARGO_MINIMO} caracteres.
            </p>
          </div>

          {error && (
            <div role="alert" className="mb-5 flex items-center gap-2 rounded-lg bg-error-bg px-4 py-3 text-sm text-error">
              <AlertCircle className="h-5 w-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {[
              { id: 'pw-actual', label: 'Contraseña actual', value: actual, set: setActual, auto: 'current-password' },
              { id: 'pw-nueva', label: 'Contraseña nueva', value: nueva, set: setNueva, auto: 'new-password' },
              { id: 'pw-confirmacion', label: 'Confirmar contraseña nueva', value: confirmacion, set: setConfirmacion, auto: 'new-password' },
            ].map(({ id, label, value, set, auto }) => (
              <div key={id}>
                <label htmlFor={id} className={labelClass}>{label}</label>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-muted" />
                  <input id={id} type={tipo} value={value} onChange={(e) => set(e.target.value)} autoComplete={auto} className={`${inputClass} py-3 pl-11`} />
                </div>
              </div>
            ))}

            <Button variant="ghost" size="sm" onClick={() => setMostrar(!mostrar)} className="-ml-3">
              {mostrar ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              {mostrar ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
            </Button>

            <Button type="submit" variant="primary" size="lg" disabled={isLoading} className="w-full">
              {isLoading ? 'Guardando...' : 'Cambiar contraseña'}
            </Button>
          </form>
        </div>

        <div className="mt-5 text-center">
          <Button variant="ghost" size="sm" onClick={onLogout}>Cerrar sesión</Button>
        </div>
      </main>
    </div>
  );
}
