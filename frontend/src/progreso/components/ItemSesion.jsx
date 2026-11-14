import { Link } from 'react-router';
import { ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Panel } from '../../shared/components/ui';
import { partesFecha } from '../../turno/services/turnosService';
import { useProgreso } from '../services/ProgresoContext';
import { textoDiaMes, tituloSesion } from '../services/progresoService';

/** Una sesión registrada del listado: fecha, nombre y cantidad de ejercicios. Abre su detalle. */
export default function ItemSesion({ sesion }) {
  const { rutaBase } = useProgreso();
  const cantidad = sesion.ejerciciosRegistrados === 1 ? '1 ejercicio registrado' : `${sesion.ejerciciosRegistrados} ejercicios registrados`;
  return (
    <Panel
      as={Link}
      to={`${rutaBase}/sesiones/${sesion.id}`}
      className={cx('flex items-center gap-3 p-2.5 pr-3 transition-colors hover:bg-row-hover', FOCUS)}
    >
      <div className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-lg bg-error-bg text-primary">
        <span className="text-[10px] font-bold uppercase leading-none">{partesFecha(sesion.fecha).dia}</span>
        <span className="mt-0.5 text-base font-bold leading-none tabular-nums">{textoDiaMes(sesion.fecha)}</span>
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-bold text-ink">{tituloSesion(sesion.nombreSesion, sesion.numeroSesion)}</p>
        <p className="text-xs text-ink-soft">{cantidad}</p>
      </div>
      <ChevronRight className="h-5 w-5 shrink-0 text-ink-muted" />
    </Panel>
  );
}
