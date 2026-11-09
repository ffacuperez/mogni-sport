import { useState } from 'react';
import { useNavigate } from 'react-router';
import { AlertCircle, KeyRound, Mail, MailCheck } from 'lucide-react';

import { solicitarRecuperacion } from '../services/authService';
import { Button } from '../../shared/components/ui';
import { inputClass, labelClass } from '../../shared/components/clases';
import PantallaAuth from '../components/PantallaAuth';

const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Recuperar contraseña, paso 1 («¿Olvidaste tu contraseña?»): pide el email y el backend manda un link por mail.
 * El mensaje final es el mismo exista o no la cuenta (no se revela qué emails están registrados).
 */
export default function RecuperarPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState(null);
  const [enviado, setEnviado] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const limpio = email.trim();
    if (!FORMATO_EMAIL.test(limpio)) {
      setError('Ingresá un correo electrónico válido.');
      return;
    }
    setError(null);
    setEnviando(true);
    try {
      const respuesta = await solicitarRecuperacion(limpio);
      setEnviado({ email: limpio, mensaje: respuesta.mensaje });
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  if (enviado) {
    return (
      <PantallaAuth icono={MailCheck} titulo="Revisá tu correo" onVolver={() => navigate('/login')}>
        <p role="status" className="rounded-lg bg-success-bg px-4 py-3 text-sm leading-relaxed text-success">
          {enviado.mensaje}
        </p>
        <p className="mt-4 text-center text-sm text-ink-soft">
          El link vence en 30 minutos. ¿No llegó?{' '}
          <button type="button" onClick={() => setEnviado(null)} className="cursor-pointer font-semibold text-primary hover:underline">
            Pedir otro
          </button>
        </p>
      </PantallaAuth>
    );
  }

  return (
    <PantallaAuth
      icono={KeyRound}
      titulo="¿Olvidaste tu contraseña?"
      descripcion="Ingresá el correo con el que entrás a Mogni System y te mandamos un link para elegir una nueva."
      onVolver={() => navigate('/login')}
    >
      {error && (
        <div role="alert" className="mb-5 flex items-center gap-2 rounded-lg bg-error-bg px-4 py-3 text-sm text-error">
          <AlertCircle className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        <div>
          <label htmlFor="recuperar-email" className={labelClass}>Correo electrónico</label>
          <div className="relative">
            <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
            <input id="recuperar-email" type="email" autoComplete="email" value={email}
              onChange={(e) => setEmail(e.target.value)} placeholder="tu@correo.com" className={`${inputClass} py-3 pl-11`} />
          </div>
        </div>
        <Button type="submit" variant="primary" size="lg" disabled={enviando} className="w-full">
          {enviando ? 'Enviando...' : 'Enviar link'}
        </Button>
      </form>
    </PantallaAuth>
  );
}
