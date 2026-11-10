import { useEffect, useState } from 'react';
import {
  Banknote, CalendarCheck, ChevronRight, CircleAlert, Landmark, OctagonAlert, UserMinus, UserPlus, Users, Wallet,
} from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { formatearMonto } from '../../shared/services/texto';
import GraficoIngresos from '../components/GraficoIngresos';
import { consultarPanel, textoMes, textoPorcentaje, textoVariacion } from '../services/panelService';

const TONO_VARIACION = (valor) => {
  if (valor == null || valor === 0) return 'bg-row-hover text-ink-soft';
  return valor > 0 ? 'bg-success-bg text-success' : 'bg-error-bg text-error';
};

/** Número clave con ícono y etiqueta. Con onClick es un botón que lleva a otra pantalla. */
function Tarjeta({ label, value, detalle, icon: Icon, tono = 'neutral', onClick }) {
  const colores = {
    neutral: 'bg-row-hover text-ink-soft',
    primary: 'bg-error-bg text-primary',
    success: 'bg-success-bg text-success',
    error: 'bg-error-bg text-error',
  }[tono];
  const contenido = (
    <>
      <span className={cx('flex h-10 w-10 shrink-0 items-center justify-center rounded-lg', colores)}>
        <Icon className="h-5 w-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-2xl font-bold tabular-nums text-ink">{value}</span>
        <span className="block text-xs text-ink-soft">{label}</span>
        {detalle && <span className="mt-0.5 block text-[11px] text-ink-muted">{detalle}</span>}
      </span>
      {onClick && <ChevronRight className="h-4 w-4 shrink-0 text-ink-muted" />}
    </>
  );
  const clases = 'flex min-w-0 items-center gap-3 rounded-lg border border-line bg-card p-4 text-left shadow-card';
  if (!onClick) return <div className={clases}>{contenido}</div>;
  return (
    <button type="button" onClick={onClick} className={cx(clases, 'cursor-pointer transition-colors hover:bg-row-hover', FOCUS)}>
      {contenido}
    </button>
  );
}

function Ingresos({ ingresos }) {
  const { mes, variacionPorcentaje, efectivo, transferencia, recargos } = ingresos;
  const desglose = [
    { label: 'Efectivo', valor: efectivo, icon: Banknote },
    { label: 'Transferencia', valor: transferencia, icon: Landmark },
    { label: 'Recargos cobrados', valor: recargos, icon: Wallet },
  ];
  return (
    <Panel className="p-4 sm:p-5">
      <p className="text-sm text-ink-soft">Ingresos del mes</p>
      <div className="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <p className="text-3xl font-bold tabular-nums text-ink">{formatearMonto(mes)}</p>
        <span className={cx('rounded-full px-2.5 py-0.5 text-xs font-semibold tabular-nums', TONO_VARIACION(variacionPorcentaje))}>
          {textoVariacion(variacionPorcentaje)}
        </span>
        <span className="text-xs text-ink-muted">vs. mes anterior</span>
      </div>
      <p className="mt-1 text-[11px] text-ink-muted">Incluye recargos y diferencias por cambio de membresía.</p>
      <ul className="mt-4 grid grid-cols-1 gap-2 border-t border-line pt-4 sm:grid-cols-3">
        {desglose.map(({ label, valor, icon: Icon }) => (
          <li key={label} className="flex items-center justify-between gap-2 rounded-lg bg-row-hover px-3 py-2 sm:flex-col sm:items-start">
            <span className="flex items-center gap-2 text-xs text-ink-soft"><Icon className="h-4 w-4" /> {label}</span>
            <span className="text-sm font-semibold tabular-nums text-ink">{formatearMonto(valor)}</span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

function ActivosPorMembresia({ filas, total }) {
  return (
    <Panel className="p-4 sm:p-5">
      <h2 className="text-base font-semibold text-ink">Alumnos activos por membresía</h2>
      {filas.length === 0 ? (
        <p className="mt-3 text-sm text-ink-soft">No hay alumnos activos.</p>
      ) : (
        <ul className="mt-3 space-y-3">
          {filas.map(({ membresia, cantidad }) => (
            <li key={membresia ?? 'sin-membresia'}>
              <div className="flex items-baseline justify-between gap-2 text-sm">
                <span className={cx('truncate', membresia ? 'text-ink' : 'italic text-ink-soft')}>
                  {membresia ?? 'Sin membresía activa'}
                </span>
                <span className="font-semibold tabular-nums text-ink">{cantidad}</span>
              </div>
              <div aria-hidden="true" className="mt-1 h-1.5 rounded-full bg-line">
                <div className="h-full rounded-full bg-primary" style={{ width: `${total > 0 ? (cantidad / total) * 100 : 0}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}

/** Panel del propietario: los números clave del mes arriba y el gráfico de ingresos abajo. */
export default function PanelPropietario({ onVerMora }) {
  const [panel, setPanel] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarPanel()
      .then((datos) => { if (vigente) setPanel(datos); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error.message}</span>
      </div>
    );
  }

  if (!panel) return <Cargando texto="Cargando el panel" />;

  const { fecha, ingresos, ingresosPorMes, alumnos, activosPorMembresia, ocupacion, asistencia } = panel;

  return (
    <div className="space-y-4">
      <PageHeader title="Inicio" subtitle={`Resumen de ${textoMes(fecha)}`} />

      <Ingresos ingresos={ingresos} />

      <div className="grid grid-cols-1 gap-3 min-[420px]:grid-cols-2 lg:grid-cols-3">
        <Tarjeta label="Alumnos activos" value={alumnos.activos} icon={Users} tono="primary" />
        <Tarjeta label="Inscripciones nuevas del mes" value={alumnos.nuevosDelMes} icon={UserPlus} tono="success" />
        <Tarjeta label="Bajas del mes" value={alumnos.bajasDelMes} icon={UserMinus} />
        <Tarjeta label="Alumnos en mora" value={alumnos.enMora} icon={OctagonAlert} tono="error"
          detalle="Ver en Alumnos" onClick={onVerMora} />
        <Tarjeta label="Ocupación de los turnos" value={textoPorcentaje(ocupacion.porcentaje)} icon={CalendarCheck}
          detalle={`${ocupacion.reservas} reservas sobre ${ocupacion.cupo} lugares`} />
        <Tarjeta label="Asistencia del mes" value={textoPorcentaje(asistencia.porcentaje)} icon={CalendarCheck}
          detalle={`${asistencia.presentes} presentes · ${asistencia.ausentes} ausentes`} />
      </div>

      <ActivosPorMembresia filas={activosPorMembresia} total={alumnos.activos} />

      <Panel className="p-4 sm:p-5">
        <h2 className="text-base font-semibold text-ink">Ingresos de los últimos {ingresosPorMes.length} meses</h2>
        <div className="mt-4">
          <GraficoIngresos meses={ingresosPorMes} />
        </div>
      </Panel>
    </div>
  );
}
