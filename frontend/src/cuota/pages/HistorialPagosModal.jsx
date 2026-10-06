import { formatearFecha, formatearMonto, formatearPeriodo, labelMedioDePago } from '../services/cuotasService';

export default function HistorialPagosModal({ alumno, pagos, cargando, onClose }) {
  if (!alumno) return null;

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-slate-800 border border-slate-700 rounded-lg p-6 max-w-lg w-full shadow-2xl animate-in zoom-in-95 duration-200">
        <h3 className="text-xl font-bold text-white mb-1">Historial de pagos</h3>
        <p className="text-slate-400 text-sm mb-5">{alumno.nombre} {alumno.apellido} · DNI {alumno.dni}</p>

        {cargando ? (
          <p className="text-slate-400 text-sm text-center py-6">Cargando historial...</p>
        ) : pagos.length === 0 ? (
          <p className="text-slate-400 text-sm bg-slate-900/60 border border-slate-700 rounded-md px-4 py-6 text-center">
            Este alumno no tiene pagos registrados.
          </p>
        ) : (
          <div className="max-h-80 overflow-y-auto rounded-md border border-slate-700">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/90 text-slate-300 sticky top-0">
                <tr>
                  <th className="px-4 py-2 font-semibold">Fecha</th>
                  <th className="px-4 py-2 font-semibold">Periodo</th>
                  <th className="px-4 py-2 font-semibold">Monto</th>
                  <th className="px-4 py-2 font-semibold">Medio de pago</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {pagos.map((pago) => (
                  <tr key={pago.id} className="bg-slate-900/40">
                    <td className="px-4 py-3 text-slate-200 whitespace-nowrap">{formatearFecha(pago.fecha)}</td>
                    <td className="px-4 py-3 text-slate-200 whitespace-nowrap">{formatearPeriodo(pago.periodo)}</td>
                    <td className="px-4 py-3 font-semibold text-emerald-400 whitespace-nowrap">{formatearMonto(pago.monto)}</td>
                    <td className="px-4 py-3 text-slate-300 whitespace-nowrap">{labelMedioDePago(pago.medioDePago)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
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
