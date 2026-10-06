import { useState } from 'react';
import { Info } from 'lucide-react';

import { describirMembresia } from '../services/alumnosService';
import { Field, FormActions, PageHeader, Panel, ReadOnly } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

export default function AlumnoMembresiaForm({ alumno, membresias, onSave, onCancel, onError }) {
  const [membresiaId, setMembresiaId] = useState(alumno.membresiaId ? String(alumno.membresiaId) : '');

  const handleSubmit = (e) => {
    e.preventDefault();
    // El backend solo crea una inscripción nueva si la membresía cambia
    if (String(alumno.membresiaId ?? '') === membresiaId) {
      onError('Seleccione una membresía distinta a la actual.');
      return;
    }
    onSave({ ...alumno, membresiaId });
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
              <option value="">Sin membresía</option>
              {membresias.map(m => (
                <option key={m.id} value={m.id}>{m.nombre}</option>
              ))}
            </select>
          </Field>
        </div>

        <div className="flex gap-3 rounded-lg border border-line bg-row-hover p-4 text-sm text-ink-soft">
          <Info className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <p>
            Al cambiar la membresía se cancela la inscripción activa y se crea una nueva. Solo se permite si la
            cuota actual está pagada; los días que le queden a esa cuota se respetan.
            Elegir "Sin membresía" solo cancela la inscripción activa.
          </p>
        </div>

        <FormActions onCancel={onCancel} />
      </Panel>
    </div>
  );
}
