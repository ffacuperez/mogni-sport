export default function MembresiasList({ membresias, selectedId, setSelectedId, onAdd, onEdit, onDelete }) {
  const selectedMembresia = membresias.find(m => m.id === selectedId);

  return (
    <div className="animate-in fade-in duration-300">
      <h1 className="text-3xl font-bold text-white mb-8 tracking-tight">Membresías (Vista de Lista)</h1>
      
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-xl">
        <div className="flex flex-wrap gap-2 mb-6">
          <button onClick={onAdd} className="flex items-center gap-2 bg-[#1b5e3a] hover:bg-[#14492d] text-white px-4 py-2 rounded-md font-medium transition-colors">
            <span className="text-xl leading-none">+</span> Agregar Nueva Membresía
          </button>
          
          {selectedMembresia && (
            <button onClick={onEdit} className="flex items-center gap-2 bg-[#1b5e3a] hover:bg-[#14492d] text-white px-4 py-2 rounded-md font-medium transition-colors">
              Modificar Membresía
            </button>
          )}

          {selectedMembresia && (
            <button onClick={onDelete} className="flex items-center gap-2 bg-red-800 hover:bg-red-900 text-white px-4 py-2 rounded-md font-medium transition-colors">
              Eliminar Membresía
            </button>
          )}
        </div>

        <div className="overflow-x-auto rounded-lg border border-red-950/30">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-slate-800/50 text-slate-300">
              <tr>
                <th className="px-4 py-3 w-12 text-center"></th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-l-md mx-1">Código</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Nombre</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Días por semana</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-r-md mx-1">Precio (ARS)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-red-950/30">
              {membresias.map((membresia) => (
                <tr 
                  key={membresia.id} 
                  className={`hover:bg-slate-800/40 transition-colors ${selectedId === membresia.id ? 'bg-red-950/20' : ''}`}
                  onClick={() => setSelectedId(selectedId === membresia.id ? null : membresia.id)}
                >
                  <td className="px-4 py-3 text-center">
                    <input 
                      type="checkbox" 
                      checked={selectedId === membresia.id}
                      onChange={() => {}} 
                      className="w-4 h-4 rounded border-slate-600 text-red-600 bg-slate-800 focus:ring-red-600 focus:ring-offset-slate-900 cursor-pointer accent-red-600"
                    />
                  </td>
                  <td className="px-4 py-3 text-slate-300">{membresia.codigo}</td>
                  <td className="px-4 py-3">{membresia.nombre}</td>
                  <td className="px-4 py-3">{membresia.dias === '' ? '—' : `${membresia.dias} días`}</td>
                  <td className="px-4 py-3">${Number(membresia.precio).toLocaleString('es-AR')}</td>
                </tr>
              ))}
              {membresias.length === 0 && (
                <tr>
                  <td colSpan="5" className="px-4 py-8 text-center text-slate-500">
                    No hay membresías registradas.
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
