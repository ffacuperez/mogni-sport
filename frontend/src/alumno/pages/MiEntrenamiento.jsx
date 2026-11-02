import { useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Dumbbell, Info } from 'lucide-react';

import { Button, Panel } from '../../shared/components/ui';
import { cx } from '../../shared/components/clases';
import EjercicioCard from './EjercicioCard';
import SelectorSesiones from './SelectorSesiones';
import SesionResumen from './SesionResumen';
import { mockPlanActivo } from './mockEntrenamientoData';
import { calcular1RM, esEjercicioCompleto } from './entrenamientoUtils';

function clonarPlan(plan) {
  return JSON.parse(JSON.stringify(plan));
}

export default function MiEntrenamiento() {
  const [plan, setPlan] = useState(() => clonarPlan(mockPlanActivo));
  const [selectedSessionId, setSelectedSessionId] = useState(mockPlanActivo.sesiones[0]?.id ?? null);
  const [feedback, setFeedback] = useState(null);

  const selectedSession = useMemo(() => {
    if (!plan.activo) return null;
    return plan.sesiones.find((session) => String(session.id) === String(selectedSessionId)) ?? plan.sesiones[0] ?? null;
  }, [plan.activo, plan.sesiones, selectedSessionId]);

  const pendingExercises = selectedSession?.ejercicios.filter((ejercicio) => !esEjercicioCompleto(ejercicio)) ?? [];
  const sessionCompleta = Boolean(selectedSession && selectedSession.ejercicios.length && selectedSession.ejercicios.every(esEjercicioCompleto));

  const actualizarSerie = (sessionId, ejercicioId, numeroSerie, campo, valor) => {
    setPlan((prev) => ({
      ...prev,
      sesiones: prev.sesiones.map((session) => {
        if (session.id !== sessionId) return session;

        return {
          ...session,
          ejercicios: session.ejercicios.map((ejercicio) => {
            if (ejercicio.id !== ejercicioId) return ejercicio;

            const seriesActualizadas = ejercicio.series.map((serie) => {
              if (serie.numero !== numeroSerie) return serie;

              const siguiente = { ...serie, [campo]: valor };
              const peso = campo === 'peso' ? valor : siguiente.peso;
              const repeticiones = campo === 'repeticiones' ? valor : siguiente.repeticiones;
              const rirRealizado = campo === 'rirRealizado' ? valor : siguiente.rirRealizado;

              return {
                ...siguiente,
                rmEstimado: calcular1RM({ peso, repeticiones }),
                rirRealizado,
              };
            });

            const ejercicioActualizado = {
              ...ejercicio,
              series: seriesActualizadas,
              estado: seriesActualizadas.every((serie) => Number.parseFloat(serie.peso) > 0 && Number.parseFloat(serie.repeticiones) > 0 && Number.parseFloat(serie.rirRealizado) >= 0) ? 'Completo' : 'Pendiente',
            };

            return ejercicioActualizado;
          }),
        };
      }),
    }));

    setFeedback(null);
  };

  const actualizarNota = (sessionId, ejercicioId, nota) => {
    setPlan((prev) => ({
      ...prev,
      sesiones: prev.sesiones.map((session) => {
        if (session.id !== sessionId) return session;
        return {
          ...session,
          ejercicios: session.ejercicios.map((ejercicio) => ejercicio.id === ejercicioId ? { ...ejercicio, nota } : ejercicio),
        };
      }),
    }));

    setFeedback(null);
  };

  const finalizarSesion = () => {
    if (!selectedSession) return;

    const pendientes = selectedSession.ejercicios.filter((ejercicio) => !esEjercicioCompleto(ejercicio)).map((ejercicio) => ejercicio.nombre);

    if (pendientes.length > 0) {
      setFeedback({
        type: 'error',
        message: `Completá los siguientes ejercicios antes de finalizar: ${pendientes.join(', ')}.`,
      });
      return;
    }

    setPlan((prev) => ({
      ...prev,
      sesiones: prev.sesiones.map((session) => session.id === selectedSession.id ? { ...session, estado: 'Completa' } : session),
    }));

    setFeedback({
      type: 'success',
      message: 'Entrenamiento registrado correctamente.',
    });
  };

  if (!plan.activo) {
    return (
      <div className="animate-fade-in">
        <div className="mb-6 space-y-2">
          <h1 className="text-2xl font-bold text-ink">Mi entrenamiento</h1>
          <p className="text-sm text-ink-soft">Completá los ejercicios de tu sesión y registrá tus datos</p>
        </div>

        <Panel className="rounded-xl border border-line bg-card p-5 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-error-bg text-primary">
            <Info className="h-6 w-6" />
          </div>
          <p className="text-base font-semibold text-ink">Sin plan activo</p>
          <p className="mt-2 text-sm text-ink-soft">Actualmente no tenés un plan de entrenamiento activo asignado.</p>
        </Panel>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-6 space-y-2">
        <h1 className="text-2xl font-bold text-ink">Mi entrenamiento</h1>
        <p className="text-sm text-ink-soft">Completá los ejercicios de tu sesión y registrá tus datos</p>
      </div>

      <SelectorSesiones sesiones={plan.sesiones} selectedId={selectedSessionId} onSelect={setSelectedSessionId} />

      {selectedSession && (
        <>
          <SesionResumen planNombre={plan.nombre} sesion={selectedSession} />

          {selectedSession.ejercicios.map((ejercicio) => (
            <EjercicioCard
              key={ejercicio.id}
              ejercicio={ejercicio}
              onUpdateSerie={(numeroSerie, campo, valor) => actualizarSerie(selectedSession.id, ejercicio.id, numeroSerie, campo, valor)}
              onUpdateNota={(nota) => actualizarNota(selectedSession.id, ejercicio.id, nota)}
            />
          ))}

          <Panel className={cx('mt-6 p-4', !sessionCompleta ? 'border-warning bg-warning-bg' : 'border-success bg-success-bg/40')}>
            <div className="flex items-start gap-3">
              {!sessionCompleta ? (
                <AlertTriangle className="mt-0.5 h-5 w-5 text-warning-text" />
              ) : (
                <CheckCircle2 className="mt-0.5 h-5 w-5 text-success" />
              )}
              <div className="flex-1">
                <p className="text-sm font-semibold text-ink">
                  {sessionCompleta ? 'Sesión completa' : 'Completá todos los ejercicios para poder finalizar'}
                </p>
                {!sessionCompleta && (
                  <p className="mt-1 text-xs text-ink-soft">
                    {pendingExercises.length > 0
                      ? `Faltan ${pendingExercises.length} ejercicio${pendingExercises.length > 1 ? 's' : ''} por completar.`
                      : 'Hay datos pendientes.'}
                  </p>
                )}
              </div>
            </div>

            <Button
              type="button"
              variant="primary"
              className="mt-4 w-full"
              disabled={!sessionCompleta}
              onClick={finalizarSesion}
            >
              Completar sesión
            </Button>

            {feedback && (
              <div
                className={cx(
                  'mt-4 rounded-lg border px-3 py-2 text-sm',
                  feedback.type === 'success'
                    ? 'border-success bg-success-bg text-success'
                    : 'border-error bg-error-bg text-error',
                )}
                role="status"
              >
                {feedback.message}
              </div>
            )}
          </Panel>
        </>
      )}

      {!selectedSession && (
        <Panel className="p-5 text-center text-sm text-ink-soft">
          <Dumbbell className="mx-auto mb-2 h-6 w-6 text-ink-muted" />
          No hay sesiones disponibles para este plan.
        </Panel>
      )}
    </div>
  );
}
