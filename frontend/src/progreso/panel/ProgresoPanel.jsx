import { Navigate, Route, Routes, useParams } from 'react-router';

import { useProgreso } from '../services/ProgresoContext';
import DrawerSesionRuta from './DrawerSesion';
import HistorialPlanes from './HistorialPlanes';
import HistorialSesiones from './HistorialSesiones';
import PlanActualPanel from './PlanActualPanel';

/** Las URL viejas del detalle de sesión (/sesiones/:id) abren ahora el drawer sobre el historial. */
function SesionVieja() {
  const { id } = useParams();
  const { rutaBase } = useProgreso();
  return <Navigate to={`${rutaBase}/sesiones?sesion=${id}`} replace />;
}

/**
 * MOGNI-268: el progreso de un alumno en la pestaña Progreso de su ficha, para el entrenador y el propietario. Solo el
 * plan actual se puede modificar o finalizar. Cada vista es una ruta; el detalle de una sesión se abre en un drawer
 * desde cualquiera de ellas.
 */
export default function ProgresoPanel({ alumno, onAsignarPlan, onModificarPlan, onPlanFinalizado, onToast }) {
  const { rutaBase } = useProgreso();
  return (
    <div className="animate-fade-in">
      <Routes>
        <Route index element={<PlanActualPanel alumno={alumno} onAsignarPlan={onAsignarPlan} onModificarPlan={onModificarPlan} onPlanFinalizado={onPlanFinalizado} onToast={onToast} />} />
        <Route path="planes" element={<HistorialPlanes />} />
        <Route path="sesiones" element={<HistorialSesiones />} />
        <Route path="sesiones/:id" element={<SesionVieja />} />
        <Route path="*" element={<Navigate to={rutaBase} replace />} />
      </Routes>
      <DrawerSesionRuta alumno={alumno} />
    </div>
  );
}
