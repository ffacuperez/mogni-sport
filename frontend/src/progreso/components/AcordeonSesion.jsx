import { useId, useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import iconoPendiente from '../assets/icono-pendiente.svg';
import { textoDetalleSesion } from '../services/progresoService';
import Chip from './Chip';
import { TARJETA } from './clases';
import EjercicioIndicado from './EjercicioIndicado';

const ESTADOS = {
  REGISTRADA: { tono: 'ok', texto: 'Registrada', icono: <Check className="h-3 w-3" strokeWidth={2.6} /> },
  PENDIENTE: { tono: 'neutro', texto: 'Pendiente', icono: <img src={iconoPendiente} alt="" className="h-3 w-3" /> },
};

/** Tarjeta de una sesión: al tocarla se expande y muestra sus ejercicios; al tocarla de nuevo se contrae. */
export default function AcordeonSesion({ sesion }) {
  const [abierta, setAbierta] = useState(false);
  const panelId = useId();
  const estado = ESTADOS[sesion.estado];

  return (
    <article className={TARJETA}>
      <button
        type="button"
        onClick={() => setAbierta((actual) => !actual)}
        aria-expanded={abierta}
        aria-controls={panelId}
        className={cx('flex min-h-11 w-full cursor-pointer items-center gap-2.5 rounded-xl py-3 pl-4 pr-3 text-left', FOCUS)}
      >
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="text-[15px] font-bold text-prog-ink">Sesión {sesion.numero}</span>
          <span className="text-[12px] font-medium text-prog-soft">{textoDetalleSesion(sesion)}</span>
        </span>
        <Chip tono={estado.tono} icono={estado.icono}>{estado.texto}</Chip>
        <ChevronDown className={cx('h-5 w-5 shrink-0 text-ink-muted transition-transform duration-200', abierta && 'rotate-180')} />
      </button>

      <div id={panelId} hidden={!abierta}>
        {sesion.ejercicios.map((ejercicio, i) => <EjercicioIndicado key={i} ejercicio={ejercicio} />)}
      </div>
    </article>
  );
}
