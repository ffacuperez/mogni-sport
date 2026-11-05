import { useEffect, useState } from 'react';
import { ClipboardList } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import DetallePlan from '../components/DetallePlan';
import ErrorConsulta from '../components/ErrorConsulta';
import EstadoVacio from '../components/EstadoVacio';
import { useProgreso } from '../services/ProgresoContext';

/** Tab "Plan actual". undefined: cargando; null: el alumno no tiene plan activo. */
export default function PlanActual() {
  const { consultas, textos } = useProgreso();
  const [plan, setPlan] = useState(undefined);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlanActivo()
      .then((respuesta) => { if (vigente) setPlan(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas]);

  if (error) return <ErrorConsulta error={error} />;

  if (plan === undefined) return <Cargando texto={textos.cargandoPlan} />;

  if (plan === null) {
    return <EstadoVacio icono={ClipboardList} {...textos.sinPlan} />;
  }

  return <DetallePlan plan={plan} />;
}
