import { Lock } from 'lucide-react';

import { inputClass, labelClass } from '../../shared/components/clases';

/** Campo de contraseña con ícono (mismo estilo que el login). visible: muestra el texto. */
export default function CampoPassword({ id, label, value, onChange, visible = false, autoComplete, ayuda, invalido = false }) {
  return (
    <div>
      <label htmlFor={id} className={labelClass}>{label}</label>
      <div className="relative">
        <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          autoComplete={autoComplete}
          aria-invalid={invalido || undefined}
          aria-describedby={ayuda ? `${id}-ayuda` : undefined}
          className={`${inputClass} py-3 pl-11`}
        />
      </div>
      {ayuda && <p id={`${id}-ayuda`} className="mt-1.5 text-xs text-ink-muted">{ayuda}</p>}
    </div>
  );
}
