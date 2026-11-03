import { useState } from 'react';
import { AlertCircle, CheckCircle2, Eye, EyeOff } from 'lucide-react';

import { cambiarPassword, LARGO_MINIMO_PASSWORD } from '../services/authService';
import { Button } from '../../shared/components/ui';
import CampoPassword from './CampoPassword';

/**
 * MOGNI-263: cambiar la contraseña con la sesión iniciada (cualquier perfil). Pide la actual, la nueva y la
 * confirmación. Al terminar la sesión sigue abierta (el backend devuelve un token nuevo) y solo vale la nueva.
 * onCambiada: se llama después de un cambio exitoso (opcional).
 */
export default function FormCambiarPassword({ onCambiada, onCancelar }) {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mostrar, setMostrar] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const validar = () => {
    if (!actual || !nueva || !confirmacion) return 'Completá los tres campos.';
    if (nueva.length < LARGO_MINIMO_PASSWORD) return `La contraseña nueva debe tener al menos ${LARGO_MINIMO_PASSWORD} caracteres.`;
    if (nueva === actual) return 'La contraseña nueva debe ser distinta de la actual.';
    if (nueva !== confirmacion) return 'La confirmación no coincide con la contraseña nueva.';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setExito(null);
    const problema = validar();
    if (problema) {
      setError(problema);
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      await cambiarPassword(actual, nueva, confirmacion);
      setActual('');
      setNueva('');
      setConfirmacion('');
      setMostrar(false);
      setExito('Tu contraseña se cambió correctamente. Desde ahora iniciá sesión con la nueva.');
      onCambiada?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {error && (
        <div role="alert" className="flex items-center gap-2 rounded-lg bg-error-bg px-4 py-3 text-sm text-error">
          <AlertCircle className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}
      {exito && (
        <div role="status" className="flex items-center gap-2 rounded-lg bg-success-bg px-4 py-3 text-sm text-success">
          <CheckCircle2 className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span>{exito}</span>
        </div>
      )}

      <CampoPassword id="perfil-pw-actual" label="Contraseña actual" value={actual} onChange={setActual}
        visible={mostrar} autoComplete="current-password" />
      <CampoPassword id="perfil-pw-nueva" label="Contraseña nueva" value={nueva} onChange={setNueva}
        visible={mostrar} autoComplete="new-password" ayuda={`Al menos ${LARGO_MINIMO_PASSWORD} caracteres y distinta de la actual.`} />
      <CampoPassword id="perfil-pw-confirmacion" label="Confirmar contraseña nueva" value={confirmacion} onChange={setConfirmacion}
        visible={mostrar} autoComplete="new-password" />

      <Button variant="ghost" size="sm" onClick={() => setMostrar(!mostrar)} className="-ml-3">
        {mostrar ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        {mostrar ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
      </Button>

      <div className="flex flex-col-reverse gap-3 border-t border-line pt-4 sm:flex-row sm:justify-end">
        {onCancelar && <Button variant="ghost" onClick={onCancelar} disabled={guardando}>Cancelar</Button>}
        <Button type="submit" variant="primary" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Cambiar contraseña'}
        </Button>
      </div>
    </form>
  );
}
