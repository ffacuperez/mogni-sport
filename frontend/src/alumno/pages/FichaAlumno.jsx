import { Navigate, NavLink, Route, Routes } from 'react-router';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Panel } from '../../shared/components/ui';
import CuotasAlumno from '../../cuota/pages/CuotasAlumno';
import ProgresoAlumno from '../../progreso/pages/ProgresoAlumno';
import DatosAlumno from './DatosAlumno';

/** Avatar con iniciales, nombre, DNI, membresía y estado. */
function EncabezadoAlumno({ alumno }) {
  const isActivo = alumno.estado === 'Activo';
  const iniciales = `${alumno.nombre?.[0] ?? ''}${alumno.apellido?.[0] ?? ''}`;
  const membresia = alumno.inscripcion?.membresia ?? alumno.membresia;
  return (
    <Panel className="flex items-center gap-4 px-5 py-4">
      <span aria-hidden="true" className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-error-bg text-lg font-bold text-primary">
        {iniciales}
      </span>
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-xl font-semibold text-ink">{alumno.nombre} {alumno.apellido}</h1>
        <p className="text-sm text-ink-soft">
          <span className="tabular-nums">DNI {alumno.dni}</span>
          {membresia && <><span className="mx-2 text-ink-muted">·</span>Membresía {membresia}</>}
        </p>
      </div>
      <Badge tone={isActivo ? 'success' : 'error'} className="shrink-0">{alumno.estado}</Badge>
    </Panel>
  );
}

/** Pestañas subrayadas de la ficha. Cada una es una ruta, así volver desde otra pantalla deja la pestaña donde estaba. */
function TabsFicha({ tabs }) {
  return (
    <nav aria-label="Secciones del alumno" className="-mb-px flex gap-6">
      {tabs.map(({ to, texto, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => cx(
            'border-b-2 px-0.5 pb-3 pt-1 text-[15px] transition-colors',
            isActive ? 'border-primary font-semibold text-primary' : 'border-transparent font-medium text-ink-soft hover:text-ink',
            FOCUS,
          )}
        >
          {texto}
        </NavLink>
      ))}
    </nav>
  );
}

/**
 * Ficha del alumno: encabezado y pestañas Datos | Cuotas | Progreso. Cuotas solo para el propietario (como la
 * cobranza); Progreso para quien puede ver planes (propietario y entrenador).
 */
export default function FichaAlumno({ alumno, esPropietario, puedeVerPlanes, onEdit, onEditMembresia, onToggleStatus, onVerPlan, onAsignarPlan, onVolver }) {
  const base = `/alumnos/${alumno.id}`;
  const tabs = [
    { to: base, texto: 'Datos', end: true },
    ...(esPropietario ? [{ to: `${base}/cuotas`, texto: 'Cuotas' }] : []),
    ...(puedeVerPlanes ? [{ to: `${base}/progreso`, texto: 'Progreso' }] : []),
  ];

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <EncabezadoAlumno alumno={alumno} />

      <div className="flex flex-col gap-3 border-b border-line md:flex-row md:items-end md:justify-between">
        <TabsFicha tabs={tabs} />
      </div>

      <Routes>
        <Route index element={
          <DatosAlumno
            alumno={alumno}
            onEdit={onEdit}
            onEditMembresia={onEditMembresia}
            onToggleStatus={onToggleStatus}
            puedeVerPlanes={puedeVerPlanes}
            onVerPlan={onVerPlan}
            onAsignarPlan={onAsignarPlan}
          />
        } />
        {esPropietario && <Route path="cuotas" element={<CuotasAlumno alumno={alumno} />} />}
        {puedeVerPlanes && (
          <Route path="progreso/*" element={<ProgresoAlumno alumno={alumno} onVolver={onVolver} />} />
        )}
        <Route path="*" element={<Navigate to={base} replace />} />
      </Routes>
    </div>
  );
}
