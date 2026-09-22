import { Dumbbell, CreditCard, Calendar, Award, User, ArrowUpRight, Flame } from 'lucide-react';

export default function AlumnoDashboard() {
  // Datos simulados del alumno logueado (Sofía Gomez del ejemplo que tenías)
  const alumnoData = {
    nombre: "Sofía",
    apellido: "Gomez",
    dni: "45982776",
    email: "alumno@mognisport.com",
    membresia: "Pase Libre",
    estado: "Activo",
    vencimiento: "15/10/2026",
    profe: "Carlos Mogni"
  };

  const rutinasAsignadas = [
    { id: 1, dia: "Lunes", enfoque: "Tren Superior (Pecho y Tríceps)", ejercicios: 6, estado: "Completado" },
    { id: 2, dia: "Miércoles", enfoque: "Tren Inferior (Piernas y Glúteos)", ejercicios: 5, estado: "Pendiente" },
    { id: 3, dia: "Viernes", enfoque: "Full Body & Core", ejercicios: 7, estado: "Pendiente" }
  ];

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-fadeIn">
      
      {/* Banner de Bienvenida */}
      <div className="bg-gradient-to-r from-[#151c24] to-[#1d2630] border border-red-900/30 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
        <div className="absolute top-[-20%] right-[-10%] w-[300px] h-[300px] bg-red-600/10 rounded-full blur-[100px] pointer-events-none" />
        
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 relative z-10">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-950/60 border border-red-800/50 text-red-400 text-xs font-bold tracking-wider uppercase mb-3">
              <Flame className="w-3.5 h-3.5 text-[#B32334]" /> Alumno Regular
            </div>
            <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
              ¡Hola, {alumnoData.nombre}! 👋
            </h1>
            <p className="text-gray-400 text-sm mt-1">
              Bienvenida a tu panel de entrenamiento en Mogni Sport. Todo listo para entrenar hoy.
            </p>
          </div>
          
          <div className="bg-[#131A22] border border-gray-800 rounded-2xl p-4 flex items-center gap-4 shadow-inner">
            <div className="w-12 h-12 rounded-xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-500 font-bold text-lg">
              {alumnoData.nombre[0]}{alumnoData.apellido[0]}
            </div>
            <div>
              <p className="text-sm font-bold text-white">{alumnoData.nombre} {alumnoData.apellido}</p>
              <p className="text-xs text-gray-400">DNI: {alumnoData.dni}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Tarjetas de Resumen / Estadísticas */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        
        {/* Membresía */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Tu Membresía</p>
              <h3 className="text-xl font-extrabold text-white mt-1">{alumnoData.membresia}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between text-xs">
            <span className="text-emerald-400 font-medium flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span> {alumnoData.estado}
            </span>
            <span className="text-gray-400">Vence: {alumnoData.vencimiento}</span>
          </div>
        </div>

        {/* Rutinas Asignadas */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Rutinas Activas</p>
              <h3 className="text-xl font-extrabold text-white mt-1">3 Días / Semana</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <Dumbbell className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between text-xs">
            <span className="text-gray-300 font-medium">Profesor asignado:</span>
            <span className="text-white font-semibold">{alumnoData.profe}</span>
          </div>
        </div>

        {/* Asistencia / Progreso */}
        <div className="bg-[#151c24] border border-gray-800 hover:border-red-900/50 transition-all rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-400 tracking-wider uppercase">Asistencias del Mes</p>
              <h3 className="text-xl font-extrabold text-white mt-1">12 Clases</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-red-600/10 text-[#B32334]">
              <Calendar className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between text-xs">
            <span className="text-gray-400">Constancia excelente</span>
            <Award className="w-4 h-4 text-amber-400" />
          </div>
        </div>

      </div>

      {/* Sección de Rutinas */}
      <div className="bg-[#151c24] border border-gray-800 rounded-3xl p-6 shadow-xl">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Dumbbell className="w-5 h-5 text-[#B32334]" /> Tus Rutinas Asignadas
          </h2>
          <span className="text-xs text-gray-400">Actualizado para esta semana</span>
        </div>

        <div className="space-y-3">
          {rutinasAsignadas.map((rutina) => (
            <div key={rutina.id} className="bg-[#1d2630]/60 border border-gray-800/80 rounded-2xl p-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:border-red-900/40 transition-all">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-xl bg-[#131A22] border border-gray-700 flex items-center justify-center font-bold text-sm text-[#B32334]">
                  {rutina.dia.substring(0, 3)}
                </div>
                <div>
                  <h4 className="text-sm font-bold text-white">{rutina.enfoque}</h4>
                  <p className="text-xs text-gray-400">{rutina.ejercicios} ejercicios programados</p>
                </div>
              </div>
              
              <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-end">
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${rutina.estado === 'Completado' ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40' : 'bg-amber-950/60 text-amber-400 border border-amber-800/40'}`}>
                  {rutina.estado}
                </span>
                <button className="px-3.5 py-1.5 bg-[#B32334] hover:bg-[#9d1f2d] text-white rounded-xl text-xs font-bold transition flex items-center gap-1 cursor-pointer shadow-md">
                  Ver <ArrowUpRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}