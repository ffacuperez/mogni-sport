import { useState } from 'react';

import {
  AlertCircle,
  Apple,
  Award,
  BadgeCheck,
  CalendarDays,
  CheckCircle2,
  CreditCard,
  Dumbbell,
  Menu,
  TrendingUp,
  Users,
  X,
} from 'lucide-react';

import Toast from '../../shared/components/Toast';
import { useToast } from '../../shared/hooks/useToast';
import { Logo } from '../../shared/components/ui';
import { FOCUS } from '../../shared/components/clases';

// Colores: tokens de index.css (design system del cliente: grises + rojo #D32F2F).

// Ningún enlace de la barra existe todavía: solo "Iniciar sesión" lleva a algún lado.
const NAV_LINKS = ['Funcionalidades', 'Cómo funciona', 'Membresías', 'Entrenadores', 'FAQ'];

const STATS = [
  { value: '8', unit: 'módulos', caption: 'integrados' },
  { value: '1', unit: 'plataforma', caption: 'para todo el gimnasio' },
  { value: '24/7', unit: 'acceso', caption: 'desde el celular' },
  { value: 'PWA', unit: 'móvil', caption: 'sin tienda de apps' },
];

const FEATURES = [
  { icon: Users, title: 'Alumnos', text: 'Alta, datos personales, objetivo y antecedentes de salud en un solo perfil.' },
  { icon: BadgeCheck, title: 'Membresías', text: 'Planes con duración, clases semanales y precio, asignados a cada alumno.' },
  { icon: CalendarDays, title: 'Turnos', text: 'Turnero automático, reservas, cancelaciones y reprogramaciones con ventana de 2 horas.' },
  { icon: Dumbbell, title: 'Rutinas', text: 'Plantillas reutilizables, mesociclos y cálculo del 1RM a partir del ejercicio guía.' },
  { icon: Apple, title: 'Alimentación', text: 'El plan de alimentación vigente de cada alumno, siempre a mano.' },
  { icon: TrendingUp, title: 'Progreso físico', text: 'Peso levantado, RIR, mediciones corporales y gráficas de evolución.' },
  { icon: Award, title: 'Hábitos y asistencia', text: 'Seguimiento mensual con porcentaje de cumplimiento y medallas.' },
  { icon: CreditCard, title: 'Cuotas', text: 'Vencimientos individuales, recargo por mora y recordatorios de pago.' },
];

// Datos de ejemplo solo para ilustrar el panel del hero.
const ASISTENCIA = [
  { dia: 'L', alto: 62 },
  { dia: 'M', alto: 78 },
  { dia: 'M', alto: 54 },
  { dia: 'J', alto: 88 },
  { dia: 'V', alto: 70 },
  { dia: 'S', alto: 46 },
];

const WHATSAPP_PATH =
  'M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z';

// Notificación con la misma forma que el Toast del resto de la app: tarjeta blanca,
// borde izquierdo del color del tipo, ícono a la izquierda y mensaje.
function Notificacion({ type, title, text, compact = false }) {
  const isSuccess = type === 'success';
  return (
    <div
      className={`flex items-center rounded-lg border border-l-4 border-line bg-card shadow-card ${compact ? 'gap-2 px-2.5 py-2' : 'gap-3 px-5 py-3.5'} ${isSuccess ? 'border-l-success' : 'border-l-error'}`}
    >
      {isSuccess ? (
        <CheckCircle2 className={`shrink-0 text-success ${compact ? 'h-4 w-4' : 'h-5 w-5'}`} />
      ) : (
        <AlertCircle className={`shrink-0 text-error ${compact ? 'h-4 w-4' : 'h-5 w-5'}`} />
      )}
      <div className="min-w-0">
        <p className={`font-medium text-ink ${compact ? 'text-xs leading-tight' : 'text-sm'}`}>{title}</p>
        {text && <p className="mt-0.5 truncate text-[10px] leading-tight text-ink-soft">{text}</p>}
      </div>
    </div>
  );
}

function PanelIlustrativo() {
  return (
    <div className="relative mx-auto w-full max-w-[560px] pb-14 sm:pb-10" aria-hidden="true">
      <div className="rounded-xl border border-line bg-card p-4 shadow-modal sm:p-5">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-primary" />
            <span className="h-2.5 w-2.5 rounded-full bg-line" />
            <span className="h-2.5 w-2.5 rounded-full bg-line" />
          </div>
          <span className="text-xs text-ink-muted">panel de gestión</span>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-lg border border-line bg-page p-3 sm:p-4">
            <p className="text-xs text-ink-soft sm:text-xs">Alumnos activos</p>
            <p className="mt-1 text-2xl font-bold text-ink sm:text-3xl">58</p>
            <p className="mt-0.5 text-xs font-semibold text-success">↑ 6% este mes</p>
          </div>
          <div className="rounded-lg border border-line bg-page p-3 sm:p-4">
            <p className="text-xs text-ink-soft sm:text-xs">Cuotas al día</p>
            <p className="mt-1 text-2xl font-bold text-ink sm:text-3xl">92%</p>
            <p className="mt-0.5 text-xs font-semibold text-success">↑ 4% vs. anterior</p>
          </div>
        </div>

        <div className="mt-3 rounded-lg border border-line bg-page p-3 sm:p-4">
          <p className="text-xs text-ink-soft sm:text-xs">Asistencia semanal</p>
          <div className="mt-3 flex h-24 items-end gap-2 pr-36 sm:h-28 sm:pr-52">
            {ASISTENCIA.map(({ alto }, i) => (
              <div
                key={i}
                className="flex-1 rounded-t-md"
                style={{ height: `${alto}%`, background: 'linear-gradient(to top, var(--color-primary-hover), var(--color-primary))' }}
              />
            ))}
          </div>
          <div className="mt-1.5 flex gap-2 pr-36 sm:pr-52">
            {ASISTENCIA.map(({ dia }, i) => (
              <span key={i} className="flex-1 text-center text-[10px] text-ink-muted">{dia}</span>
            ))}
          </div>
        </div>
      </div>

      <div className="absolute bottom-0 right-0 w-40 rounded-[1.75rem] border border-line bg-sidebar p-2.5 shadow-modal sm:-right-3 sm:w-52 sm:p-3">
        <div className="mx-auto mb-2.5 h-1 w-10 rounded-full bg-sidebar-hover" />
        <div className="space-y-2">
          <Notificacion compact type="success" title="Turno reservado" text="Lucía F. · 19:00" />
          <Notificacion compact type="error" title="Cuota por vencer" text="Martín R. · 3 días" />
          <div className="hidden rounded-lg border border-line bg-card p-2.5 sm:block">
            <div className="flex items-end justify-between gap-2">
              <div>
                <p className="text-xl font-bold leading-none text-ink">68%</p>
                <p className="mt-1 text-[10px] leading-tight text-ink-soft">Aumento de masa muscular</p>
              </div>
              <svg viewBox="0 0 60 28" className="h-7 w-14 shrink-0" fill="none">
                <polyline points="2,24 14,20 26,15 38,11 50,5 58,2" className="stroke-success" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <circle cx="58" cy="2" r="2.5" className="fill-success" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function Home({ isAuthenticated = false, onLogin }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [toast, mostrarToast] = useToast();

  // Aviso para todo lo que todavía no existe en el frontend o en el backend.
  const notificar = (message = 'Esta sección todavía no está disponible.') => mostrarToast(message, 'info');

  const textoLogin = isAuthenticated ? 'Ir al panel' : 'Iniciar sesión';

  const handleLogin = () => {
    setMenuOpen(false);
    onLogin?.();
  };

  const handleLink = () => {
    setMenuOpen(false);
    notificar();
  };

  return (
    <div
      className="min-h-screen w-full overflow-x-hidden bg-page text-ink"
    >

      <Toast key={toast?.key} message={toast?.message} type={toast?.type} />

      {/* Barra de navegación */}
      <header className="sticky top-0 z-40 bg-sidebar">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <Logo onDark />

          <nav className="hidden items-center gap-1 lg:flex" aria-label="Principal">
            {NAV_LINKS.map((label) => (
              <button
                key={label}
                type="button"
                onClick={handleLink}
                className={`cursor-pointer rounded-lg px-3 py-2 text-sm font-medium text-ink-muted transition-colors hover:bg-sidebar-hover hover:text-on-dark ${FOCUS}`}
              >
                {label}
              </button>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleLogin}
              className={`hidden cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-dark transition-colors hover:bg-primary-hover lg:inline-block ${FOCUS}`}
            >
              {textoLogin}
            </button>
            <button
              type="button"
              onClick={() => setMenuOpen((open) => !open)}
              aria-expanded={menuOpen}
              aria-controls="menu-movil"
              aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
              className={`cursor-pointer rounded-lg p-2 text-on-dark transition-colors hover:bg-sidebar-hover lg:hidden ${FOCUS}`}
            >
              {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </div>

        {menuOpen && (
          <div id="menu-movil" className="border-t border-sidebar-hover bg-sidebar px-4 pb-5 pt-2 sm:px-6 lg:hidden">
            <ul>
              {NAV_LINKS.map((label) => (
                <li key={label} className="border-b border-sidebar-hover">
                  <button
                    type="button"
                    onClick={handleLink}
                    className={`w-full cursor-pointer py-3.5 text-left text-sm font-medium text-on-dark ${FOCUS}`}
                  >
                    {label}
                  </button>
                </li>
              ))}
            </ul>
            <button
              type="button"
              onClick={handleLogin}
              className={`mt-5 w-full cursor-pointer rounded-lg bg-primary px-5 py-3 text-sm font-semibold text-on-dark transition-colors hover:bg-primary-hover ${FOCUS}`}
            >
              {textoLogin}
            </button>
          </div>
        )}
      </header>

      <main>
        {/* Hero */}
        <section className="mx-auto grid max-w-7xl items-center gap-14 px-4 pb-16 pt-12 sm:px-6 sm:pt-16 lg:grid-cols-2 lg:gap-10 lg:px-8 lg:pb-24 lg:pt-20">
          <div>
            <h1 className="text-4xl font-bold leading-[1.1] tracking-tight text-ink sm:text-5xl xl:text-6xl">
              Software de gestión para gimnasios de entrenamiento personalizado
            </h1>
            <p className="mt-5 text-xl font-bold text-ink sm:text-2xl">
              Tu gimnasio, <span className="text-primary">100% digital</span>
            </p>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-ink-soft">
              Alumnos, turnos, rutinas, planes de alimentación y cuotas desde una sola plataforma.
              Pensada para usar desde el celular, sin instalar nada desde una tienda de aplicaciones.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <button
                type="button"
                onClick={() => notificar()}
                className={`cursor-pointer rounded-lg bg-primary px-7 py-3 text-sm font-semibold text-on-dark transition-colors hover:bg-primary-hover ${FOCUS}`}
              >
                Ver funcionalidades
              </button>
              <button
                type="button"
                onClick={() => notificar()}
                className={`cursor-pointer rounded-lg border border-primary bg-card px-7 py-3 text-sm font-semibold text-primary transition-colors hover:bg-error-bg ${FOCUS}`}
              >
                Cómo funciona
              </button>
            </div>

            <dl className="mt-12 grid grid-cols-2 gap-x-6 gap-y-6 sm:grid-cols-4">
              {STATS.map(({ value, unit, caption }) => (
                <div key={unit}>
                  <dt className="flex items-baseline gap-1.5">
                    <span className="text-3xl font-bold text-primary">{value}</span>
                    <span className="text-xs text-ink-soft">{unit}</span>
                  </dt>
                  <dd className="mt-1 text-xs text-ink-muted">{caption}</dd>
                </div>
              ))}
            </dl>
          </div>

          <PanelIlustrativo />
        </section>

        {/* Funcionalidades */}
        <section className="border-t border-line bg-card">
          <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
            <h2 className="max-w-2xl text-3xl font-bold tracking-tight text-ink sm:text-4xl">
              Todo lo que necesita tu gimnasio, en una sola plataforma
            </h2>
            <p className="mt-4 max-w-2xl text-base leading-relaxed text-ink-soft">
              Cada área tiene su módulo y todos se conectan entre sí: sin planillas sueltas ni cobros
              por chat.
            </p>

            <ul className="mt-12 grid gap-x-10 gap-y-2 sm:grid-cols-2 lg:grid-cols-4">
              {FEATURES.map(({ icon: Icon, title, text }) => (
                <li key={title} className="border-t border-line py-6">
                  <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-error-bg text-primary">
                    <Icon className="h-5 w-5" />
                  </span>
                  <h3 className="mt-4 text-base font-semibold text-ink">{title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-ink-soft">{text}</p>
                </li>
              ))}
            </ul>
          </div>
        </section>
      </main>

      <footer className="bg-sidebar">
        <div className="mx-auto flex max-w-7xl flex-col items-start justify-between gap-4 px-4 py-8 sm:flex-row sm:items-center sm:px-6 lg:px-8">
          <Logo onDark />
          <p className="text-xs text-ink-muted">© 2026 Mogni Sport. Sistema de gestión para gimnasio boutique.</p>
        </div>
      </footer>

      {/* Botón flotante de WhatsApp */}
      <button
        type="button"
        onClick={() => notificar('El contacto por WhatsApp todavía no está disponible.')}
        aria-label="Contactar por WhatsApp"
        className={`fixed bottom-5 right-5 z-40 flex h-14 w-14 cursor-pointer items-center justify-center rounded-full bg-whatsapp text-on-dark shadow-modal transition-transform hover:scale-105 ${FOCUS}`}
      >
        <svg viewBox="0 0 24 24" className="h-7 w-7" fill="currentColor" aria-hidden="true">
          <path d={WHATSAPP_PATH} />
        </svg>
      </button>
    </div>
  );
}
