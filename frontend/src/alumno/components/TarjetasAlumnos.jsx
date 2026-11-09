import { createPortal } from 'react-dom';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';

import { COLUMNAS_ACTIVOS } from '../services/listaAlumnos';
import { avisoVencimiento, formatearFecha, formatearMonto, tonoEstadoCuota } from '../../cuota/services/cuotasService';
import { tituloPlan } from '../../progreso/services/progresoService';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { Badge, Button } from '../../shared/components/ui';

function Dato({ label, children, className }) {
  return (
    <span className={cx('block min-w-0', className)}>
      <span className="block text-[11px] text-ink-soft">{label}</span>
      {children}
    </span>
  );
}

function DatosActivo({ alumno }) {
  const { planActivo: plan, cuota } = alumno;
  return (
    <>
      <Dato label="Plan">{plan ? tituloPlan(plan) : 'Sin plan'}</Dato>
      <Dato label="Semana">{plan ? `${plan.semanaActual} de ${plan.cantidadSemanas}` : '—'}</Dato>
      <Dato label="Membresía">
        {alumno.membresia}
        {alumno.membresiaProgramada && (
          <span className="mt-0.5 block text-xs text-ink-soft">
            → {alumno.membresiaProgramada} desde {formatearFecha(alumno.programadaDesde)}
          </span>
        )}
      </Dato>
      <Dato label="Vencimiento">{formatearFecha(cuota?.vencimiento)}</Dato>
      <Dato label="Próx. cuota">{cuota?.precio != null ? formatearMonto(cuota.precio) : '—'}</Dato>
    </>
  );
}

function DatosInactivo({ alumno }) {
  return (
    <>
      <Dato label="Última membresía">{alumno.inscripcion?.membresia ?? '—'}</Dato>
      <Dato label="Fecha de baja">{formatearFecha(alumno.fechaBaja)}</Dato>
      <Dato label="Última cuota venció">{formatearFecha(alumno.vencimientoUltimaCuota)}</Dato>
      <Dato label="Reactivación">{alumno.reactivacionRequierePago ? 'Con pago' : 'Sin pago'}</Dato>
    </>
  );
}

function Tarjeta({ alumno, esActivos, seleccionada, onElegir }) {
  const aviso = esActivos && alumno.cuota && avisoVencimiento(alumno.cuota);
  return (
    <button
      type="button"
      onClick={onElegir}
      aria-pressed={seleccionada}
      className={cx(
        'block w-full cursor-pointer rounded-lg border-2 p-3 text-left text-ink shadow-card transition-colors',
        seleccionada ? 'border-primary-light bg-error-bg/60' : 'border-line bg-card hover:bg-row-hover',
        FOCUS,
      )}
    >
      <span className="flex items-start justify-between gap-2">
        <span className="min-w-0 font-semibold">{alumno.apellido}, {alumno.nombre}</span>
        <span className="shrink-0 text-right">
          {esActivos
            ? <Badge tone={tonoEstadoCuota(alumno.cuota?.estado)}>{alumno.cuota?.estado ?? 'Sin cuotas'}</Badge>
            : <Badge tone={alumno.reactivacionRequierePago ? 'alert' : 'success'}>{alumno.reactivacionRequierePago ? 'Con pago' : 'Sin pago'}</Badge>}
          {aviso && <span className="mt-1 block text-xs text-error">{aviso}</span>}
        </span>
      </span>
      <span className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 border-t border-line-soft pt-2 text-[13px]">
        {esActivos ? <DatosActivo alumno={alumno} /> : <DatosInactivo alumno={alumno} />}
      </span>
    </button>
  );
}

/** La lista en pantallas chicas: una columna en celular y dos desde sm. */
export default function TarjetasAlumnos({ alumnos, esActivos, selectedId, setSelectedId, vacio }) {
  if (alumnos.length === 0) return <p className="px-3 py-12 text-center text-sm text-ink-muted">{vacio}</p>;
  return (
    <div className="grid grid-cols-1 items-start gap-2 sm:grid-cols-2">
      {alumnos.map((alumno) => {
        const seleccionada = alumno.id === selectedId;
        return (
          <Tarjeta
            key={alumno.id}
            alumno={alumno}
            esActivos={esActivos}
            seleccionada={seleccionada}
            onElegir={() => setSelectedId(seleccionada ? null : alumno.id)}
          />
        );
      })}
    </div>
  );
}

/** Mismo estado que los encabezados de la tabla: "Situación de pago" es el orden normal, sin columna elegida. */
export function SelectorOrden({ orden, setOrden }) {
  const Flecha = { asc: ArrowUp, desc: ArrowDown }[orden?.direccion] ?? ArrowUpDown;
  return (
    <div className="flex min-w-0 flex-1 items-center gap-2">
      <label htmlFor="orden-alumnos" className="whitespace-nowrap text-[13px] text-ink-soft">Ordenar por</label>
      <select
        id="orden-alumnos"
        value={orden?.columna ?? ''}
        onChange={(e) => setOrden(e.target.value ? { columna: e.target.value, direccion: 'asc' } : null)}
        className={cx(inputClass, 'min-w-0 flex-1')}
      >
        <option value="">Situación de pago</option>
        {COLUMNAS_ACTIVOS.map(({ clave, label }) => <option key={clave} value={clave}>{label}</option>)}
      </select>
      <button
        type="button"
        onClick={() => setOrden({ ...orden, direccion: orden.direccion === 'asc' ? 'desc' : 'asc' })}
        disabled={!orden}
        aria-label="Invertir el orden"
        title="Invertir el orden"
        className={cx(
          'flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-lg border border-line bg-card disabled:cursor-not-allowed',
          orden ? 'text-primary' : 'text-ink-muted',
          FOCUS,
        )}
      >
        <Flecha aria-hidden="true" className="h-4 w-4" />
      </button>
    </div>
  );
}

/**
 * Barra fija con las acciones del alumno seleccionado. Va en un portal a body: dentro de la página, animate-fade-in
 * dejaría el fixed encerrado en ella. Queda debajo de los modales (z-50).
 */
export function BarraAcciones({ alumno, acciones, motivo }) {
  return createPortal(
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-card px-3 pt-2.5 pb-[calc(0.75rem+env(safe-area-inset-bottom))] shadow-modal">
      <p className="mb-2 truncate text-[13px] font-semibold text-ink">{alumno.apellido}, {alumno.nombre}</p>
      <div className="grid auto-cols-fr grid-flow-col gap-1.5">
        {acciones.map(({ texto, icono: Icono, variante, deshabilitado, onClick }) => (
          <Button key={texto} variant={variante} size="barra" onClick={onClick} disabled={deshabilitado}>
            <Icono className="h-4 w-4 shrink-0" /> {texto}
          </Button>
        ))}
      </div>
      {motivo && <p className="mt-1.5 text-xs text-ink-soft">{motivo}</p>}
    </div>,
    document.body,
  );
}
