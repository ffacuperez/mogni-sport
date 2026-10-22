import { useEffect, useState } from 'react';
import { CircleAlert, TriangleAlert } from 'lucide-react';

import { formatearMonto, formatearPeriodo, MEDIOS_DE_PAGO, obtenerRenovacion, SITUACIONES } from '../services/cuotasService';
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
 * muestra en grande para confirmar: un pago registrado no se puede deshacer.
 */
export default function RegistrarPagoModal({ cuota, onConfirm, onCancel, onError }) {
  const [renovacion, setRenovacion] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    obtenerRenovacion(cuota.alumnoId)
      .then(setRenovacion)
      .catch((error) => setErrorCarga(error.message));
  }, [cuota.alumnoId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!medioDePago) {
      onError('Seleccioná un medio de pago.');
      return;
    }
    setEnviando(true);
    try {
      await onConfirm({ alumnoId: cuota.alumnoId, medioDePago });
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
