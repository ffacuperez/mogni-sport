import { useEffect, useState } from 'react';
import { ClipboardList, Plus } from 'lucide-react';

import FinalizarPlanModal from '../../plan/components/FinalizarPlanModal';
import { finalizarPlan } from '../../plan/services/planesService';
import { Button, Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import EstadoVacioPanel from './EstadoVacioPanel';
import ResumenPlanPanel from './ResumenPlanPanel';
import SesionesDeLaSemana from './SesionesDeLaSemana';

import { useSesionAbierta } from './useSesionAbierta';

/**
 * Vista "Plan actual" del progreso en el panel: el resumen y las sesiones de la semana en columnas, con la opción de
 * modificar o finalizar el plan. Sin plan activo, el estado vacío ofrece asignarle uno (si el alumno está activo).
 */
export default function PlanActualPanel({ alumno, onAsignarPlan, onModificarPlan, onPlanFinalizado, onToast }) {
  const { consultas, textos } = useProgreso();
  const { abrir } = useSesionAbierta();
  // undefined: cargando; null: no tiene plan activo
  const [plan, setPlan] = useState(undefined);
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const nombreAlumno = `${alumno.nombre} ${alumno.apellido}`;

  const finalizar = async () => {
    setEnviando(true);
    try {
      await finalizarPlan(plan.id);
      setPlan(null);
      onToast(`Se finalizó el plan de ${nombreAlumno}. Ya le podés asignar uno nuevo.`);
      onPlanFinalizado();
    } catch (err) {
      onToast(err.message, true);
    } finally {
      setEnviando(false);
      setConfirmando(false);
    }
  };

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
      <ResumenPlanPanel plan={plan} onModificar={() => onModificarPlan(plan.id)} onFinalizar={() => setConfirmando(true)} />
      <SesionesDeLaSemana key={plan.id} plan={plan} semanaInicial={plan.semanaActual} onVerRegistro={abrir} />
      {confirmando && (
        <FinalizarPlanModal
          nombrePlan={plan.nombre}
          nombreAlumno={nombreAlumno}
          enviando={enviando}
          onConfirm={finalizar}
          onCancel={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
