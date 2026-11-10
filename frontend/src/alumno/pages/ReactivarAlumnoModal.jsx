import { useState } from 'react';
import { TriangleAlert } from 'lucide-react';

import { formatearFecha, formatearMonto, MEDIOS_DE_PAGO } from '../../cuota/services/cuotasService';
import { Button, Field, Modal, ReadOnly } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

/**
 * MOGNI-246: reactivar a un alumno que ya no tiene días pagos es como un alta. Se elige la membresía y el medio de pago
 * de la primera cuota (un mes desde hoy), sin recargo: la deuda anterior se olvida.
 */
export default function ReactivarAlumnoModal({ alumno, membresias, onConfirm, onCancel, onError }) {
  const [membresiaId, setMembresiaId] = useState(alumno.inscripcion?.membresiaId ? String(alumno.inscripcion.membresiaId) : '');
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);

  const membresia = membresias.find(m => String(m.id) === membresiaId);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!membresiaId || !medioDePago) {
      onError('Seleccioná la membresía y el medio de pago de la primera cuota.');
      return;
    }
    setEnviando(true);
    try {
      await onConfirm({ membresiaId, medioDePago });
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      title="Reactivar alumno"
      subtitle={`${alumno.nombre} ${alumno.apellido} · DNI ${alumno.dni}`}
      onClose={onCancel}
      cerrable={!enviando}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-ink-soft">
          {alumno.vencimientoUltimaCuota
            ? `Su última cuota venció el ${formatearFecha(alumno.vencimientoUltimaCuota)}. `
            : ''}
          Para reactivarlo se registra el pago de una cuota nueva desde hoy, sin recargo.
        </p>

        <Field label="Membresía" htmlFor="reactivar-membresia">
          <select id="reactivar-membresia" value={membresiaId} onChange={(e) => setMembresiaId(e.target.value)} className={inputClass} autoFocus>
            <option value="" disabled>Seleccioná una membresía</option>
            {membresias.map(m => (
              <option key={m.id} value={m.id}>{m.nombre}</option>
            ))}
          </select>
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Monto">
            <ReadOnly numerico>{membresia ? formatearMonto(membresia.precio) : '—'}</ReadOnly>
          </Field>
          <Field label="Medio de pago" htmlFor="reactivar-medio">
            <select id="reactivar-medio" value={medioDePago} onChange={(e) => setMedioDePago(e.target.value)} className={inputClass}>
              <option value="">Seleccionar...</option>
              {MEDIOS_DE_PAGO.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </Field>
        </div>

        <p className="flex items-start gap-2 rounded-lg bg-warning-bg px-3 py-2.5 text-sm text-warning-text">
          <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0" />
          Revisá los datos antes de confirmar: un pago registrado no se puede deshacer.
        </p>

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={enviando}>
            {enviando ? 'Registrando...' : 'Reactivar y registrar pago'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
