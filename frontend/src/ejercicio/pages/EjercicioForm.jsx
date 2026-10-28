import { useState } from 'react';
import { Field, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';

const GRUPOS_MUSCULARES = ['Piernas', 'Espalda', 'Pecho', 'Hombros', 'Brazos', 'Core', 'Glúteos'];

export default function EjercicioForm({ ejercicioToEdit, onSave, onCancel, onError }) {
  const isEdit = !!ejercicioToEdit;
  
  const [formData, setFormData] = useState(
    ejercicioToEdit || {
      nombre: '',
      grupoMuscular: '',
      descripcion: '',
      activo: true
    }
  );

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.nombre.trim()) {
      onError('El nombre del ejercicio es obligatorio.');
      return;
    }
    if (!formData.grupoMuscular) {
      onError('Debes seleccionar un grupo muscular.');
      return;
    }

    onSave(formData, isEdit);
  };

  return (
    <div className="mx-auto max-w-2xl animate-fade-in">
      <PageHeader
        eyebrow="Gestión · Ejercicios"
        title={isEdit ? 'Modificación de ejercicio' : 'Alta de nuevo ejercicio'}
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <div className="grid grid-cols-1 gap-6">
          <Field label="Nombre del ejercicio" htmlFor="ejercicio-nombre">
            <input 
              id="ejercicio-nombre" 
              type="text" 
              name="nombre" 
              value={formData.nombre} 
              onChange={handleChange} 
              required 
              className={inputClass} 
              placeholder="Ej: Sentadilla con barra"
            />
          </Field>

          <Field label="Grupo muscular" htmlFor="ejercicio-grupo">
            <select 
              id="ejercicio-grupo" 
              name="grupoMuscular" 
              value={formData.grupoMuscular} 
              onChange={handleChange} 
              required 
              className={inputClass}
            >
              <option value="">Seleccionar grupo...</option>
              {GRUPOS_MUSCULARES.map((grupo) => (
                <option key={grupo} value={grupo}>{grupo}</option>
              ))}
            </select>
          </Field>

          <Field label="Descripción (opcional)" htmlFor="ejercicio-descripcion">
            <textarea 
              id="ejercicio-descripcion" 
              name="descripcion" 
              value={formData.descripcion} 
              onChange={handleChange} 
              rows={3}
              className={inputClass}
              placeholder="Detalles de la técnica o ejecución..."
            />
          </Field>
        </div>

        <FormActions onCancel={onCancel} />
      </Panel>
    </div>
  );
}