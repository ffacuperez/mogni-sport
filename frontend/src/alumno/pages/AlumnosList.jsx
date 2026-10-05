const IconSearch = () => <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>;

export default function AlumnosList({ alumnos, selectedId, setSelectedId, onAdd, onEdit, onToggleStatus, searchDNI, setSearchDNI }) {
  const filteredAlumnos = alumnos.filter(a => a.dni.includes(searchDNI));
  const selectedAlumno = alumnos.find(a => a.id === selectedId);

  return (
    <div className="animate-in fade-in duration-300">
      <h1 className="text-3xl font-bold text-white mb-8 tracking-tight">Alumnos (Vista de Lista)</h1>
      
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <div className="flex flex-wrap gap-2">
            <button onClick={onAdd} className="flex items-center gap-2 bg-[#1b5e3a] hover:bg-[#14492d] text-white px-4 py-2 rounded-md font-medium transition-colors">
              <span className="text-xl leading-none">+</span> Agregar Nuevo Alumno
            </button>
            
            {selectedAlumno && (
              <>
                <button onClick={onEdit} className="flex items-center gap-2 bg-[#1b5e3a] hover:bg-[#14492d] text-white px-4 py-2 rounded-md font-medium transition-colors">
                  Modificar Alumno
                </button>
                <button 
                  onClick={onToggleStatus} 
                  className={`flex items-center gap-2 text-white px-4 py-2 rounded-md font-medium transition-colors ${selectedAlumno.estado === 'Activo' ? 'bg-red-800 hover:bg-red-900' : 'bg-[#1b5e3a] hover:bg-[#14492d]'}`}
                >
                  {selectedAlumno.estado === 'Activo' ? 'Dar de baja' : 'Activar alumno'}
                </button>
              </>
            )}
          </div>

          <div className="relative w-full sm:w-80">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
              <IconSearch />
            </div>
            <input 
              type="text" 
              placeholder="Buscar por DNI (ej: 12345678)" 
              value={searchDNI}
              onChange={(e) => setSearchDNI(e.target.value.replace(/\D/g, ''))}
              className="w-full bg-white text-slate-900 pl-10 pr-4 py-2 rounded-md border border-slate-300 focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto rounded-lg border border-red-950/30">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-slate-800/50 text-slate-300">
              <tr>
                <th className="px-4 py-3 w-12 text-center"></th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-l-md mx-1">DNI</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Apellido</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Nombre</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Correo Electrónico</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 mx-1">Membresía</th>
                <th className="px-4 py-3 font-semibold bg-slate-800/80 rounded-r-md mx-1">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-red-950/30">
              {filteredAlumnos.map((alumno) => (
                <tr 
                  key={alumno.id} 
                  className={`hover:bg-slate-800/40 transition-colors ${selectedId === alumno.id ? 'bg-red-950/20' : ''}`}
                  onClick={() => setSelectedId(selectedId === alumno.id ? null : alumno.id)}
                >
                  <td className="px-4 py-3 text-center">
                    <input 
                      type="checkbox" 
                      checked={selectedId === alumno.id}
                      onChange={() => {}} 
                      className="w-4 h-4 rounded border-slate-600 text-red-600 bg-slate-800 focus:ring-red-600 focus:ring-offset-slate-900 cursor-pointer accent-red-600"
                    />
                  </td>
                  <td className="px-4 py-3">{alumno.dni}</td>
                  <td className="px-4 py-3">{alumno.apellido}</td>
                  <td className="px-4 py-3">{alumno.nombre}</td>
                  <td className="px-4 py-3">{alumno.email}</td>
                  <td className="px-4 py-3">{alumno.membresia}</td>
                  <td className="px-4 py-3">
                    <span className={alumno.estado === 'Activo' ? 'text-slate-200' : 'text-slate-500'}>
                      {alumno.estado}
                    </span>
                  </td>
                </tr>
              ))}
              {filteredAlumnos.length === 0 && (
                <tr>
                  <td colSpan="7" className="px-4 py-8 text-center text-slate-500">
                    No se encontraron alumnos con ese DNI.
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
