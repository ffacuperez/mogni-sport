import { useState } from 'react';

import { Field, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

// B-2: las membresías son de 2 a 6 días por semana (lo mismo valida el backend)
const DIAS_MIN = 2;
const DIAS_MAX = 6;

export default function MembresiaForm({ membresiaToEdit, onSave, onCancel, onError }) {
  const isEdit = !!membresiaToEdit;
  
  const [formData, setFormData] = useState(
    membresiaToEdit || {
      nombre: '',
      dias: '',
      precio: ''
    }
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'dias' && value !== '' && !/^\d+$/.test(value)) {
      return;
    }
    // Precio: enteros o hasta 2 decimales, con punto o coma
    if (name === 'precio' && value !== '' && !/^\d+([.,]\d{0,2})?$/.test(value)) {
      return;
    }
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.nombre.trim()) {
      onError('El nombre de la membresía es obligatorio.');
      return;
    }

    const dias = Number(formData.dias);
    if (!formData.dias || dias < DIAS_MIN || dias > DIAS_MAX) {
      onError(`Los días por semana deben estar entre ${DIAS_MIN} y ${DIAS_MAX}.`);
      return;
    }

    const precio = Number(String(formData.precio).replace(',', '.'));
    if (!formData.precio || !(precio > 0)) {
      onError('El precio debe ser mayor a 0.');
      return;
    }

    onSave(formData, isEdit);
  };

  return (
    <div className="mx-auto max-w-2xl animate-fade-in">
      <PageHeader
        eyebrow="Gestión · Membresías"
        title={isEdit ? 'Modificación de membresía' : 'Alta de nueva membresía'}
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <div className="grid grid-cols-1 gap-6">
          <Field label="Nombre de la membresía" htmlFor="membresia-nombre">
            <input id="membresia-nombre" type="text" name="nombre" value={formData.nombre} onChange={handleChange} required className={inputClass} />
          </Field>

          <Field label="Días por semana" htmlFor="membresia-dias">
            <select id="membresia-dias" name="dias" value={formData.dias} onChange={handleChange} required className={inputClass}>
              <option value="">Seleccionar...</option>
              {Array.from({ length: DIAS_MAX - DIAS_MIN + 1 }, (_, i) => DIAS_MIN + i).map((n) => (
                <option key={n} value={n}>{n} día{n > 1 ? 's' : ''} por semana</option>
              ))}
            </select>
          </Field>

          <Field label="Precio en ARS (ej: 25000)" htmlFor="membresia-precio">
            <div className="relative">
              <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-ink-muted">$</span>
              <input id="membresia-precio" type="text" inputMode="decimal" name="precio" value={formData.precio} onChange={handleChange} required placeholder="Mayor a 0" className={`${inputClass} pl-7 tabular-nums`} />
            </div>
          </Field>
        </div>

        <FormActions onCancel={onCancel} />
      </Panel>
    </div>
  );
}
