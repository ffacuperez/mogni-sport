import { useEffect, useState } from 'react';
import { ClipboardList } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import DetallePlan from '../components/DetallePlan';
import ErrorConsulta from '../components/ErrorConsulta';
import EstadoVacio from '../components/EstadoVacio';
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

  if (plan === null) {
    return <EstadoVacio icono={ClipboardList} titulo="Todavía no tenés un plan asignado" texto="Tu profe te va a asignar uno pronto." />;
  }

  return <DetallePlan plan={plan} />;
}
