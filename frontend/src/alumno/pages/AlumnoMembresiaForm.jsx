import { useState } from 'react';
import { describirMembresia } from '../services/alumnosService';

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

  const inputClass = "w-full bg-slate-100 text-slate-900 px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 border border-transparent";
  const labelClass = "block text-sm font-semibold text-slate-200 mb-1";

  return (
    <div className="animate-in fade-in zoom-in-95 duration-300 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-white mb-2 tracking-tight">Modificación de Membresía</h1>
      <p className="text-slate-400 mb-8">{alumno.nombre} {alumno.apellido} · DNI {alumno.dni}</p>

      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 md:p-8 shadow-xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <div>
            <span className={labelClass}>Membresía actual</span>
            <p className="text-slate-100 py-2">{describirMembresia(alumno)}</p>
          </div>

          <div>
            <label className={labelClass}>Nueva membresía</label>
            <select value={membresiaId} onChange={(e) => setMembresiaId(e.target.value)} className={inputClass}>
              <option value="">Sin membresía</option>
              {membresias.map(m => (
                <option key={m.id} value={m.id}>{m.nombre}</option>
              ))}
            </select>
          </div>
        </div>

        <p className="text-sm text-slate-400">
          Al cambiar la membresía se cancela la inscripción activa y se crea una nueva desde hoy por 30 días.
          Elegir "Sin membresía" solo cancela la inscripción activa.
        </p>

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
