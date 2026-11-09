import { useEffect, useState } from 'react';
import { ArrowLeft, CircleAlert, Flag, Pencil, UserPlus } from 'lucide-react';

import FinalizarPlanModal from '../components/FinalizarPlanModal';
import { Badge, Button, Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { cantidadEjercicios, textoNivel, textoObjetivo, tonoNivel } from '../../plantilla/services/plantillasService';
import TarjetasSesiones from '../../plantilla/components/TarjetasSesiones';
import { finalizarPlan, obtenerPlan, textoFecha } from '../services/planesService';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

/** HU-266: el plan de un alumno, con la opción de modificarlo o de finalizarlo para poder asignarle otro. */
export default function PlanAsignadoDetalle({ planId, onVolver, onModificar, onAsignarNuevo, onToast }) {
  const [plan, setPlan] = useState(null);
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    obtenerPlan(planId).then(setPlan).catch((err) => setError(err.message));
  }, [planId]);

  const botonVolver = (
    <button type="button" onClick={onVolver}
      className={cx('mb-3 flex cursor-pointer items-center gap-1.5 rounded-md text-xs font-medium text-ink-soft transition-colors hover:text-ink', FOCUS)}>
      <ArrowLeft className="h-3.5 w-3.5" /> Volver
    </button>
  );

  if (error) {
    return (
      <div className="mx-auto max-w-5xl">
        {botonVolver}
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      </div>
    );
  }
  if (!plan) return <Cargando texto="Cargando plan" />;

  const activo = plan.estado === 'ACTIVO';

  const finalizar = async () => {
    setEnviando(true);
    try {
      setPlan(await finalizarPlan(plan.id));
      onToast(`Se finalizó el plan de ${plan.alumnoNombre}. Ya le podés asignar uno nuevo.`);
    } catch (err) {
      onToast(err.message, true);
    } finally {
      setEnviando(false);
      setConfirmando(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl animate-fade-in">
      {botonVolver}

      <PageHeader
        eyebrow={`Plan de entrenamiento · ${plan.alumnoNombre}`}
        title={plan.nombre}
        actions={activo ? (
          <>
            <Button variant="secondary" onClick={() => onModificar(plan.id)} disabled={enviando}>
              <Pencil className="h-4 w-4" /> Modificar plan
            </Button>
            <Button variant="danger" onClick={() => setConfirmando(true)} disabled={enviando}>
              <Flag className="h-4 w-4" /> Finalizar plan
            </Button>
          </>
        ) : (
          <Button variant="primary" onClick={() => onAsignarNuevo(plan.alumnoId)}>
            <UserPlus className="h-4 w-4" /> Asignar un nuevo plan
          </Button>
        )}
      >
        <div className="mt-3 flex flex-wrap items-center gap-1.5">
          <Badge tone={activo ? 'success' : 'neutral'}>{activo ? 'Activo' : 'Finalizado'}</Badge>
          <Badge tone={tonoNivel(plan.nivel)}>{textoNivel(plan.nivel)}</Badge>
          <Badge>{textoObjetivo(plan.objetivo)}</Badge>
        </div>
      </PageHeader>

      <Panel className="p-5">
        <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2 text-xs text-ink-soft">
          <p>
            Inicio: {textoFecha(plan.fechaAsignacion)}
            {plan.fechaFin && ` · Fin: ${textoFecha(plan.fechaFin)}`}
            {plan.fechaFinalizacion && ` · Finalizado el ${textoFecha(plan.fechaFinalizacion)}`}
            {' · '}{plan.plantillaOrigenId ? 'Copiado de una plantilla' : 'Plan personalizado'}
          </p>
          <p className="tabular-nums">
            {plural(plan.sesiones.length, 'sesión', 'sesiones')} · {plural(cantidadEjercicios(plan), 'ejercicio', 'ejercicios')}
          </p>
        </div>
        <TarjetasSesiones sesiones={plan.sesiones} />
      </Panel>

      {confirmando && (
        <FinalizarPlanModal
          nombrePlan={plan.nombre}
          nombreAlumno={plan.alumnoNombre}
          enviando={enviando}
          onConfirm={finalizar}
          onCancel={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
