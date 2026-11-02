import { useEffect, useState } from 'react';

import { Cargando } from '../../shared/components/ui';
import DetallePlan from '../components/DetallePlan';
import ErrorConsulta from '../components/ErrorConsulta';
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

  if (error) return <ErrorConsulta error={error} />;

  if (plan === undefined) return <Cargando texto="Cargando tu plan" />;

  // Provisorio: el estado vacío con el diseño del frame 06 llega en la Etapa 3
  if (plan === null) return <p className="mt-12 text-center text-sm font-semibold text-ink">Todavía no tenés un plan asignado</p>;

  return <DetallePlan plan={plan} />;
}
