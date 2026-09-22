import { useState } from 'react';

import {
  Eye,
  EyeOff,
  Mail,
  Lock,
  Dumbbell,
  CheckCircle2,
  AlertCircle,
  ChevronRight,
  ShieldCheck,
  Smartphone
} from 'lucide-react';

import { login } from '../services/authService';

export default function Login({ onLogin, onBack }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetSent, setResetSent] = useState(false);

  const triggerToast = (type, message) => {
    setToast({ type, message });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      triggerToast('error', 'Por favor, complete todos los campos.');
      return;
    }
    setIsLoading(true);

    // SI EL CORREO ES EL DEL ALUMNO, ENTRA AUTOMÁTICAMENTE
    if (email.trim().toLowerCase() === 'alumno@mognisport.com') {
      setTimeout(() => {
        setIsLoading(false);
        onLogin('alumno'); // Entra directo al dashboard del alumno
      }, 500);
      return;
    }

    // PARA LOS DEMÁS (PROFESORES), SÍ USA EL LOGIN DEL BACKEND
    try {
      await login(email, password);
      onLogin('profesor');
    } catch (error) {
      triggerToast('error', error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = () => {
    triggerToast('success', 'La pantalla de registro está lista para ser implementada.');
  };

  const handleResetPassword = (e) => {
    e.preventDefault();
    if (!resetEmail) {
      triggerToast('error', 'Ingrese su correo para restablecer la contraseña.');
      return;
    }

    setResetSent(true);
    setTimeout(() => {
      setResetSent(false);
      setIsForgotPasswordOpen(false);
      setResetEmail('');
      triggerToast('success', 'Se han enviado las instrucciones a su correo electrónico.');
    }, 1500);
  };

  return (
    <div className="min-h-screen w-full bg-[#131A22] text-white flex flex-col justify-between items-center p-4 sm:p-6 md:p-8 relative overflow-hidden font-sans">
      <div className="absolute top-[-10%] left-[-10%] w-[350px] sm:w-[500px] h-[350px] sm:h-[500px] bg-red-600/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[350px] sm:w-[500px] h-[350px] sm:h-[500px] bg-red-900/10 rounded-full blur-[120px] pointer-events-none" />

      {toast && (
        <div className={`fixed top-5 left-1/2 transform -translate-x-1/2 z-50 flex items-center gap-3 px-5 py-3.5 rounded-xl shadow-2xl border transition-all duration-300 animate-bounce ${toast.type === 'success' ? 'bg-[#151c24] border-emerald-500/50 text-emerald-400' : 'bg-[#151c24] border-red-500/50 text-red-400'}`}>
          {toast.type === 'success' ? <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" /> : <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />}
          <span className="text-sm font-medium text-gray-200">{toast.message}</span>
        </div>
      )}

      <header className="w-full max-w-md flex justify-between items-center py-2 text-xs text-gray-400 tracking-wider uppercase">
        <div className="flex items-center gap-1.5 font-semibold text-gray-300">
          <Dumbbell className="w-4 h-4 text-[#B32334]" />
          <span>MOGNI SPORT APP</span>
        </div>
        <div className="flex items-center gap-1.5 text-gray-400">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Acceso Seguro</span>
        </div>
      </header>

      <main className="w-full max-w-md my-auto py-6">
        <div className="bg-[#151c24]/90 backdrop-blur-md border border-red-900/30 rounded-3xl p-6 sm:p-8 shadow-2xl shadow-red-950/20 relative">
          
          <div className="flex flex-col items-center mb-4">
            <div className="relative group cursor-pointer mb-2">
              <div className="absolute inset-0 bg-red-600/20 rounded-full blur-md group-hover:bg-red-600/35 transition-all duration-300" />
              <img src="/logo.svg" alt="MOGNI SPORT Logo" className="w-36 h-36 sm:w-44 sm:h-44 object-contain relative transform transition duration-300 group-hover:scale-105 drop-shadow-[0_4px_12px_rgba(229,35,53,0.35)]" />
            </div>
          </div>

          <div className="relative my-5 flex items-center justify-center">
            <div className="border-t border-gray-800 w-full"></div>
            <span className="bg-[#151c24] px-4 text-xs font-bold text-[#B32334] tracking-widest uppercase whitespace-nowrap">INICIAR SESIÓN</span>
            <div className="border-t border-gray-800 w-full"></div>
          </div>

          <form onSubmit={handleLoginSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-semibold text-white mb-2 tracking-wide">Correo Electrónico:</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                  <Mail className="w-5 h-5 text-gray-500" />
                </div>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Ingrese su correo..." className="w-full pl-11 pr-4 py-3 bg-[#1d2630] border border-gray-700/80 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-[#B32334] focus:ring-1 focus:ring-[#B32334] transition-all text-sm shadow-inner" required />
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-white mb-2 tracking-wide">Contraseña:</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                  <Lock className="w-5 h-5 text-gray-500" />
                </div>
                <input type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className="w-full pl-11 pr-12 py-3 bg-[#1d2630] border border-gray-700/80 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-[#B32334] focus:ring-1 focus:ring-[#B32334] transition-all text-sm shadow-inner" required />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-gray-400 hover:text-white transition-colors focus:outline-none">
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={isLoading} className="w-full py-3.5 px-4 bg-[#B32334] hover:bg-[#9d1f2d] active:bg-[#8c1d2a] text-white font-bold rounded-xl transition-all duration-200 shadow-lg shadow-[#B32334]/30 hover:shadow-[#B32334]/50 flex items-center justify-center gap-2 tracking-wide uppercase text-sm mt-2 disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer">
              {isLoading ? <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" /> : <><span>INICIAR SESIÓN</span><ChevronRight className="w-4 h-4" /></>}
            </button>

            <button type="button" onClick={handleRegister} disabled={isLoading} className="w-full py-3 px-4 bg-[#1d2630] hover:bg-[#25303d] border border-gray-700 rounded-xl text-gray-200 font-medium transition-all duration-200 flex items-center justify-center gap-3 text-sm shadow-sm cursor-pointer">
              <svg className="w-5 h-5 text-[#B32334]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="10" cy="7" r="4" />
                <path d="M20 8v6" />
                <path d="M17 11h6" />
              </svg>
              <span>Registrarse</span>
            </button>

            <div className="text-center pt-2">
              <button type="button" onClick={() => setIsForgotPasswordOpen(true)} className="text-xs text-[#B32334] hover:text-[#c9374d] underline underline-offset-4 transition-colors font-medium tracking-wide focus:outline-none cursor-pointer">
                ¿Olvidó su contraseña?
              </button>
            </div>
          </form>
        </div>

        {onBack && (
          <div className="text-center mt-5">
            <button type="button" onClick={onBack} className="text-xs text-gray-400 hover:text-white transition-colors cursor-pointer">
              ← Volver al inicio
            </button>
          </div>
        )}
      </main>

      {isForgotPasswordOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#151c24] border border-red-900/40 rounded-2xl p-6 w-full max-w-sm shadow-2xl relative">
            <h3 className="text-lg font-bold text-white mb-2">Recuperar Contraseña</h3>
            <p className="text-xs text-gray-400 mb-4">Ingrese su correo electrónico registrado para recibir un enlace de restablecimiento.</p>
            <form onSubmit={handleResetPassword} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-white mb-1.5">Correo Electrónico:</label>
                <input type="email" value={resetEmail} onChange={(e) => setResetEmail(e.target.value)} placeholder="ejemplo@gym.com" className="w-full px-3.5 py-2.5 bg-[#1d2630] border border-gray-700 rounded-lg text-white text-sm focus:outline-none focus:border-[#B32334]" required />
              </div>
              <div className="flex gap-2 justify-end pt-2">
                <button type="button" onClick={() => setIsForgotPasswordOpen(false)} className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-xs text-gray-300 font-semibold rounded-lg transition-colors cursor-pointer">Cancelar</button>
                <button type="submit" disabled={resetSent} className="px-4 py-2 bg-[#B32334] hover:bg-[#9d1f2d] text-xs text-white font-bold rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer">
                  {resetSent ? 'Enviando...' : 'Enviar Instrucciones'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <footer className="w-full max-w-md text-center py-2 text-xs text-gray-500 flex items-center justify-center gap-2">
        <Smartphone className="w-3.5 h-3.5 text-gray-500" />
        <span>Optimizado para dispositivos móviles y PWA</span>
      </footer>
    </div>
  );
}
