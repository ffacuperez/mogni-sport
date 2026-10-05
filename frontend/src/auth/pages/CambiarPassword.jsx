import { useState } from 'react';
import { AlertCircle, Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react';

import { cambiarPassword } from '../services/authService';

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

  const inputClass = "w-full pl-11 pr-4 py-3 bg-[#1d2630] border border-gray-700/80 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-[#B32334] focus:ring-1 focus:ring-[#B32334] transition-all text-sm shadow-inner";
  const tipo = mostrar ? 'text' : 'password';

  return (
    <div className="min-h-screen w-full bg-[#131A22] text-white flex items-center justify-center p-4 sm:p-6 font-sans">
      <main className="w-full max-w-md">
        <div className="bg-[#151c24]/90 border border-red-900/30 rounded-3xl p-6 sm:p-8 shadow-2xl shadow-red-950/20">
          <div className="flex flex-col items-center text-center mb-6">
            <div className="w-12 h-12 rounded-xl bg-red-600/15 border border-red-500/30 flex items-center justify-center mb-3">
              <ShieldCheck className="w-6 h-6 text-[#e24a5e]" />
            </div>
            <h1 className="text-xl font-bold">Cambiá tu contraseña</h1>
            <p className="text-sm text-gray-400 mt-2">
              Es tu primer inicio de sesión. Antes de continuar, elegí una contraseña nueva de al menos {LARGO_MINIMO} caracteres.
            </p>
          </div>

          {error && (
            <div role="alert" className="mb-5 flex items-center gap-2 rounded-xl border border-red-500/50 bg-red-950/40 px-4 py-3 text-sm text-red-300">
              <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {[
              { id: 'pw-actual', label: 'Contraseña actual', value: actual, set: setActual, auto: 'current-password' },
              { id: 'pw-nueva', label: 'Contraseña nueva', value: nueva, set: setNueva, auto: 'new-password' },
              { id: 'pw-confirmacion', label: 'Confirmar contraseña nueva', value: confirmacion, set: setConfirmacion, auto: 'new-password' },
            ].map(({ id, label, value, set, auto }) => (
              <div key={id}>
                <label htmlFor={id} className="block text-sm font-semibold mb-2 tracking-wide">{label}</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none">
                    <Lock className="w-5 h-5 text-gray-500" />
                  </div>
                  <input id={id} type={tipo} value={value} onChange={(e) => set(e.target.value)} autoComplete={auto} className={inputClass} required />
                </div>
              </div>
            ))}

            <button type="button" onClick={() => setMostrar(!mostrar)} className="flex items-center gap-2 text-xs text-gray-400 hover:text-white transition-colors cursor-pointer">
              {mostrar ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              {mostrar ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
            </button>

            <button type="submit" disabled={isLoading} className="w-full py-3.5 px-4 bg-[#B32334] hover:bg-[#9d1f2d] text-white font-bold rounded-xl transition-all shadow-lg shadow-[#B32334]/30 tracking-wide uppercase text-sm disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer">
              {isLoading ? 'Guardando...' : 'Cambiar contraseña'}
            </button>
          </form>
        </div>

        <div className="text-center mt-5">
          <button type="button" onClick={onLogout} className="text-xs text-gray-400 hover:text-white transition-colors cursor-pointer">
            Cerrar sesión
          </button>
        </div>
      </main>
    </div>
  );
}
