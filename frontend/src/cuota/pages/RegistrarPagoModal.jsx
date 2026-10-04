import { useState } from 'react';

const inputClass = "w-full bg-slate-100 text-slate-900 px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 border border-transparent";
const labelClass = "block text-sm font-semibold text-slate-200 mb-1";

export default function RegistrarPagoModal({ alumno, montoSugerido, onConfirm, onCancel, onError }) {
  const [monto, setMonto] = useState(montoSugerido ? String(montoSugerido) : '');
  const [medioPago, setMedioPago] = useState('');
  const [fecha, setFecha] = useState(new Date().toISOString().split('T')[0]);

  if (!alumno) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!monto || Number(monto) <= 0) {
      onError('El monto debe ser numérico y mayor a 0.');
      return;
    }
    if (!medioPago) {
      onError('Seleccione un medio de pago.');
      return;
    }
    if (!fecha) {
      onError('Seleccione la fecha de pago.');
      return;
    }
    onConfirm({ monto: Number(monto), medioPago, fecha });
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-slate-800 border border-slate-700 rounded-lg p-6 max-w-sm w-full shadow-2xl animate-in zoom-in-95 duration-200">
        <h3 className="text-xl font-bold text-white mb-1">Registrar pago de cuota</h3>
        <p className="text-slate-400 text-sm mb-5">{alumno.nombre} {alumno.apellido} · DNI {alumno.dni}</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className={labelClass}>Monto (ARS)</label>
            <input
              type="text"
              value={monto}
              onChange={(e) => { if (e.target.value === '' || /^\d+$/.test(e.target.value)) setMonto(e.target.value); }}
              placeholder="ej: 25000"
              className={inputClass}
            />
          </div>

          <div>
            <label className={labelClass}>Medio de pago</label>
            <select value={medioPago} onChange={(e) => setMedioPago(e.target.value)} className={inputClass}>
              <option value="">Seleccionar...</option>
              <option value="Efectivo">Efectivo</option>
              <option value="Transferencia">Transferencia</option>
            </select>
          </div>

          <div>
            <label className={labelClass}>Fecha de pago</label>
            <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} className={inputClass} />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onCancel} className="px-4 py-2 rounded bg-slate-700 text-white hover:bg-slate-600 transition-colors">
              Cancelar
            </button>
            <button type="submit" className="px-4 py-2 rounded bg-[#1b5e3a] text-white hover:bg-[#14492d] transition-colors">
              Confirmar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
