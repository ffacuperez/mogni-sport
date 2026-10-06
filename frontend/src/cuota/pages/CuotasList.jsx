import { clasesEstadoCuota, formatearFecha, formatearMonto } from '../services/cuotasService';

const IconSearch = () => <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>;

const FILTROS = ['Todas', 'Próxima a vencer', 'Vencida'];

export default function CuotasList({
  cuotas,
  cargando,
  selectedId,
  setSelectedId,
  onRegistrarPago,
  onVerHistorial,
  searchTerm,
  setSearchTerm,
  filtro,
  setFiltro,
}) {
  // El backend ya devuelve solo cuotas de alumnos activos con inscripción activa.
  const filas = cuotas
    .filter((cuota) => {
      const term = searchTerm.trim().toLowerCase();
      if (!term) return true;
      return (
        cuota.dni.includes(term) ||
        cuota.nombre.toLowerCase().includes(term) ||
        cuota.apellido.toLowerCase().includes(term)
      );
    })
    .filter((cuota) => filtro === 'Todas' || cuota.estado === filtro);

  const seleccion = filas.find((c) => c.id === selectedId);

  return (
    <div className="animate-in fade-in duration-300">
      <h1 className="text-3xl font-bold text-white mb-8 tracking-tight">Cuotas</h1>

      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => onRegistrarPago(seleccion)}
              disabled={!seleccion?.pagable}
              className="flex items-center gap-2 bg-[#1b5e3a] hover:bg-[#14492d] text-white px-4 py-2 rounded-md font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#1b5e3a]"
            >
              Registrar Pago
            </button>
            <button
              onClick={() => onVerHistorial(seleccion)}
              disabled={!seleccion}
              className="flex items-center gap-2 bg-slate-700 hover:bg-slate-600 text-white px-4 py-2 rounded-md font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-slate-700"
            >
              Ver Historial
            </button>
            {!seleccion && (
              <p className="text-sm text-slate-500">Seleccioná una cuota para registrar el pago o ver el historial.</p>
            )}
            {seleccion && !seleccion.pagable && (
              <p className="text-sm text-slate-500">La cuota seleccionada ya está pagada.</p>
            )}
          </div>

          <div className="relative w-full sm:w-80">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
              <IconSearch />
            </div>
            <input
              type="text"
              placeholder="Buscar por nombre o DNI"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-white text-slate-900 pl-10 pr-4 py-2 rounded-md border border-slate-300 focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>
        </div>

        <div className="flex flex-wrap gap-2 mb-6">
          {FILTROS.map((f) => (
            <button
              key={f}
              onClick={() => setFiltro(f)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                filtro === f
                  ? 'bg-red-800 text-white'
                  : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200'
              }`}
            >
              {f}
            </button>
          ))}
        </div>

        <div className="overflow-x-auto rounded-lg border border-red-950/30">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-slate-800/50 text-slate-300">
              <tr>
                <th className="px-4 py-3 w-12 text-center"></th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-l-md mx-1">Alumno</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">DNI</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Membresía</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Monto</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Vencimiento</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-r-md mx-1">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-red-950/30">
              {filas.map((cuota) => (
                <tr
                  key={cuota.id}
                  className={`hover:bg-slate-800/40 transition-colors cursor-pointer ${selectedId === cuota.id ? 'bg-red-950/20' : ''}`}
                  onClick={() => setSelectedId(selectedId === cuota.id ? null : cuota.id)}
                >
                  <td className="px-4 py-3 text-center">
                    <input
                      type="checkbox"
                      checked={selectedId === cuota.id}
                      onChange={() => {}}
                      className="w-4 h-4 rounded border-slate-600 text-red-600 bg-slate-800 focus:ring-red-600 focus:ring-offset-slate-900 cursor-pointer accent-red-600"
                    />
                  </td>
                  <td className="px-4 py-3">{cuota.apellido}, {cuota.nombre}</td>
                  <td className="px-4 py-3 text-slate-300">{cuota.dni}</td>
                  <td className="px-4 py-3">{cuota.membresia}</td>
                  <td className="px-4 py-3">{formatearMonto(cuota.monto)}</td>
                  <td className="px-4 py-3">{formatearFecha(cuota.vencimiento)}</td>
                  <td className="px-4 py-3">
                    <span className={`px-3 py-1 rounded-full text-xs font-semibold ${clasesEstadoCuota(cuota.estado)}`}>
                      {cuota.estado}
                    </span>
                  </td>
                </tr>
              ))}
              {filas.length === 0 && (
                <tr>
                  <td colSpan="7" className="px-4 py-8 text-center text-slate-500">
                    {cargando ? 'Cargando cuotas...' : 'No hay cuotas que coincidan con la búsqueda.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
