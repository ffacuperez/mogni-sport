import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { AlertCircle, CheckCircle2, Eye, EyeOff, KeyRound, LinkIcon } from 'lucide-react';

import { LARGO_MINIMO_PASSWORD, restablecerPassword, validarLinkRecuperacion } from '../services/authService';
import { Button, Cargando } from '../../shared/components/ui';
import CampoPassword from '../components/CampoPassword';
import PantallaAuth from '../components/PantallaAuth';

/**
 * Recuperar contraseña, paso 2: se abre desde el link del mail (/restablecer-password?token=...). Primero
 * comprueba que el link siga sirviendo; después pide la contraseña nueva y su confirmación.
 */
export default function RestablecerPassword() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  // validando | valido | invalido | listo
  const [estado, setEstado] = useState(token ? 'validando' : 'invalido');
  const [motivo, setMotivo] = useState(token ? null : 'El link no es válido. Pedí uno nuevo.');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mostrar, setMostrar] = useState(false);
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!token) return undefined;
    let vigente = true;
    validarLinkRecuperacion(token)
      .then(() => { if (vigente) setEstado('valido'); })
      .catch((err) => {
        if (!vigente) return;
        setMotivo(err.message);
        setEstado('invalido');
      });
    return () => { vigente = false; };
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nueva || !confirmacion) {
      setError('Completá los dos campos.');
      return;
    }
    if (nueva.length < LARGO_MINIMO_PASSWORD) {
      setError(`La contraseña nueva debe tener al menos ${LARGO_MINIMO_PASSWORD} caracteres.`);
      return;
    }
    if (nueva !== confirmacion) {
      setError('La confirmación no coincide con la contraseña nueva.');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      await restablecerPassword(token, nueva, confirmacion);
      setEstado('listo');
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  if (estado === 'validando') {
    return <PantallaAuth titulo="Restablecer contraseña"><Cargando texto="Verificando el link" /></PantallaAuth>;
  }

  if (estado === 'invalido') {
    return (
      <PantallaAuth icono={LinkIcon} titulo="El link ya no sirve" onVolver={() => navigate('/login')}>
        <p role="alert" className="rounded-lg bg-error-bg px-4 py-3 text-sm text-error">{motivo}</p>
        <Button variant="primary" size="lg" className="mt-5 w-full" onClick={() => navigate('/recuperar-password')}>
          Pedir un link nuevo
        </Button>
      </PantallaAuth>
    );
  }

  if (estado === 'listo') {
    return (
      <PantallaAuth icono={CheckCircle2} titulo="¡Listo!">
        <p role="status" className="rounded-lg bg-success-bg px-4 py-3 text-sm text-success">
          Tu contraseña se restableció. Ya podés iniciar sesión con la nueva.
        </p>
        <Button variant="primary" size="lg" className="mt-5 w-full" onClick={() => navigate('/login', { replace: true })}>
          Iniciar sesión
        </Button>
      </PantallaAuth>
    );
  }

  return (
    <PantallaAuth
      icono={KeyRound}
      titulo="Elegí una contraseña nueva"
      descripcion={`Tiene que tener al menos ${LARGO_MINIMO_PASSWORD} caracteres.`}
      onVolver={() => navigate('/login')}
    >
      {error && (
        <div role="alert" className="mb-5 flex items-center gap-2 rounded-lg bg-error-bg px-4 py-3 text-sm text-error">
          <AlertCircle className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        <CampoPassword id="restablecer-nueva" label="Contraseña nueva" value={nueva} onChange={setNueva}
          visible={mostrar} autoComplete="new-password" />
        <CampoPassword id="restablecer-confirmacion" label="Confirmar contraseña nueva" value={confirmacion}
          onChange={setConfirmacion} visible={mostrar} autoComplete="new-password" />
        <Button variant="ghost" size="sm" onClick={() => setMostrar(!mostrar)} className="-ml-3">
          {mostrar ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          {mostrar ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
        </Button>
        <Button type="submit" variant="primary" size="lg" disabled={guardando} className="w-full">
          {guardando ? 'Guardando...' : 'Guardar contraseña'}
        </Button>
      </form>
    </PantallaAuth>
  );
}
