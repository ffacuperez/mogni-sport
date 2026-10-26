import { useState } from 'react';
import { Info } from 'lucide-react';

import { describirMembresia, previsualizarCambioMembresia } from '../services/alumnosService';
import CambioMembresiaModal from './CambioMembresiaModal';
import { Field, FormActions, PageHeader, Panel, ReadOnly } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

/**
 * Cambio de membresía (reglas M1 a M9 del rediseño de cuotas). Antes de guardar pide al backend la vista previa: si no
 * se permite muestra el motivo; si se permite, abre la confirmación con lo que cambia, cuánto cobrar y el medio de pago.
 * onSave recibe { membresiaId, medioDePago } y hace el cambio.
 */
export default function AlumnoMembresiaForm({ alumno, membresias, onSave, onCancel, onError }) {
  const [membresiaId, setMembresiaId] = useState(alumno.membresiaId ? String(alumno.membresiaId) : '');
  const [consultando, setConsultando] = useState(false);
  // Vista previa del backend: mientras está, se muestra el modal de confirmación
  const [vista, setVista] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!membresiaId) {
      onError('Seleccioná la nueva membresía.');
      return;
    }
    // El backend solo crea una inscripción nueva si la membresía cambia
    if (String(alumno.membresiaId ?? '') === membresiaId) {
      onError('Seleccioná una membresía distinta a la actual.');
      return;
    }
    setConsultando(true);
    try {
      const previa = await previsualizarCambioMembresia(alumno.id, membresiaId);
      if (!previa.permitido) {
        onError(previa.motivo);
        return;
      }
      setVista(previa);
    } catch (error) {
      onError(error.message);
    } finally {
      setConsultando(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl animate-fade-in">
      <PageHeader
        eyebrow={`Alumno · DNI ${alumno.dni}`}
        title="Modificación de membresía"
        subtitle={`${alumno.nombre} ${alumno.apellido}`}
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8">
        <div className="mb-6 grid grid-cols-1 gap-6 md:grid-cols-2">
          <Field label="Membresía actual">
            <ReadOnly>{describirMembresia(alumno)}</ReadOnly>
          </Field>

          <Field label="Nueva membresía" htmlFor="nueva-membresia">
            <select id="nueva-membresia" value={membresiaId} onChange={(e) => setMembresiaId(e.target.value)} className={inputClass}>
              <option value="" disabled>Seleccioná una membresía</option>
              {membresias.map(m => (
                <option key={m.id} value={m.id}>{m.nombre}</option>
              ))}
            </select>
          </Field>
        </div>

        <div className="flex gap-3 rounded-lg border border-line bg-row-hover p-4 text-sm text-ink-soft">
          <Info className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <p>
            El cambio aplica desde hoy hasta el mismo vencimiento de la cuota actual, que queda como se pagó. Si la
            membresía nueva es más cara se cobra la diferencia completa; si es igual o más barata no se cobra nada (sin
            reembolso). No se puede cambiar con la cuota vencida o en mora, ni si ya pagó el mes siguiente. Para dejar al
            alumno sin membresía, dalo de baja.{' '}
            <strong className="font-semibold text-ink">
              Para cambiarla desde la próxima cuota, registrá el pago desde Cuotas eligiendo la membresía nueva.
            </strong>
          </p>
        </div>

        <FormActions onCancel={onCancel} enviando={consultando} />
      </Panel>

      {vista && (
        <CambioMembresiaModal
          alumno={alumno}
          vista={vista}
          onConfirm={(medioDePago) => onSave({ membresiaId, medioDePago })}
          onCancel={() => setVista(null)}
          onError={onError}
        />
      )}
    </div>
  );
}
