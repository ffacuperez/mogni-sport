import { useState } from 'react';

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
      membresiaId: '',
      objetivo: '',
      antecedentes: ''
    }
  );

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
        || !formData.fechaNacimiento || !formData.telefono) {
      onError('Nombre, apellido, DNI, email, fecha de nacimiento y teléfono son obligatorios.');
      return;
    }

    if (!EMAIL_REGEX.test(formData.email.trim())) {
      onError('El email no tiene un formato válido.');
      return;
    }

    // Al inscribir siempre se crea una inscripción, así que la membresía es obligatoria
    if (!isEdit && !formData.membresiaId) {
      onError('Debe seleccionar una membresía para inscribir al alumno.');
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

  const inputClass = "w-full bg-slate-100 text-slate-900 px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 border border-transparent disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-slate-300";
  const labelClass = "block text-sm font-semibold text-slate-200 mb-1";

  return (
    <div className="animate-in fade-in zoom-in-95 duration-300 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-white mb-8 tracking-tight">
        {isEdit ? 'Modificación de Alumno' : 'Alta de Nuevo Alumno'}
      </h1>

      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 md:p-8 shadow-xl">
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          {/* Al editar, la membresía se cambia desde su propio formulario (AlumnoMembresiaForm) */}
          {!isEdit && (
            <div>
              <label className={labelClass}>Membresía</label>
              <select name="membresiaId" value={formData.membresiaId} onChange={handleChange} required className={inputClass}>
                <option value="" disabled>Seleccione una membresía</option>
                {membresias.map(m => (
                  <option key={m.id} value={m.id}>{m.nombre}</option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className={labelClass}>Nombre</label>
            <input type="text" name="nombre" value={formData.nombre} onChange={handleChange} required className={inputClass} />
          </div>
          
          <div>
            <label className={labelClass}>Apellido</label>
            <input type="text" name="apellido" value={formData.apellido} onChange={handleChange} required className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>DNI</label>
            <input type="text" name="dni" value={formData.dni} onChange={handleChange} required className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Correo electrónico</label>
            <input type="email" name="email" value={formData.email} onChange={handleChange} required className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Fecha de nacimiento</label>
            <input type="date" name="fechaNacimiento" value={formData.fechaNacimiento} onChange={handleChange} required className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Teléfono (celular)</label>
            <input type="text" name="telefono" value={formData.telefono} onChange={handleChange} required className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Sexo</label>
            <select name="sexo" value={formData.sexo} onChange={handleChange} className={inputClass}>
              <option value="">Sin especificar</option>
              <option value="Femenino">Femenino</option>
              <option value="Masculino">Masculino</option>
              <option value="Otro">Otro</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <label className={labelClass}>Objetivo del alumno</label>
            <textarea name="objetivo" value={formData.objetivo} onChange={handleChange} rows="2" className={inputClass} />
          </div>

          <div className="md:col-span-2">
            <label className={labelClass}>Antecedentes de salud</label>
            <textarea name="antecedentes" value={formData.antecedentes} onChange={handleChange} rows="2" className={inputClass} />
          </div>
        </div>

        <div className="flex justify-center gap-4 mt-8 pt-6 border-t border-slate-800">
          <button type="submit" className="bg-[#1b5e3a] hover:bg-[#14492d] text-white px-8 py-2 rounded-md font-medium transition-colors shadow-lg">
            Confirmar
          </button>
          <button type="button" onClick={onCancel} className="bg-red-800 hover:bg-red-900 text-white px-8 py-2 rounded-md font-medium transition-colors shadow-lg">
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}
