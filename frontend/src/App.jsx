import React, { useState } from 'react';
import { Mail, Lock, Eye, EyeOff, LogIn, CheckCircle2, AlertCircle, X } from 'lucide-react';

export default function App() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isForgotModalOpen, setIsForgotModalOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [toast, setToast] = useState(null);

  const showNotification = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const handleLogin = (e) => {
    e.preventDefault();
    if (!email || !password) {
      showNotification('Por favor complete todos los campos', 'error');
      return;
    }
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      showNotification('¡Inicio de sesión exitoso! Bienvenido a MOGNI SPORT.');
    }, 1500);
  };

  const handleGoogleLogin = () => {
    showNotification('Conectando con el servicio de autenticación de Google...');
  };

  const handleForgotSubmit = (e) => {
    e.preventDefault();
    if (!forgotEmail) {
      showNotification('Ingrese su correo para recuperar la contraseña', 'error');
      return;
    }
    setIsForgotModalOpen(false);
    showNotification(`Se han enviado las instrucciones de recuperación a ${forgotEmail}`);
    setForgotEmail('');
  };

  return (
    <div className="min-h-screen bg-[#111822] text-white flex flex-col justify-center items-center p-4 relative overflow-hidden font-sans">
      {/* Glow / Resplandor de fondo en tono rojo */}
      <div className="absolute w-96 h-96 bg-[#e52335]/15 rounded-full blur-3xl pointer-events-none top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2" />

      {/* Toast Notification */}
      {toast && (
        <div className={`fixed top-5 right-5 z-50 flex items-center gap-3 px-4 py-3 rounded-lg shadow-xl border backdrop-blur-md transition-all duration-300 animate-bounce ${
          toast.type === 'error' 
            ? 'bg-red-950/80 border-red-500 text-red-200' 
            : 'bg-emerald-950/80 border-emerald-500 text-emerald-200'
        }`}>
          {toast.type === 'error' ? <AlertCircle className="w-5 h-5 text-red-400" /> : <CheckCircle2 className="w-5 h-5 text-emerald-400" />}
          <span className="text-sm font-medium">{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 hover:opacity-75">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Main Container */}
      <div className="w-full max-w-md z-10">
        
        {/* Logo SVG Interactivo Integrado */}
        <div className="flex flex-col items-center mb-6 group cursor-pointer">
          <div className="relative transform group-hover:scale-105 transition-transform duration-300">
            <svg 
              className="w-40 h-40 sm:w-48 sm:h-48 drop-shadow-[0_0_20px_rgba(229,35,53,0.35)]" 
              viewBox="0 0 500 500" 
              fill="none" 
              xmlns="http://www.w3.org/2000/svg"
            >
              {/* Círculo Borde Exterior */}
              <circle cx="250" cy="250" r="230" stroke="#e52335" strokeWidth="10" />
              
              {/* Garra Izquierda */}
              <path d="M 120 230 C 100 210, 110 180, 130 180 C 145 180, 150 200, 155 220" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />
              <path d="M 150 210 C 135 180, 150 150, 170 155 C 185 160, 185 185, 185 210" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />
              <path d="M 185 200 C 180 160, 200 135, 220 145 C 230 150, 225 180, 215 210" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />

              {/* Garra Derecha */}
              <path d="M 380 230 C 400 210, 390 180, 370 180 C 355 180, 350 200, 345 220" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />
              <path d="M 350 210 C 365 180, 350 150, 330 155 C 315 160, 315 185, 315 210" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />
              <path d="M 315 200 C 320 160, 300 135, 280 145 C 270 150, 275 180, 285 210" stroke="#e52335" strokeWidth="8" strokeLinecap="round" fill="#e52335" />

              {/* Cabeza Pantera */}
              <path d="M 200 180 C 200 130, 230 100, 250 100 C 270 100, 300 130, 300 180 C 300 210, 285 240, 250 260 C 215 240, 200 210, 200 180 Z" stroke="#e52335" strokeWidth="8" fill="#111822" />
              
              {/* Ojos y Hocico */}
              <polygon points="225,155 240,165 220,165" fill="#e52335" />
              <polygon points="275,155 260,165 280,165" fill="#e52335" />
              <path d="M 240 200 L 260 200 L 250 215 Z" fill="#e52335" />
              <path d="M 235 225 Q 250 235 265 225" stroke="#e52335" strokeWidth="5" strokeLinecap="round" />

              {/* Texto MOGNI SPORT */}
              <text x="250" y="325" textAnchor="middle" fill="#e52335" fontSize="48" fontWeight="900" fontFamily="sans-serif" letterSpacing="2">MOGNI</text>
              <line x1="160" y1="345" x2="340" y2="345" stroke="#e52335" strokeWidth="4" />
              <text x="250" y="380" textAnchor="middle" fill="#e52335" fontSize="30" fontWeight="700" fontFamily="sans-serif" letterSpacing="8">SPORT</text>
            </svg>
          </div>
        </div>

        {/* Card Formulario */}
        <div className="bg-[#18222d] border border-[#e52335]/40 rounded-2xl shadow-2xl p-6 sm:p-8 backdrop-blur-sm relative">
          
          <div className="text-center mb-6">
            <h2 className="text-xl sm:text-2xl font-bold tracking-wide uppercase text-[#e52335] flex items-center justify-center gap-2">
              Iniciar Sesión
            </h2>
            <p className="text-xs text-gray-400 mt-1">Accede a tu panel de afiliado de MOGNI SPORT</p>
          </div>

          <form onSubmit={handleLogin} className="space-y-5">
            {/* Campo Correo */}
            <div>
              <label className="block text-sm font-semibold text-white mb-1.5">
                Correo Electrónico:
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                  <Mail className="w-5 h-5" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="ejemplo@correo.com"
                  className="w-full pl-10 pr-4 py-2.5 bg-[#111822] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-[#e52335] focus:ring-1 focus:ring-[#e52335] transition-all"
                  required
                />
              </div>
            </div>

            {/* Campo Contraseña */}
            <div>
              <label className="block text-sm font-semibold text-white mb-1.5">
                Contraseña:
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-10 py-2.5 bg-[#111822] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-[#e52335] focus:ring-1 focus:ring-[#e52335] transition-all"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-white transition-colors"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {/* Botón 1: Iniciar Sesión */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 px-4 bg-[#e52335] hover:bg-[#c91c2c] text-white font-bold rounded-lg shadow-lg hover:shadow-red-900/40 transition-all duration-200 flex items-center justify-center gap-2 transform active:scale-[0.99] disabled:opacity-50"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <LogIn className="w-5 h-5" />
                  <span>INICIAR SESIÓN</span>
                </>
              )}
            </button>
          </form>

          {/* Divisor */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-700"></div>
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-[#18222d] px-3 text-gray-400">O ingresa con</span>
            </div>
          </div>

          {/* Botón 2: Iniciar Sesión con Google */}
          <button
            type="button"
            onClick={handleGoogleLogin}
            className="w-full py-2.5 px-4 bg-[#111822] hover:bg-[#1a2432] border border-gray-700 hover:border-gray-500 text-white font-medium rounded-lg transition-all duration-200 flex items-center justify-center gap-3 transform active:scale-[0.99]"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
              />
            </svg>
            <span>Iniciar sesión con Google</span>
          </button>

          {/* Botón 3: Olvidó su contraseña */}
          <div className="mt-6 text-center">
            <button
              type="button"
              onClick={() => setIsForgotModalOpen(true)}
              className="text-sm text-[#e52335] hover:text-[#f35261] hover:underline font-medium transition-colors"
            >
              ¿Olvidó su contraseña?
            </button>
          </div>
        </div>
      </div>

      {/* Modal de Recuperación */}
      {isForgotModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#18222d] border border-gray-700 rounded-xl p-6 w-full max-w-md shadow-2xl relative animate-in fade-in zoom-in duration-200">
            <button
              onClick={() => setIsForgotModalOpen(false)}
              className="absolute top-4 right-4 text-gray-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-white mb-2">Recuperar Contraseña</h3>
            <p className="text-sm text-gray-300 mb-4">
              Ingrese su correo electrónico y le enviaremos un enlace para restablecer su clave.
            </p>
            <form onSubmit={handleForgotSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-white mb-1">Correo Electrónico:</label>
                <input
                  type="email"
                  value={forgotEmail}
                  onChange={(e) => setForgotEmail(e.target.value)}
                  placeholder="ejemplo@correo.com"
                  className="w-full px-3 py-2 bg-[#111822] border border-gray-700 rounded-lg text-white focus:outline-none focus:border-[#e52335]"
                  required
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsForgotModalOpen(false)}
                  className="px-4 py-2 text-sm bg-gray-700 hover:bg-gray-600 rounded-lg transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm bg-[#e52335] hover:bg-[#c91c2c] text-white font-semibold rounded-lg transition-colors"
                >
                  Enviar enlace
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}