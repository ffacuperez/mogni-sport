import { useEffect, useState } from 'react';
import { CircleAlert } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import AccesoBloqueado from '../../turno/components/AccesoBloqueado';
import DetallePlan from '../components/DetallePlan';
import { consultarPlanActivo } from '../services/progresoService';

/** Tab "Plan actual". undefined: cargando; null: el alumno no tiene plan activo. */
export default function PlanActual() {
  const [plan, setPlan] = useState(undefined);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarPlanActivo()
      .then((respuesta) => { if (vigente) setPlan(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  // 403: alumno inactivo. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) return <AccesoBloqueado titulo="No podés acceder a tu progreso" mensaje={error.message} />;

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error.message}</span>
      </div>
    );
  }

  if (plan === undefined) return <Cargando texto="Cargando tu plan" />;

  // Provisorio: el estado vacío con el diseño del frame 06 llega en la Etapa 3
  if (plan === null) return <p className="mt-12 text-center text-sm font-semibold text-ink">Todavía no tenés un plan asignado</p>;

  return <DetallePlan plan={plan} />;
}
