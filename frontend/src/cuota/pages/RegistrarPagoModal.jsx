import { useState } from 'react';

import { formatearMonto, formatearPeriodo, MEDIOS_DE_PAGO } from '../services/cuotasService';

const readonlyClass = "w-full bg-slate-900/60 text-slate-300 px-3 py-2 rounded-md border border-slate-700";
const inputClass = "w-full bg-slate-100 text-slate-900 px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 border border-transparent";
const labelClass = "block text-sm font-semibold text-slate-200 mb-1";

export default function RegistrarPagoModal({ cuota, onConfirm, onCancel, onError }) {
  const [medioDePago, setMedioDePago] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (!cuota) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!medioDePago) {
      onError('Seleccione un medio de pago.');
      return;
    }
    setEnviando(true);
    try {
      // El monto no lo ingresa el propietario: siempre es el de la cuota (ver cuotasService.registrarPago).
      await onConfirm({ cuotaId: cuota.id, medioDePago });
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-slate-800 border border-slate-700 rounded-lg p-6 max-w-sm w-full shadow-2xl animate-in zoom-in-95 duration-200">
        <h3 className="text-xl font-bold text-white mb-1">Registrar pago de cuota</h3>
        <p className="text-slate-400 text-sm mb-5">{cuota.nombre} {cuota.apellido} · DNI {cuota.dni}</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelClass}>Cuota</label>
              <p className={readonlyClass}>{formatearPeriodo(cuota.periodo)}</p>
            </div>
            <div>
              <label className={labelClass}>Monto a pagar</label>
              <p className={readonlyClass}>{formatearMonto(cuota.monto)}</p>
            </div>
          </div>

          <div>
            <label className={labelClass}>Medio de pago</label>
            <select value={medioDePago} onChange={(e) => setMedioDePago(e.target.value)} className={inputClass}>
              <option value="">Seleccionar...</option>
              {MEDIOS_DE_PAGO.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </div>

          <p className="text-xs text-slate-400">El monto es el de la membresía vigente del alumno; la fecha del pago se registra automáticamente con el día de hoy.</p>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onCancel} disabled={enviando} className="px-4 py-2 rounded bg-slate-700 text-white hover:bg-slate-600 transition-colors disabled:opacity-50">
              Cancelar
            </button>
            <button type="submit" disabled={enviando} className="px-4 py-2 rounded bg-[#1b5e3a] text-white hover:bg-[#14492d] transition-colors disabled:opacity-50">
              {enviando ? 'Registrando...' : 'Confirmar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
