import { useEffect, useState } from 'react';
import { ClipboardList, Plus } from 'lucide-react';

import { Button, Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import EstadoVacioPanel from './EstadoVacioPanel';
import ResumenPlanPanel from './ResumenPlanPanel';
import SesionesDeLaSemana from './SesionesDeLaSemana';
import { useSesionAbierta } from './useSesionAbierta';

/**
 * Vista "Plan actual" del progreso en el panel: el resumen y las sesiones de la semana en columnas. Sin plan activo,
 * el estado vacío ofrece asignarle uno (si el alumno está activo).
 */
export default function PlanActualPanel({ alumno, onAsignarPlan, onAjustarPlan }) {
  const { consultas, textos } = useProgreso();
  const { abrir } = useSesionAbierta();
  // undefined: cargando; null: no tiene plan activo
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
    return (
      <EstadoVacioPanel
        icono={ClipboardList}
        {...textos.sinPlan}
        accion={alumno.estado === 'Activo' && (
          <Button variant="primary" onClick={onAsignarPlan}>
            <Plus className="h-4 w-4" /> Asignar plan
          </Button>
        )}
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <ResumenPlanPanel plan={plan} onAjustar={() => onAjustarPlan(plan.id)} />
      <SesionesDeLaSemana key={plan.id} plan={plan} semanaInicial={plan.semanaActual} onVerRegistro={abrir} />
    </div>
  );
}
