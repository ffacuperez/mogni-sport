import { useMemo } from 'react';
import { Navigate, Route, Routes } from 'react-router';

import ControlSegmentado from '../components/ControlSegmentado';
import EncabezadoDetalle from '../components/EncabezadoDetalle';
import { progresoParaElEntrenador, ProgresoContext } from '../services/ProgresoContext';
import DetallePlanAnterior from './DetallePlanAnterior';
import DetalleSesion from './DetalleSesion';
import PlanActual from './PlanActual';
import TabPlanes from './TabPlanes';
import TabSesiones from './TabSesiones';

/** Encabezado y tabs; cada tab es una ruta, como en Mi progreso. */
function Tabs({ alumno, onVolver, children }) {
  return (
    <>
      <EncabezadoDetalle
        titulo={`Progreso de ${alumno.nombre} ${alumno.apellido}`}
        subtitulo="Planes y sesiones registradas · solo lectura"
        onVolver={onVolver}
      />
      <ControlSegmentado />
      {children}
    </>
  );
}

/**
 * MOGNI-268: el progreso de un alumno para el entrenador. Son las mismas pantallas de Mi progreso (HU-270), con las
 * consultas, los links y los textos del entrenador. Angosta y centrada: se usa desde el celular durante las clases.
 */
export default function ProgresoAlumno({ alumno, onVolver }) {
  const progreso = useMemo(() => progresoParaElEntrenador(alumno.id), [alumno.id]);
  const tab = (contenido) => <Tabs alumno={alumno} onVolver={onVolver}>{contenido}</Tabs>;

  return (
    <ProgresoContext.Provider value={progreso}>
      <div className="mx-auto w-full max-w-xl animate-fade-in">
        <Routes>
          <Route index element={tab(<PlanActual />)} />
          <Route path="planes" element={tab(<TabPlanes />)} />
          <Route path="sesiones" element={tab(<TabSesiones />)} />
          <Route path="planes/:id" element={<DetallePlanAnterior />} />
          <Route path="sesiones/:id" element={<DetalleSesion />} />
          <Route path="*" element={<Navigate to={progreso.rutaBase} replace />} />
        </Routes>
      </div>
    </ProgresoContext.Provider>
  );
}
