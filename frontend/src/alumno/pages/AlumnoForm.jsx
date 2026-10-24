import { useState } from 'react';

import { Field, FormActions, PageHeader, Panel, ReadOnly } from '../../shared/components/ui';
import { inputClass } from '../../shared/components/clases';
import { formatearMonto } from '../../shared/services/texto';
import { MEDIOS_DE_PAGO } from '../../cuota/services/cuotasService';
import { MODALIDADES } from '../services/alumnosService';

// Mismo criterio que el backend: algo@dominio.ext, sin espacios
const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export default function AlumnoForm({ alumnoToEdit, membresias, onSave, onCancel, onError }) {
  const isEdit = !!alumnoToEdit;
  
  const [formData, setFormData] = useState(
    alumnoToEdit || {
      nombre: '',
      apellido: '',
      dni: '',
      email: '',
      fechaNacimiento: '',
      telefono: '',
      sexo: '',
      modalidad: '',
      membresiaId: '',
      medioDePago: '',
      objetivo: '',
      antecedentes: ''
    }
  );
  // MOGNI-73: el monto de la primera cuota es el precio vigente de la membresía elegida (solo para ver)
  const membresiaElegida = membresias.find(m => String(m.id) === String(formData.membresiaId));

  const handleChange = (e) => {
    const { name, value } = e.target;
    if ((name === 'dni' || name === 'telefono') && value !== '' && !/^\d+$/.test(value)) {
      return;
    }
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.nombre.trim() || !formData.apellido.trim() || !formData.dni || !formData.email
        || !formData.fechaNacimiento || !formData.telefono || !formData.sexo) {
      onError('Nombre, apellido, DNI, email, fecha de nacimiento, teléfono y sexo son obligatorios.');
      return;
    }

    if (!EMAIL_REGEX.test(formData.email.trim())) {
      onError('El email no tiene un formato válido.');
      return;
    }

    // HU-253: obligatoria al registrar y al modificar
    if (!formData.modalidad) {
      onError('Debe seleccionar la modalidad del alumno.');
      return;
    }

    // Al inscribir siempre se crea una inscripción, así que la membresía es obligatoria
    if (!isEdit && !formData.membresiaId) {
      onError('Debe seleccionar una membresía para inscribir al alumno.');
      return;
    }

    // MOGNI-73: el alta siempre se hace con la primera cuota pagada
    if (!isEdit && !formData.medioDePago) {
      onError('Debe seleccionar el medio de pago de la primera cuota.');
      return;
    }

    if (formData.fechaNacimiento) {
      const [year, month, day] = formData.fechaNacimiento.split('-');
      const dob = new Date(year, month - 1, day);
      const today = new Date();
      let age = today.getFullYear() - dob.getFullYear();
      const monthDiff = today.getMonth() - dob.getMonth();
      
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
        age--;
      }
      
      if (age < 16) {
        onError(isEdit ? 'El alumno debe tener al menos 16 años.' : 'El alumno debe tener al menos 16 años para registrarse.');
        return;
      }
    }

    onSave(formData, isEdit);
  };

  // id/htmlFor para que cada label quede asociada a su campo
  const campo = (name, label, input, className) => (
    <Field label={label} htmlFor={`alumno-${name}`} className={className}>{input(`alumno-${name}`)}</Field>
  );

  return (
    <div className="mx-auto max-w-4xl animate-fade-in">
      <PageHeader
        eyebrow={isEdit ? `Alumno · DNI ${alumnoToEdit.dni}` : 'Gestión · Alumnos'}
        title={isEdit ? 'Modificación de alumno' : 'Alta de nuevo alumno'}
        subtitle={isEdit ? undefined : 'La contraseña inicial del alumno será su DNI; deberá cambiarla en su primer ingreso.'}
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        {/* MOGNI-73: el alta incluye el pago de la primera cuota. Al editar, la membresía se cambia desde su propio formulario (AlumnoMembresiaForm) */}
        {!isEdit && (
          <fieldset className="mb-8 rounded-lg border border-line p-4 md:p-5">
            <legend className="px-2 text-sm font-semibold text-ink">Cuota</legend>
            <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
              {campo('membresiaId', 'Membresía', (id) => (
                <select id={id} name="membresiaId" value={formData.membresiaId} onChange={handleChange} required className={inputClass}>
                  <option value="" disabled>Seleccione una membresía</option>
                  {membresias.map(m => (
                    <option key={m.id} value={m.id}>{m.nombre}</option>
                  ))}
                </select>
              ))}
              <Field label="Monto de la primera cuota">
                <ReadOnly numerico>{membresiaElegida ? formatearMonto(membresiaElegida.precio) : '—'}</ReadOnly>
              </Field>
              {campo('medioDePago', 'Medio de pago', (id) => (
                <select id={id} name="medioDePago" value={formData.medioDePago} onChange={handleChange} required className={inputClass}>
                  <option value="" disabled>Seleccione un medio de pago</option>
                  {MEDIOS_DE_PAGO.map(m => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </select>
              ))}
            </div>
            <p className="mt-3 text-xs text-ink-muted">
              La primera cuota cubre 30 días desde hoy y el pago se registra con la fecha de hoy.
            </p>
          </fieldset>
        )}

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {campo('nombre', 'Nombre', (id) => <input id={id} type="text" name="nombre" value={formData.nombre} onChange={handleChange} required className={inputClass} />)}
          {campo('apellido', 'Apellido', (id) => <input id={id} type="text" name="apellido" value={formData.apellido} onChange={handleChange} required className={inputClass} />)}
          {campo('dni', 'DNI', (id) => <input id={id} type="text" inputMode="numeric" name="dni" value={formData.dni} onChange={handleChange} required className={`${inputClass} tabular-nums`} />)}
          {campo('email', 'Correo electrónico', (id) => <input id={id} type="email" name="email" value={formData.email} onChange={handleChange} required className={inputClass} />)}
          {campo('fechaNacimiento', 'Fecha de nacimiento', (id) => <input id={id} type="date" name="fechaNacimiento" value={formData.fechaNacimiento} onChange={handleChange} required className={`${inputClass} `} />)}
          {campo('telefono', 'Teléfono (celular)', (id) => <input id={id} type="tel" inputMode="numeric" name="telefono" value={formData.telefono} onChange={handleChange} required className={`${inputClass} tabular-nums`} />)}
          {campo('sexo', 'Sexo', (id) => (
            <select id={id} name="sexo" value={formData.sexo} onChange={handleChange} required className={inputClass}>
              <option value="" disabled>Seleccione el sexo</option>
              <option value="Femenino">Femenino</option>
              <option value="Masculino">Masculino</option>
              <option value="Otro">Otro</option>
            </select>
          ))}
          {campo('modalidad', 'Modalidad', (id) => (
            <select id={id} name="modalidad" value={formData.modalidad} onChange={handleChange} required className={inputClass}>
              <option value="" disabled>Seleccione una modalidad</option>
              {Object.entries(MODALIDADES).map(([valor, texto]) => (
                <option key={valor} value={valor}>{texto}</option>
              ))}
            </select>
          ))}
          {campo('objetivo', 'Objetivo del alumno', (id) => <textarea id={id} name="objetivo" value={formData.objetivo} onChange={handleChange} rows="2" className={inputClass} />, 'md:col-span-2')}
          {campo('antecedentes', 'Antecedentes de salud', (id) => <textarea id={id} name="antecedentes" value={formData.antecedentes} onChange={handleChange} rows="2" className={inputClass} />, 'md:col-span-2')}
        </div>

        <FormActions onCancel={onCancel} />
      </Panel>
    </div>
  );
}
