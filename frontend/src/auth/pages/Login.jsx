import { useState } from 'react';
import { useNavigate } from 'react-router';

import { ArrowLeft, ChevronRight, Eye, EyeOff, Lock, Mail, ShieldCheck, Smartphone } from 'lucide-react';

import { login } from '../services/authService';
import Toast from '../../shared/components/Toast';
import { useToast } from '../../shared/hooks/useToast';
import { Button, Logo } from '../../shared/components/ui';
import { FOCUS, inputClass, labelClass } from '../../shared/components/clases';

export default function Login({ onLogin, onBack }) {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [toast, mostrarToast] = useToast();

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      mostrarToast('Por favor, complete todos los campos.', 'error');
      return;
    }
    setIsLoading(true);

    // Todos los usuarios (personal y alumnos) se autentican contra el backend
    try {
      await login(email, password);
      onLogin(); // App lee el rol y si debe cambiar la contraseña desde el token
    } catch (error) {
      mostrarToast(error.message, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-between bg-page p-4 text-ink sm:p-6 md:p-8">
      <Toast key={toast?.key} message={toast?.message} type={toast?.type} />

      <header className="flex w-full max-w-md items-center justify-end py-2 text-xs text-ink-soft">
        <span className="flex items-center gap-1.5">
          <ShieldCheck className="h-4 w-4 text-success" /> Conexión segura
        </span>
      </header>

      <main className="my-auto w-full max-w-md py-6">
        <div className="rounded-xl border border-line bg-card p-6 shadow-card sm:p-8">

          <div className="mb-6 flex justify-center">
            <Logo size="lg" />
          </div>

          <h1 className="mb-6 text-center text-xl font-semibold text-ink">Iniciar sesión</h1>

          <form onSubmit={handleLoginSubmit} className="space-y-5" noValidate>
            <div>
              <label htmlFor="login-email" className={labelClass}>Correo electrónico</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-muted" />
                <input id="login-email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Ingrese su correo..." className={`${inputClass} py-3 pl-11`} />
              </div>
            </div>

            <div>
              <label htmlFor="login-password" className={labelClass}>Contraseña</label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-muted" />
                <input id="login-password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className={`${inputClass} py-3 pl-11 pr-12`} />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  className={`absolute inset-y-0 right-0 flex cursor-pointer items-center rounded-lg pr-3.5 text-ink-muted transition-colors hover:text-ink ${FOCUS}`}
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            <Button type="submit" variant="primary" size="lg" disabled={isLoading} className="mt-2 w-full">
              {isLoading
                ? <span className="h-5 w-5 animate-spin rounded-full border-2 border-on-dark border-t-transparent" aria-label="Ingresando" />
                : <>Iniciar sesión <ChevronRight className="h-4 w-4" /></>}
            </Button>

            {/* Recuperar contraseña: link por mail (Brevo) */}
            <p className="pt-2 text-center text-sm">
              <button
                type="button"
                onClick={() => navigate('/recuperar-password')}
                className={`cursor-pointer rounded font-semibold text-primary hover:underline ${FOCUS}`}
              >
                ¿Olvidaste tu contraseña?
              </button>
            </p>
          </form>
        </div>

        {onBack && (
          <div className="mt-5 text-center">
            <Button variant="ghost" size="sm" onClick={onBack}>
              <ArrowLeft className="h-3.5 w-3.5" /> Volver al inicio
            </Button>
          </div>
        )}
      </main>

      <footer className="flex w-full max-w-md items-center justify-center gap-2 py-2 text-xs text-ink-muted">
        <Smartphone className="h-3.5 w-3.5" />
        <span>Optimizado para dispositivos móviles y PWA</span>
      </footer>
    </div>
  );
}
