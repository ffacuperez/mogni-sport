import { useState } from 'react';

const DIAS_MIN = 1;
const DIAS_MAX = 5;

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

  const inputClass = "w-full bg-slate-100 text-slate-900 px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 border border-transparent disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-slate-300";
  const labelClass = "block text-sm font-semibold text-slate-200 mb-1";

  return (
    <div className="animate-in fade-in zoom-in-95 duration-300 max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold text-white mb-8 tracking-tight">
        {isEdit ? 'Modificación de Membresía' : 'Alta de Nueva Membresía'}
      </h1>

      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 md:p-8 shadow-xl">
        
        <div className="grid grid-cols-1 gap-6 mb-6">
          <div>
            <label className={labelClass}>Código ID</label>
            <input type="text" name="codigo" value={formData.codigo ?? 'Se asigna al guardar'} disabled className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>Nombre de la membresía</label>
            <input type="text" name="nombre" value={formData.nombre} onChange={handleChange} required className={inputClass} />
          </div>
          
          <div>
            <label className={labelClass}>Días por semana</label>
            <select name="dias" value={formData.dias} onChange={handleChange} required className={inputClass}>
              <option value="">Seleccionar...</option>
              {Array.from({ length: DIAS_MAX - DIAS_MIN + 1 }, (_, i) => DIAS_MIN + i).map((n) => (
                <option key={n} value={n}>{n} día{n > 1 ? 's' : ''} por semana</option>
              ))}
            </select>
          </div>

          <div>
            <label className={labelClass}>Precio en ARS (ej: 25000)</label>
            <input type="text" inputMode="decimal" name="precio" value={formData.precio} onChange={handleChange} required placeholder="Mayor a 0" className={inputClass} />
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
