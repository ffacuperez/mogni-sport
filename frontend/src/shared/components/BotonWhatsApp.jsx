import { linkWhatsApp } from '../services/telefono';
import { cx, FOCUS } from './clases';
import { WHATSAPP_PATH } from './iconoWhatsApp';
import { Button } from './ui';

/** Abre un chat con el alumno. Con un número incompleto queda deshabilitado; sin teléfono, no hay que mostrarlo. */
export default function BotonWhatsApp({ telefono }) {
  const link = linkWhatsApp(telefono);
  if (!link) {
    return (
      <span className="flex flex-col items-end gap-1">
        <Button size="sm" disabled title="El número no tiene característica o está incompleto">WhatsApp</Button>
        <span className="text-[11px] text-ink-muted">Número incompleto</span>
      </span>
    );
  }
  return (
    <a
      href={link}
      target="_blank"
      rel="noopener noreferrer"
      className={cx('inline-flex shrink-0 items-center gap-2 rounded-lg bg-whatsapp px-4 py-2 text-sm font-semibold text-on-dark transition-[filter] hover:brightness-95', FOCUS)}
    >
      <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor" aria-hidden="true">
        <path d={WHATSAPP_PATH} />
      </svg>
      WhatsApp
    </a>
  );
}
