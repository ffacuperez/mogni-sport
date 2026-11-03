import { KeyRound, Mail, ShieldCheck } from 'lucide-react';

import { getEmail, getRol } from '../services/authService';
import { PageHeader, Panel } from '../../shared/components/ui';
import FormCambiarPassword from '../components/FormCambiarPassword';

const NOMBRE_ROL = { PROPIETARIO: 'Propietario', ENTRENADOR: 'Entrenador', NUTRICIONISTA: 'Encargado de nutrición' };

/** MOGNI-263: «Mi cuenta» del personal (propietario, entrenador y nutricionista): datos de la cuenta y cambio de contraseña. */
export default function MiCuenta() {
  const rol = getRol();
  return (
    <div className="animate-fade-in">
      <PageHeader eyebrow="Configuración" title="Mi cuenta" subtitle="Datos de acceso y seguridad de tu cuenta." />

      <div className="grid max-w-3xl gap-6">
        <Panel className="p-4 sm:p-6">
          <h2 className="mb-4 text-base font-semibold text-ink">Datos de acceso</h2>
          <ul className="space-y-3 text-sm">
            <li className="flex items-center gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary"><Mail className="h-5 w-5" /></span>
              <span className="min-w-0">
                <span className="block text-xs font-medium text-ink-soft">Email</span>
                <span className="block truncate font-semibold text-ink">{getEmail() ?? '—'}</span>
              </span>
            </li>
            <li className="flex items-center gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary"><ShieldCheck className="h-5 w-5" /></span>
              <span>
                <span className="block text-xs font-medium text-ink-soft">Perfil</span>
                <span className="block font-semibold text-ink">{NOMBRE_ROL[rol] ?? rol}</span>
              </span>
            </li>
          </ul>
        </Panel>

        <Panel className="p-4 sm:p-6">
          <div className="mb-5 flex items-start gap-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary"><KeyRound className="h-5 w-5" /></span>
            <div>
              <h2 className="text-base font-semibold text-ink">Cambiar contraseña</h2>
              <p className="mt-0.5 text-sm text-ink-soft">Vas a seguir con la sesión iniciada; la próxima vez entrás con la contraseña nueva.</p>
            </div>
          </div>
          <FormCambiarPassword />
        </Panel>
      </div>
    </div>
  );
}
