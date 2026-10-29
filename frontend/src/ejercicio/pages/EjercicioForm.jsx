import React, { useState } from 'react';

export default function EjercicioForm({ 
  onSubmit, 
  onGuardar, 
  onSave, 
  agregarEjercicio, 
  onCancel, 
  initialData = {} 
}) {
  const [nombre, setNombre] = useState(initialData.nombre || '');
  const [grupoMuscular, setGrupoMuscular] = useState(initialData.grupoMuscular || 'Piernas');
  const [descripcion, setDescripcion] = useState(initialData.descripcion || '');

  // Validación en tiempo real (solo letras, acentos, ñ y espacios)
  const handleNombreChange = (e) => {
    const valor = e.target.value;
    const soloLetrasRegex = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]*$/;

    if (soloLetrasRegex.test(valor)) {
      setNombre(valor);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const nombreLimpio = nombre.trim();

    if (nombreLimpio.length < 3) {
      alert("El nombre del ejercicio debe tener al menos 3 caracteres.");
      return;
    }

    const ejercicioData = {
      ...initialData,
      nombre: nombreLimpio,
      grupoMuscular,
      descripcion
    };

    // Resuelve automáticamente según cuál prop le pasaste desde App.jsx
    const submitCallback = onSubmit || onGuardar || onSave || agregarEjercicio;

    if (submitCallback) {
      submitCallback(ejercicioData);
    } else {
      console.error("No se encontró ninguna función de envío en las props.");
    }
  };

  return (
    <div className="p-6 max-w-2xl mx-auto bg-white rounded-xl shadow-sm border border-gray-200">
      <h2 className="text-xl font-bold text-gray-900 mb-4">
        {initialData.id ? 'Modificar ejercicio' : 'Alta de nuevo ejercicio'}
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Nombre del ejercicio
          </label>
          <input
            type="text"
            value={nombre}
            onChange={handleNombreChange}
            placeholder="Ej. Sentadilla"
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
          />
          <p className="text-xs text-gray-400 mt-1">Solo letras, acentos y espacios.</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Grupo muscular
          </label>
          <select
            value={grupoMuscular}
            onChange={(e) => setGrupoMuscular(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 bg-white"
          >
            <option value="Piernas">Piernas</option>
            <option value="Espalda">Espalda</option>
            <option value="Pecho">Pecho</option>
            <option value="Hombros">Hombros</option>
            <option value="Brazos">Brazos</option>
            <option value="Core">Core</option>
            <option value="Glúteos">Glúteos</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Descripción (opcional)
          </label>
          <textarea
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            placeholder="Detalles de la técnica o ejecución..."
            rows="3"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
          />
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 font-medium transition"
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium transition shadow-sm"
          >
            Confirmar
          </button>
        </div>
      </form>
    </div>
  );
}