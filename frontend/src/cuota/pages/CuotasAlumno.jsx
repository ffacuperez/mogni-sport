import { useEffect, useState } from 'react';
import { Wallet } from 'lucide-react';

import { Badge, Button, Cargando, Panel } from '../../shared/components/ui';
import TablaPagos from '../components/TablaPagos';
import { avisoVencimiento, formatearFecha, formatearMonto, motivoSinRenovacion, obtenerHistorialDePagos, tonoEstadoCuota } from '../services/cuotasService';

function Dato({ label, children }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs font-medium text-ink-soft">{label}</dt>
      <dd className="mt-1 font-semibold text-ink">{children}</dd>
    </div>
  );
}

/** Registrar pago y la situación de cuota del alumno activo: en celular, el botón arriba y los datos debajo. */
function TarjetaCuota({ alumno, onRegistrarPago }) {
  const { cuota } = alumno;
  const aviso = cuota && avisoVencimiento(cuota);
  return (
    <Panel aria-label="Situación de pago" className="flex flex-col gap-x-10 gap-y-4 p-4 sm:p-5 md:flex-row md:items-center md:justify-between">
      <div className="flex shrink-0 flex-col gap-1.5 md:max-w-56">
        <Button variant="primary" className="w-full md:w-auto md:self-start" onClick={onRegistrarPago} disabled={!cuota?.puedeRenovar}>
          <Wallet className="h-4 w-4" /> Registrar pago
        </Button>
        {!cuota?.puedeRenovar && <p className="text-xs text-ink-soft">{motivoSinRenovacion(cuota)}</p>}
      </div>
      <dl className="grid grid-cols-2 gap-x-3 gap-y-3.5 md:flex md:flex-wrap md:items-start md:gap-x-10 md:gap-y-4">
        <Dato label="Membresía">
          {alumno.membresia}
          {alumno.membresiaProgramada && (
            <span className="mt-1 block text-xs font-normal text-ink-soft">
              → {alumno.membresiaProgramada} desde {formatearFecha(alumno.programadaDesde)}
            </span>
          )}
        </Dato>
        <Dato label="Vencimiento"><span className="tabular-nums">{formatearFecha(cuota?.vencimiento)}</span></Dato>
        <Dato label="Próxima cuota"><span className="tabular-nums">{cuota?.precio != null ? formatearMonto(cuota.precio) : '—'}</span></Dato>
        <Dato label="Situación">
          <Badge tone={tonoEstadoCuota(cuota?.estado)}>{cuota?.estado ?? 'Sin cuotas'}</Badge>
          {aviso && <span className="mt-1 block text-xs font-normal text-error">{aviso}</span>}
        </Dato>
      </dl>
    </Panel>
  );
}

/**
 * Tab "Cuotas" de la ficha (solo el propietario): con el alumno activo, Registrar pago y su situación; debajo, el
 * historial de pagos (HU-48). De un alumno inactivo se ve solo el historial.
 */
export default function CuotasAlumno({ alumno, onRegistrarPago, refrescoPagos }) {
  // null: cargando
  const [pagos, setPagos] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    obtenerHistorialDePagos(alumno.id)
      .then((respuesta) => {
        if (!vigente) return;
        setPagos(respuesta);
        setError(null);
      })
      .catch((err) => { if (vigente) setError(err.message); });
    return () => { vigente = false; };
  }, [alumno.id, refrescoPagos]);

  return (
    <div className="flex flex-col gap-5">
      {alumno.estado === 'Activo' && <TarjetaCuota alumno={alumno} onRegistrarPago={onRegistrarPago} />}
      <Panel className="p-4 sm:p-6">
        <h2 className="mb-5 border-b border-line pb-4 text-base font-semibold text-ink">Historial de pagos</h2>
        {error && <p role="alert" className="text-sm text-error">No se pudo consultar el historial: {error}</p>}
        {!error && !pagos && <Cargando texto="Cargando historial" className="py-8" />}
        {pagos?.length === 0 && (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-error-bg text-primary">
              <Wallet className="h-6 w-6" />
            </span>
            <p className="text-sm text-ink-soft">Este alumno no tiene pagos registrados.</p>
          </div>
        )}
        {pagos?.length > 0 && <TablaPagos pagos={pagos} />}
      </Panel>
    </div>
  );
}
