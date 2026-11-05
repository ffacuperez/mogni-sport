import { useMemo } from 'react';
import { Navigate, NavLink, Route, Routes } from 'react-router';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Panel } from '../../shared/components/ui';
import CuotasAlumno from '../../cuota/pages/CuotasAlumno';
import ControlSegmentadoPanel from '../../progreso/panel/ControlSegmentadoPanel';
import DetallePlanPanel from '../../progreso/panel/DetallePlanPanel';
import ProgresoPanel from '../../progreso/panel/ProgresoPanel';
import { progresoParaElEntrenador, ProgresoContext } from '../../progreso/services/ProgresoContext';
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
          <span className="whitespace-nowrap tabular-nums">DNI {alumno.dni}</span>
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

/** Encabezado y pestañas de la ficha; debajo, el contenido de la pestaña elegida. */
function Armazon({ alumno, tabs, children }) {
  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <EncabezadoAlumno alumno={alumno} />
      {/* Alto fijo en desktop: así las pestañas no saltan cuando aparece el control de Progreso */}
      <div className="flex flex-col gap-3 border-b border-line md:min-h-[52px] md:flex-row md:items-end md:justify-between">
        <TabsFicha tabs={tabs} />
        {/* En Progreso, sus vistas van a la derecha de las pestañas */}
        <Routes>
          <Route path="progreso/*" element={<ControlSegmentadoPanel />} />
          <Route path="*" element={null} />
        </Routes>
      </div>
      {children}
    </div>
  );
}

/**
 * Ficha del alumno: encabezado y pestañas Datos | Cuotas | Progreso. Cuotas solo para el propietario (como la
 * cobranza); Progreso para quien puede ver planes (propietario y entrenador). El detalle de un plan anterior se ve a
 * pantalla completa, sin el encabezado ni las pestañas.
 */
export default function FichaAlumno({ alumno, esPropietario, puedeVerPlanes, onEdit, onEditMembresia, onToggleStatus, onVerPlan, onAsignarPlan, onAjustarPlan }) {
  const base = `/alumnos/${alumno.id}`;
  const progreso = useMemo(() => progresoParaElEntrenador(alumno.id), [alumno.id]);
  const tabs = [
    { to: base, texto: 'Datos', end: true },
    ...(esPropietario ? [{ to: `${base}/cuotas`, texto: 'Cuotas' }] : []),
    ...(puedeVerPlanes ? [{ to: `${base}/progreso`, texto: 'Progreso' }] : []),
  ];

  return (
    <ProgresoContext.Provider value={progreso}>
      <Routes>
        {puedeVerPlanes && <Route path="progreso/planes/:planId" element={<DetallePlanPanel alumno={alumno} />} />}
        <Route path="*" element={
          <Armazon alumno={alumno} tabs={tabs}>
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
                <Route path="progreso/*" element={<ProgresoPanel alumno={alumno} onAsignarPlan={onAsignarPlan} onAjustarPlan={onAjustarPlan} />} />
              )}
              <Route path="*" element={<Navigate to={base} replace />} />
            </Routes>
          </Armazon>
        } />
      </Routes>
    </ProgresoContext.Provider>
  );
}
