import { useEffect, useState } from 'react';
import { CircleAlert, Info, TriangleAlert } from 'lucide-react';

import { formatearFecha, formatearMonto, formatearPeriodo, MEDIOS_DE_PAGO, obtenerRenovacion, SITUACIONES } from '../services/cuotasService';
import { Button, Cargando, Field, Modal } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

function Dato({ label, children, destacado = false }) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-2">
      <dt className="text-sm text-ink-soft">{label}</dt>
      <dd className={destacado ? 'text-2xl font-bold tabular-nums text-ink' : 'text-right text-sm font-semibold tabular-nums text-ink'}>
        {children}
      </dd>
    </div>
  );
}

/**
 * MOGNI-45 y MOGNI-80: renovación del alumno. Pide al backend la vista previa (período, precio, recargo y total) y la
 * muestra en grande para confirmar: un pago registrado no se puede deshacer. Se puede renovar con otra membresía
 * (20.10): por defecto viene la actual y, al elegir otra, la vista previa dice desde cuándo rige.
 */
export default function RegistrarPagoModal({ cuota, membresias, onConfirm, onCancel, onError }) {
  const actualId = cuota.membresiaId != null ? String(cuota.membresiaId) : '';
  const [membresiaId, setMembresiaId] = useState(actualId);
  const [renovacion, setRenovacion] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);
  const otraMembresia = membresiaId !== '' && membresiaId !== actualId ? membresiaId : null;

  useEffect(() => {
    // Si se cambia la membresía rápido, solo cuenta la respuesta de la última elegida
    let vigente = true;
    setRenovacion(null);
    setErrorCarga(null);
    obtenerRenovacion(cuota.alumnoId, otraMembresia)
      .then((r) => vigente && setRenovacion(r))
      .catch((error) => vigente && setErrorCarga(error.message));
    return () => {
      vigente = false;
    };
  }, [cuota.alumnoId, otraMembresia]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!medioDePago) {
      onError('Seleccioná un medio de pago.');
      return;
    }
    setEnviando(true);
    try {
      await onConfirm({ alumnoId: cuota.alumnoId, medioDePago, membresiaId: otraMembresia });
    } finally {
      setEnviando(false);
    }
  };

  const conRecargo = renovacion?.recargo > 0;

  return (
    <Modal title="Registrar pago de cuota" onClose={onCancel} cerrable={!enviando} size="lg">
      <div className="mb-5 rounded-lg border border-line bg-row-hover px-4 py-3">
        <p className="text-xl font-bold text-ink">{cuota.nombre} {cuota.apellido}</p>
        <p className="text-base tabular-nums text-ink-soft">DNI {cuota.dni}</p>
      </div>

      {errorCarga && (
        <p role="alert" className="flex items-center gap-2 text-sm text-error">
          <CircleAlert className="h-4 w-4 shrink-0" /> {errorCarga}
        </p>
      )}

      {!renovacion && !errorCarga && <Cargando texto="Calculando el pago" className="py-6" />}

      {renovacion && !renovacion.puedeRenovar && (
        <>
          <p role="alert" className="flex items-start gap-2 rounded-lg bg-error-bg px-3 py-2.5 text-sm text-error">
            <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {renovacion.motivo}
          </p>
          <div className="mt-6 flex justify-end">
            <Button variant="ghost" onClick={onCancel}>Cerrar</Button>
          </div>
        </>
      )}

      {renovacion?.puedeRenovar && (
        <form onSubmit={handleSubmit}>
          {membresias.length > 1 && actualId && (
            <Field label="Membresía" htmlFor="pago-membresia" className="mb-2">
              <select id="pago-membresia" value={membresiaId} onChange={(e) => setMembresiaId(e.target.value)} className={inputClass}>
                {membresias.map((m) => (
                  <option key={m.id} value={String(m.id)}>
                    {m.nombre}{String(m.id) === actualId ? ' (actual)' : ''}
                  </option>
                ))}
              </select>
            </Field>
          )}

          {renovacion.cambioDeMembresia && (
            <p className="mb-2 flex items-start gap-2 rounded-lg bg-info-bg px-3 py-2.5 text-sm text-info">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              {renovacion.programada
                ? `Pasa de ${renovacion.membresiaActual} a ${renovacion.membresiaNombre} desde el ${formatearFecha(renovacion.rigeDesde)}. Hasta ese día sigue con ${renovacion.membresiaActual}. El cambio no se puede cancelar.`
                : `La membresía nueva (${renovacion.membresiaNombre}) empieza hoy, en lugar de ${renovacion.membresiaActual}.`}
            </p>
          )}

          <dl className="divide-y divide-line-soft">
            <Dato label="Membresía">{renovacion.membresiaNombre}</Dato>
            <Dato label="Período que cubre">{formatearPeriodo(renovacion.fechaInicio, renovacion.fechaVencimiento)}</Dato>
            {renovacion.situacion && <Dato label="Situación actual">{SITUACIONES[renovacion.situacion]}</Dato>}
            <Dato label="Precio de la cuota">{formatearMonto(renovacion.precio)}</Dato>
            {conRecargo && <Dato label="Recargo por mora (10%, redondeado)">{formatearMonto(renovacion.recargo)}</Dato>}
            <Dato label="Total a cobrar" destacado>{formatearMonto(renovacion.total)}</Dato>
          </dl>

          <Field label="Medio de pago" htmlFor="medio-de-pago" className="mt-4">
            <select id="medio-de-pago" value={medioDePago} onChange={(e) => setMedioDePago(e.target.value)} className={inputClass} autoFocus>
              <option value="">Seleccionar...</option>
              {MEDIOS_DE_PAGO.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </Field>

          <p className="mt-4 flex items-start gap-2 rounded-lg bg-warning-bg px-3 py-2.5 text-sm text-warning-text">
            <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0" />
            Revisá los datos antes de confirmar: un pago registrado no se puede deshacer. La fecha del pago es la de hoy.
          </p>

          <div className="flex justify-end gap-3 pt-5">
            <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
            <Button type="submit" variant="primary" disabled={enviando}>
              {enviando ? 'Registrando...' : 'Confirmar pago'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
