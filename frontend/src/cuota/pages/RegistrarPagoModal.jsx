import { useState } from 'react';

import { formatearMonto, formatearPeriodo, MEDIOS_DE_PAGO } from '../services/cuotasService';
import { Button, Field, Modal, ReadOnly } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

export default function RegistrarPagoModal({ cuota, onConfirm, onCancel, onError }) {
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (!cuota) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!medioDePago) {
      onError('Seleccione un medio de pago.');
      return;
    }
    setEnviando(true);
    try {
      // El monto no lo ingresa el propietario: siempre es el de la cuota (ver cuotasService.registrarPago).
      await onConfirm({ cuotaId: cuota.id, medioDePago });
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      title="Registrar pago de cuota"
      subtitle={`${cuota.nombre} ${cuota.apellido} · DNI ${cuota.dni}`}
      onClose={onCancel}
      cerrable={!enviando}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Cuota">
            <ReadOnly numerico>{formatearPeriodo(cuota.periodo)}</ReadOnly>
          </Field>
          <Field label="Monto a pagar">
            <ReadOnly numerico>{formatearMonto(cuota.monto)}</ReadOnly>
          </Field>
        </div>

        <Field label="Medio de pago" htmlFor="medio-de-pago">
          <select id="medio-de-pago" value={medioDePago} onChange={(e) => setMedioDePago(e.target.value)} className={inputClass} autoFocus>
            <option value="">Seleccionar...</option>
            {MEDIOS_DE_PAGO.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </Field>

        <p className="text-xs leading-relaxed text-ink-muted">
          El monto es el de la membresía vigente del alumno; la fecha del pago se registra automáticamente con el día de hoy.
        </p>

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={enviando}>
            {enviando ? 'Registrando...' : 'Confirmar'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
