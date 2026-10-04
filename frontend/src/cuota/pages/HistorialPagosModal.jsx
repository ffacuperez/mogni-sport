import { formatearFecha, formatearMonto } from '../services/cuotasService';

export default function HistorialPagosModal({ alumno, pagos, onClose }) {
  if (!alumno) return null;

  const ordenados = [...(pagos ?? [])].sort((a, b) => b.fecha.localeCompare(a.fecha));

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-slate-800 border border-slate-700 rounded-lg p-6 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-200">
        <h3 className="text-xl font-bold text-white mb-1">Historial de pagos</h3>
        <p className="text-slate-400 text-sm mb-5">{alumno.nombre} {alumno.apellido} · DNI {alumno.dni}</p>

        {ordenados.length === 0 ? (
          <p className="text-slate-400 text-sm bg-slate-900/60 border border-slate-700 rounded-md px-4 py-6 text-center">
            Este alumno todavía no tiene pagos registrados.
          </p>
        ) : (
          <div className="max-h-80 overflow-y-auto rounded-md border border-slate-700 divide-y divide-slate-700">
            {ordenados.map((pago, i) => (
              <div key={i} className="px-4 py-3 bg-slate-900/40 flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-white">{formatearFecha(pago.fecha)}</p>
                  <p className="text-xs text-slate-400">{pago.medioPago}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-emerald-400">{formatearMonto(pago.monto)}</p>
                  {pago.recargo && (
                    <span className="text-[10px] font-semibold text-red-400">Con recargo</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end mt-6">
          <button onClick={onClose} className="px-4 py-2 rounded bg-slate-700 text-white hover:bg-slate-600 transition-colors">
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
