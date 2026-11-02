import { useId, useState } from 'react';
import { Check, ChevronDown, Clock } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Panel } from '../../shared/components/ui';
import { textoDetalleSesion } from '../services/progresoService';
import EjercicioIndicado from './EjercicioIndicado';

const ESTADOS = {
  REGISTRADA: { tono: 'success', texto: 'Registrada', icono: Check },
  PENDIENTE: { tono: 'neutral', texto: 'Pendiente', icono: Clock },
  NO_REGISTRADA: { tono: 'neutral', texto: 'No registrada' },
};

/** Tarjeta de una sesión: al tocarla se expande y muestra sus ejercicios; al tocarla de nuevo se contrae. */
export default function AcordeonSesion({ sesion }) {
  const [abierta, setAbierta] = useState(false);
  const panelId = useId();
  const { tono, texto, icono: Icono } = ESTADOS[sesion.estado];

  return (
    <Panel as="article">
      <button
        type="button"
        onClick={() => setAbierta((actual) => !actual)}
        aria-expanded={abierta}
        aria-controls={panelId}
        className={cx('flex min-h-11 w-full cursor-pointer items-center gap-2.5 rounded-lg py-3 pl-4 pr-3 text-left', FOCUS)}
      >
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="text-sm font-bold text-ink">Sesión {sesion.numero}</span>
          <span className="text-xs text-ink-soft">{textoDetalleSesion(sesion)}</span>
        </span>
        <Badge tone={tono} className="shrink-0 gap-1">
          {Icono && <Icono className="h-3 w-3" />}
          {texto}
        </Badge>
        <ChevronDown className={cx('h-5 w-5 shrink-0 text-ink-muted transition-transform duration-200', abierta && 'rotate-180')} />
      </button>

      <div id={panelId} hidden={!abierta}>
        {sesion.ejercicios.map((ejercicio, i) => <EjercicioIndicado key={i} ejercicio={ejercicio} />)}
      </div>
    </Panel>
  );
}
