import { useEffect, useState } from 'react';
import { AlertCircle, Calendar, CreditCard, Dumbbell, Flame, User } from 'lucide-react';

import { obtenerMiPerfil } from '../services/alumnosService';

const formatearFecha = (iso) => {
  if (!iso) return '—';
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
};

export default function AlumnoDashboard() {
  const [alumno, setAlumno] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    obtenerMiPerfil()
      .then(setAlumno)
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return (
      <div className="max-w-6xl mx-auto">
        <div className="bg-[#151c24] border border-red-900/40 rounded-2xl p-6 flex items-center gap-3 text-red-400">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      </div>
    );
  }

  if (!alumno) {
    return (
      <div className="max-w-6xl mx-auto text-sm text-gray-400" role="status">
        Cargando tus datos...
      </div>
    );
  }

  const tieneMembresia = Boolean(alumno.membresiaId);
  const activo = alumno.estado === 'Activo';

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-fadeIn">

      {/* Banner de Bienvenida */}
      <div className="bg-gradient-to-r from-[#151c24] to-[#1d2630] border border-red-900/30 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
        <div className="absolute top-[-20%] right-[-10%] w-[300px] h-[300px] bg-red-600/10 rounded-full blur-[100px] pointer-events-none" />

        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 relative z-10">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-950/60 border border-red-800/50 text-red-400 text-xs font-bold tracking-wider uppercase mb-3">
              <Flame className="w-3.5 h-3.5 text-[#B32334]" /> Alumno
            </div>
            <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
              ¡Hola, {alumno.nombre}!
            </h1>
            <p className="text-gray-400 text-sm mt-1">
              Bienvenido a tu panel de entrenamiento en Mogni Sport.
            </p>
          </div>

          <div className="bg-[#131A22] border border-gray-800 rounded-2xl p-4 flex items-center gap-4 shadow-inner">
            <div className="w-12 h-12 rounded-xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-500 font-bold text-lg">
              {alumno.nombre?.[0]}{alumno.apellido?.[0]}
            </div>
            <div>
              <p className="text-sm font-bold text-white">{alumno.nombre} {alumno.apellido}</p>
              <p className="text-xs text-gray-400">DNI: {alumno.dni}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Tarjetas de Resumen */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">

        {/* Membresía */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Tu Membresía</p>
              <h3 className="text-xl font-extrabold text-white mt-1">{tieneMembresia ? alumno.membresia : 'Sin membresía'}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between text-xs">
            <span className={`font-medium flex items-center gap-1.5 ${activo ? 'text-emerald-400' : 'text-red-400'}`}>
              <span className={`w-2 h-2 rounded-full ${activo ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`}></span> {alumno.estado}
            </span>
            {tieneMembresia && <span className="text-gray-400">Vence: {formatearFecha(alumno.fechaVencimiento)}</span>}
          </div>
        </div>

        {/* Inscripción */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Inscripción</p>
              <h3 className="text-xl font-extrabold text-white mt-1">{formatearFecha(alumno.fechaIngreso)}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <Calendar className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between text-xs">
            <span className="text-gray-400">Fecha de inicio de tu membresía actual</span>
          </div>
        </div>

        {/* Datos de contacto */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div className="min-w-0">
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Tus Datos</p>
              <h3 className="text-sm font-bold text-white mt-1 truncate">{alumno.email}</h3>
              <p className="text-xs text-gray-400 mt-1">{alumno.telefono || 'Sin teléfono'}</p>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <User className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 text-xs">
            <span className="text-gray-400">Objetivo: </span>
            <span className="text-gray-200">{alumno.objetivo || 'Sin definir'}</span>
          </div>
        </div>

      </div>

      {/* Rutinas: todavía no existen en el backend */}
      <div className="bg-[#151c24] border border-gray-800 rounded-3xl p-6 shadow-xl">
        <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-4">
          <Dumbbell className="w-5 h-5 text-[#B32334]" /> Tus Rutinas Asignadas
        </h2>
        <p className="text-sm text-gray-400">Todavía no tenés rutinas asignadas.</p>
      </div>

    </div>
  );
}
