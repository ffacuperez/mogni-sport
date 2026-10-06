import { useEffect, useState } from 'react';
import { AlertCircle, Calendar, CreditCard, Dumbbell, User } from 'lucide-react';

import { obtenerMiPerfil } from '../services/alumnosService';
import { formatearFecha } from '../../shared/services/texto';
import { Badge, Cargando, Panel } from '../../shared/components/ui';

function Tarjeta({ etiqueta, icono: Icono, children, pie }) {
  return (
    <Panel className="flex flex-col justify-between p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-medium text-ink-soft">{etiqueta}</p>
          {children}
        </div>
        <span className="rounded-lg bg-error-bg p-2.5 text-primary">
          <Icono className="h-5 w-5" />
        </span>
      </div>
      <div className="mt-4 border-t border-line pt-4 text-xs">{pie}</div>
    </Panel>
  );
}

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
      <div className="mx-auto max-w-6xl">
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-6 text-error">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      </div>
    );
  }

  if (!alumno) return <Cargando texto="Cargando tus datos" />;

  const tieneMembresia = Boolean(alumno.membresiaId);
  const activo = alumno.estado === 'Activo';

  return (
    <div className="mx-auto max-w-6xl space-y-6 animate-fade-in">

      {/* Banner de bienvenida */}
      <Panel className="p-6 sm:p-8">
        <div className="flex flex-col items-start justify-between gap-5 md:flex-row md:items-center">
          <div>
            <p className="mb-1 text-xs font-medium text-ink-soft">Panel del alumno</p>
            <h1 className="text-2xl font-bold text-ink">¡Hola, {alumno.nombre}!</h1>
            <p className="mt-1 text-sm text-ink-soft">Bienvenido a tu panel de entrenamiento en Mogni Sport.</p>
          </div>

          <div className="flex items-center gap-4 rounded-lg border border-line bg-row-hover p-4">
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-error-bg text-lg font-bold text-primary">
              {alumno.nombre?.[0]}{alumno.apellido?.[0]}
            </span>
            <div>
              <p className="text-sm font-bold text-ink">{alumno.nombre} {alumno.apellido}</p>
              <p className="text-xs tabular-nums text-ink-soft">DNI {alumno.dni}</p>
            </div>
          </div>
        </div>
      </Panel>

      {/* Tarjetas de resumen */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Tarjeta
          etiqueta="Tu membresía"
          icono={CreditCard}
          pie={<Badge tone={activo ? 'success' : 'error'}>{alumno.estado}</Badge>}
        >
          <h3 className="mt-1 text-base font-semibold text-ink">{tieneMembresia ? alumno.membresia : 'Sin membresía'}</h3>
        </Tarjeta>

        <Tarjeta
          etiqueta="Inscripción"
          icono={Calendar}
          pie={<span className="text-ink-soft">Fecha de inicio de tu membresía actual</span>}
        >
          <h3 className="mt-1 text-base font-semibold tabular-nums text-ink">{formatearFecha(alumno.fechaIngreso)}</h3>
        </Tarjeta>

        <Tarjeta
          etiqueta="Tus datos"
          icono={User}
          pie={<><span className="text-ink-soft">Objetivo: </span><span className="text-ink">{alumno.objetivo || 'Sin definir'}</span></>}
        >
          <h3 className="mt-1 truncate text-sm font-bold text-ink">{alumno.email}</h3>
          <p className="mt-1 text-xs text-ink-soft">{alumno.telefono || 'Sin teléfono'}</p>
        </Tarjeta>
      </div>

      {/* Rutinas: todavía no existen en el backend */}
      <Panel className="p-6">
        <h2 className="mb-4 flex items-center gap-2 text-base font-semibold text-ink">
          <Dumbbell className="h-5 w-5 text-primary" /> Tus rutinas asignadas
        </h2>
        <p className="rounded-lg border border-dashed border-line bg-row-hover px-4 py-6 text-center text-sm text-ink-soft">
          Todavía no tenés rutinas asignadas.
        </p>
      </Panel>
    </div>
  );
}
