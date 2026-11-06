import { cx } from '../../shared/components/clases';
import { Panel } from '../../shared/components/ui';

/** Estado vacío del progreso en el panel: ícono en un círculo, título, texto y una acción opcional. */
export function ContenidoVacio({ icono: Icono, titulo, texto, accion, className }) {
  return (
    <div className={cx('flex flex-col items-center justify-center gap-2 text-center', className)}>
      <span className="mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-error-bg text-primary">
        <Icono className="h-7 w-7" />
      </span>
      <h2 className="text-base font-semibold text-ink">{titulo}</h2>
      <p className="max-w-md text-sm text-ink-soft">{texto}</p>
      {accion && <div className="mt-3">{accion}</div>}
    </div>
  );
}

/** La pantalla vacía completa, en una card alta. */
export default function EstadoVacioPanel(props) {
  return (
    <Panel>
      <ContenidoVacio {...props} className="min-h-[420px] px-6 py-16" />
    </Panel>
  );
}
