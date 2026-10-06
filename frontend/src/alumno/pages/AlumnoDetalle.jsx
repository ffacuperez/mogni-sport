const formatearFecha = (iso) => {
  if (!iso) return '—';
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
};

const CLASES_ESTADO_INSCRIPCION = {
  Activa: 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40',
  Vencida: 'bg-amber-950/60 text-amber-400 border border-amber-800/40',
  Cancelada: 'bg-red-950/60 text-red-400 border border-red-800/40',
};

function Dato({ label, children, ancho = false }) {
  return (
    <div className={ancho ? 'md:col-span-2' : ''}>
      <dt className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{label}</dt>
      <dd className="mt-1 text-slate-100 break-words">{children || '—'}</dd>
    </div>
  );
}

export default function AlumnoDetalle({ alumno, onBack, onEdit, onEditMembresia, onToggleStatus }) {
  const isActivo = alumno.estado === 'Activo';
  const inscripcion = alumno.inscripcion;

  return (
    <div className="animate-in fade-in duration-300 max-w-4xl mx-auto">
      <button onClick={onBack} className="text-sm text-slate-400 hover:text-slate-100 transition-colors mb-4">
        ← Volver al listado
      </button>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">{alumno.nombre} {alumno.apellido}</h1>
          <p className="text-slate-400 mt-1">DNI {alumno.dni}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={onToggleStatus}
            className={`text-white px-4 py-2 rounded-md font-medium transition-colors ${isActivo ? 'bg-red-800 hover:bg-red-900' : 'bg-[#1b5e3a] hover:bg-[#14492d]'}`}
          >
            {isActivo ? 'Dar de baja' : 'Activar'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6">
        {/* Datos personales */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
            <div className="flex flex-wrap items-center gap-3">
              <h2 className="text-lg font-semibold text-white">Datos personales</h2>
              <span className={`px-3 py-1 rounded-full text-xs font-semibold ${isActivo ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40' : 'bg-slate-800 text-slate-400 border border-slate-700'}`}>
                Alumno {alumno.estado.toLowerCase()}
              </span>
            </div>
            <button onClick={onEdit} className="bg-[#1b5e3a] hover:bg-[#14492d] text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors">
              Modificar alumno
            </button>
          </div>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <Dato label="Nombre">{alumno.nombre}</Dato>
            <Dato label="Apellido">{alumno.apellido}</Dato>
            <Dato label="DNI">{alumno.dni}</Dato>
            <Dato label="Email">{alumno.email}</Dato>
            <Dato label="Fecha de nacimiento">{formatearFecha(alumno.fechaNacimiento)}</Dato>
            <Dato label="Edad">{alumno.edad != null ? `${alumno.edad} años` : ''}</Dato>
            <Dato label="Teléfono">{alumno.telefono}</Dato>
            <Dato label="Sexo">{alumno.sexo}</Dato>
            <Dato label="Objetivo" ancho>{alumno.objetivo}</Dato>
            <Dato label="Antecedentes de salud" ancho>{alumno.antecedentes}</Dato>
          </dl>
        </section>

        {/* Inscripción / membresía */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
            <div className="flex flex-wrap items-center gap-3">
              <h2 className="text-lg font-semibold text-white">Membresía</h2>
              {inscripcion && (
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${CLASES_ESTADO_INSCRIPCION[inscripcion.estado] ?? 'bg-slate-800 text-slate-400 border border-slate-700'}`}>
                  Inscripción {inscripcion.estado.toLowerCase()}
                </span>
              )}
            </div>
            <button onClick={onEditMembresia} className="bg-[#1b5e3a] hover:bg-[#14492d] text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors">
              Modificar membresía
            </button>
          </div>
          {inscripcion ? (
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <Dato label="Membresía" ancho>{inscripcion.membresia}</Dato>
              <Dato label="Fecha de inicio">{formatearFecha(inscripcion.fechaInicio)}</Dato>
              {/* fechaFin solo existe si la inscripción se canceló */}
              {inscripcion.fechaFin && <Dato label="Fecha de cancelación">{formatearFecha(inscripcion.fechaFin)}</Dato>}
            </dl>
          ) : (
            <p className="text-slate-400">Sin membresía: el alumno nunca tuvo una inscripción.</p>
          )}
        </section>
      </div>
    </div>
  );
}
