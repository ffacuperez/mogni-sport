import React, { useState } from 'react';

export default function EjerciciosList({ ejercicios = [], onEdit, onToggleEstado, onDelete, onAdd }) {
  const [openGroups, setOpenGroups] = useState({ Piernas: true });
  const [ejercicioSeleccionado, setEjercicioSeleccionado] = useState(null);

  const toggleGroup = (grupo) => {
    setOpenGroups(prev => ({
      ...prev,
      [grupo]: !prev[grupo]
    }));
  };

  const gruposMusculares = ['Piernas', 'Espalda', 'Pecho', 'Hombros', 'Brazos', 'Core', 'Glúteos'];
  
  const ejerciciosPorGrupo = gruposMusculares.reduce((acc, grupo) => {
    acc[grupo] = ejercicios.filter(e => e.grupoMuscular === grupo);
    return acc;
  }, {});

  return (
    <div className="p-6 max-w-6xl mx-auto relative">
      {/* Cabecera superior */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <p className="text-sm text-gray-500">Gestión - {ejercicios.length} ejercicios</p>
          <h1 className="text-2xl font-bold text-gray-900">Ejercicios</h1>
        </div>
        <button
          type="button"
          onClick={onAdd}
          className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-medium transition shadow-sm flex items-center gap-2"
        >
          + Agregar nuevo ejercicio
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <p className="text-sm text-gray-500 mb-4">
          Seleccioná un ejercicio para modificarlo, cambiar su estado o eliminarlo.
        </p>

        {/* Listado en Acordeones por Grupo Muscular */}
        <div className="space-y-3">
          {gruposMusculares.map((grupo) => {
            const listaEjercicios = ejerciciosPorGrupo[grupo] || [];
            const isOpen = openGroups[grupo];

            return (
              <div key={grupo} className="border border-gray-200 rounded-lg overflow-hidden transition-all">
                <button
                  type="button"
                  onClick={() => toggleGroup(grupo)}
                  className="w-full flex justify-between items-center p-4 bg-gray-100 hover:bg-gray-200 text-left font-semibold text-gray-800 transition"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-gray-900">{grupo}</span>
                    <span className="bg-white text-gray-600 text-xs px-2.5 py-1 rounded-full border border-gray-300 font-normal">
                      {listaEjercicios.length} ejercicios
                    </span>
                  </div>
                  <span className={`transform transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}>
                    ▼
                  </span>
                </button>

                {isOpen && (
                  <div className="bg-white border-t border-gray-200">
                    {listaEjercicios.length === 0 ? (
                      <p className="p-4 text-sm text-gray-400 italic text-center">No hay ejercicios registrados en este grupo.</p>
                    ) : (
                      <table className="w-full text-left border-collapse">
                        <thead>
                          <tr className="border-b border-gray-100 text-xs text-gray-400 uppercase bg-gray-50">
                            <th className="py-2.5 px-4 font-medium">Nombre</th>
                            <th className="py-2.5 px-4 font-medium">Descripción</th>
                            <th className="py-2.5 px-4 font-medium text-right">Estado</th>
                          </tr>
                        </thead>
                        <tbody>
                          {listaEjercicios.map((ej, index) => {
                            const ejercicioId = ej.id || ej.idEjercicio || index;
                            
                            const estaSeleccionado = ejercicioSeleccionado && 
                              (
                                (ej.id && ejercicioSeleccionado.id && ej.id === ejercicioSeleccionado.id) ||
                                (ej.idEjercicio && ejercicioSeleccionado.idEjercicio && ej.idEjercicio === ejercicioSeleccionado.idEjercicio) ||
                                (ej.nombre === ejercicioSeleccionado.nombre)
                              );
                            
                            const estaActivo = ej.activo !== false;

                            return (
                              <tr 
                                key={ejercicioId} 
                                onClick={() => setEjercicioSeleccionado(ej)}
                                className={`border-b border-gray-100 cursor-pointer transition ${
                                  estaSeleccionado 
                                    ? 'bg-red-50/90 border-l-4 border-l-red-600' 
                                    : 'hover:bg-red-50/40'
                                }`}
                              >
                                <td className="py-3 px-4 font-medium text-gray-800">
                                  {ej.nombre}
                                </td>
                                <td className="py-3 px-4 text-gray-500 text-sm">{ej.descripcion || '-'}</td>
                                <td className="py-3 px-4 text-right">
                                  <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${
                                    estaActivo ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                                  }`}>
                                    {estaActivo ? 'Activo' : 'Inactivo'}
                                  </span>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* MODAL DE ACCIONES AL SELECCIONAR UN EJERCICIO */}
      {ejercicioSeleccionado && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 animate-fadeIn">
            <h3 className="text-lg font-bold text-gray-900 mb-1">
              {ejercicioSeleccionado.nombre}
            </h3>
            <p className="text-sm text-gray-500 mb-6">
              Selecciona qué acción deseas realizar sobre este ejercicio:
            </p>

            <div className="space-y-3">
              {/* Opción 1: Modificar */}
              <button
                type="button"
                onClick={() => {
                  const id = ejercicioSeleccionado.id || ejercicioSeleccionado.idEjercicio;
                  setEjercicioSeleccionado(null);
                  onEdit(id);
                }}
                className="w-full text-left px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-lg font-medium text-gray-800 transition flex items-center justify-between border border-gray-200"
              >
                <span>✏️ Modificar datos</span>
                <span className="text-xs text-gray-400">Editar nombre/grupo</span>
              </button>

              {/* Opción 2: Toggle Estado */}
              <button
                type="button"
                onClick={() => {
                  const ej = ejercicioSeleccionado;
                  const estaActivo = ej.activo !== false;
                  setEjercicioSeleccionado(null);
                  onToggleEstado(ej, !estaActivo); 
                }}
                className="w-full text-left px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-lg font-medium text-gray-800 transition flex items-center justify-between border border-gray-200"
              >
                <span>🔄 {ejercicioSeleccionado.activo !== false ? 'Desactivar ejercicio' : 'Activar ejercicio'}</span>
                <span className="text-xs text-gray-400">Cambiar estado</span>
              </button>

              {/* Opción 3: Eliminar Definitivamente */}
              <button
                type="button"
                onClick={() => {
                  const id = ejercicioSeleccionado.id || ejercicioSeleccionado.idEjercicio;
                  setEjercicioSeleccionado(null);
                  onDelete(id);
                }}
                className="w-full text-left px-4 py-3 bg-red-50 hover:bg-red-100 rounded-lg font-medium text-red-700 transition flex items-center justify-between border border-red-200"
              >
                <span>🗑️ Eliminar definitivamente</span>
                <span className="text-xs text-red-400">Borrar de la base</span>
              </button>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                type="button"
                onClick={() => setEjercicioSeleccionado(null)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 text-sm font-medium transition"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}