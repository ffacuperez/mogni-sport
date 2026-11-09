import { useState } from 'react';
import { TriangleAlert } from 'lucide-react';

import { formatearFecha, formatearMonto, MEDIOS_DE_PAGO } from '../../cuota/services/cuotasService';
import { Button, Field, Modal } from '../../shared/components/ui';
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
 * Confirmación del cambio de membresía (reglas M1 a M9), con los datos de la vista previa del backend. Si la nueva es
 * más cara se cobra la diferencia y hay que elegir el medio de pago; si no, no se cobra nada (sin reembolso).
 */
export default function CambioMembresiaModal({ alumno, vista, onConfirm, onCancel, onError }) {
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);
  const cobra = vista.requiereMedioDePago;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (cobra && !medioDePago) {
      onError('Seleccioná el medio de pago de la diferencia.');
      return;
    }
    setEnviando(true);
    try {
      await onConfirm(medioDePago);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      title="Confirmar cambio de membresía"
      subtitle={`${alumno.nombre} ${alumno.apellido} · DNI ${alumno.dni}`}
      onClose={onCancel}
      cerrable={!enviando}
    >
      <form onSubmit={handleSubmit}>
        <dl className="divide-y divide-line-soft">
          <Dato label="Membresía actual">{vista.membresiaActual ?? 'Sin membresía'}</Dato>
          <Dato label="Membresía nueva">{vista.membresiaNueva}</Dato>
          <Dato label="Desde">{formatearFecha(vista.desde)}</Dato>
          {vista.hasta && <Dato label="Hasta (mismo vencimiento)">{formatearFecha(vista.hasta)}</Dato>}
          {vista.precioPagado != null && <Dato label="Pagado por la membresía actual">{formatearMonto(vista.precioPagado)}</Dato>}
          {vista.precioPagado != null && <Dato label="Precio de la membresía nueva">{formatearMonto(vista.precioNuevo)}</Dato>}
          {vista.diferenciaACobrar != null && <Dato label="A cobrar hoy" destacado>{formatearMonto(vista.diferenciaACobrar)}</Dato>}
        </dl>

        {vista.diasRestantes != null && (
          <p className="mt-3 text-sm text-ink">
            ¿Seguro que querés cambiar desde hoy? Le {vista.diasRestantes === 1 ? 'queda 1 día' : `quedan ${vista.diasRestantes} días`} de
            la cuota actual y{' '}
            {cobra
              ? <>tiene que pagar <strong>{formatearMonto(vista.diferenciaACobrar)}</strong> de diferencia.</>
              : 'no se cobra nada y no hay reembolso.'}
            {' '}Para cambiarla desde la próxima cuota, registrá el pago desde Alumnos eligiendo la membresía nueva.
          </p>
        )}

        {vista.sinReembolso && vista.diasRestantes == null && (
          <p className="mt-3 text-sm text-ink-soft">La membresía nueva es más barata: no hay reembolso de la diferencia.</p>
        )}

        {cobra && (
          <>
            <Field label="Medio de pago de la diferencia" htmlFor="cambio-medio" className="mt-4">
              <select id="cambio-medio" value={medioDePago} onChange={(e) => setMedioDePago(e.target.value)} className={inputClass} autoFocus>
                <option value="">Seleccionar...</option>
                {MEDIOS_DE_PAGO.map((m) => (
                  <option key={m.value} value={m.value}>{m.label}</option>
                ))}
              </select>
            </Field>
            <p className="mt-4 flex items-start gap-2 rounded-lg bg-warning-bg px-3 py-2.5 text-sm text-warning-text">
              <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0" />
              Revisá los datos antes de confirmar: un pago registrado no se puede deshacer.
            </p>
          </>
        )}

        <div className="flex justify-end gap-3 pt-5">
          <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={enviando}>
            {enviando ? 'Guardando...' : cobra ? 'Confirmar y registrar pago' : 'Confirmar cambio'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
