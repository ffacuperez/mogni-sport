import { useEffect, useMemo, useRef, useState } from 'react';
import { AlertTriangle, CheckCircle2, Dumbbell, Info } from 'lucide-react';

import { Button, Panel } from '../../shared/components/ui';
import { cx } from '../../shared/components/clases';
import EjercicioCard from './EjercicioCard';
import SelectorSesiones from './SelectorSesiones';
import SesionResumen from './SesionResumen';
import { calcular1RM, esEjercicioCompleto } from './entrenamientoUtils';
import {
  finalizarEntrenamiento,
  guardarEntrenamiento,
  iniciarSesionEntrenamiento,
  obtenerMiEntrenamiento,
} from '../services/entrenamientoService';

const crearPayload = (sesion) => ({
  ejercicios: (sesion?.ejercicios ?? []).map((ejercicio) => ({
    id: ejercicio.id,
    nota: ejercicio.nota ?? '',
    series: (ejercicio.series ?? []).map((serie) => ({
      numero: serie.numero,
      peso: serie.peso === '' ? null : Number(serie.peso),
      repeticiones: serie.repeticiones === '' ? null : Number(serie.repeticiones),
      rirRealizado: serie.rirRealizado === '' ? null : Number(serie.rirRealizado),
    })),
  })),
});

export default function MiEntrenamiento() {
  const [plan, setPlan] = useState({ id: null, nombre: '', activo: false, sesiones: [] });
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [cargando, setCargando] = useState(true);
  const persistRequestRef = useRef(0);

  const mergeSesiones = (prevPlan, siguientePlan) => {
    if (!prevPlan || !siguientePlan) return prevPlan;

    const sesionesMap = new Map((siguientePlan.sesiones ?? []).map((sesion) => [String(sesion.id), sesion]));
    const mergedSesiones = (prevPlan.sesiones ?? []).map((sesion) => {
      const actualizada = sesionesMap.get(String(sesion.id));
      return actualizada ?? sesion;
    });

    const nuevasSesiones = (siguientePlan.sesiones ?? []).filter(
      (sesion) => !mergedSesiones.some((actual) => String(actual.id) === String(sesion.id)),
    );

    return {
      ...prevPlan,
      ...siguientePlan,
      sesiones: [...mergedSesiones, ...nuevasSesiones],
    };
  };

  const cargarPlan = async () => {
    try {
      const datos = await obtenerMiEntrenamiento();
      setPlan(datos);
      setSelectedSessionId((prev) => prev ?? datos.sesiones[0]?.id ?? null);
    } catch (error) {
      setPlan({ id: null, nombre: '', activo: false, sesiones: [] });
      setFeedback({ type: 'error', message: error.message });
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarPlan();
  }, []);

  const selectedSession = useMemo(() => {
    if (!plan.activo) return null;
    return plan.sesiones.find((session) => String(session.id) === String(selectedSessionId)) ?? plan.sesiones[0] ?? null;
  }, [plan.activo, plan.sesiones, selectedSessionId]);

  const pendingExercises = selectedSession?.ejercicios.filter((ejercicio) => !esEjercicioCompleto(ejercicio)) ?? [];
  const sessionCompleta = Boolean(selectedSession && selectedSession.ejercicios.length && selectedSession.ejercicios.every(esEjercicioCompleto));

  const iniciarSesion = async (sessionId) => {
    setFeedback(null);

    try {
      const iniciada = await iniciarSesionEntrenamiento(sessionId);
      setPlan((prev) => mergeSesiones(prev, iniciada));
      setSelectedSessionId(sessionId);
      return iniciada;
    } catch (error) {
      setFeedback({ type: 'error', message: error.message });
      throw error;
    }
  };

  const persistirSesion = async (sesion) => {
    if (!sesion) return null;

    const requestId = ++persistRequestRef.current;

    let trabajo = sesion;
    if (!sesion.entrenamientoId) {
      const iniciada = await iniciarSesion(sesion.id);
      const creada = iniciada.sesiones.find((item) => String(item.id) === String(sesion.id));
      if (!creada) throw new Error('No se pudo iniciar la sesión de entrenamiento.');
      trabajo = creada;
      if (requestId !== persistRequestRef.current) return null;
    }

    const payload = crearPayload(trabajo);
    const actualizado = await guardarEntrenamiento(trabajo.entrenamientoId, payload);

    if (requestId !== persistRequestRef.current) {
      return actualizado;
    }

    setPlan((prev) => mergeSesiones(prev, actualizado));
    return actualizado;
  };

  const actualizarSerie = async (sessionId, ejercicioId, numeroSerie, campo, valor) => {
    let siguientePlan;
    setPlan((prev) => {
      siguientePlan = {
        ...prev,
        sesiones: prev.sesiones.map((session) => {
          if (session.id !== sessionId) return session;
          return {
            ...session,
            ejercicios: session.ejercicios.map((ejercicio) => {
              if ((ejercicio.ejercicioPlanId ?? ejercicio.id) !== ejercicioId) return ejercicio;
              return {
                ...ejercicio,
                series: ejercicio.series.map((serie) => {
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
                }),
              };
            }),
          };
        }),
      };
      return siguientePlan;
    });

    const sesionActualizada = siguientePlan?.sesiones.find((session) => String(session.id) === String(sessionId));
    setFeedback(null);

    try {
      await persistirSesion(sesionActualizada);
    } catch (error) {
      setFeedback({ type: 'error', message: error.message });
    }
  };

  const actualizarNota = async (sessionId, ejercicioId, nota) => {
    let siguientePlan;
    setPlan((prev) => {
      siguientePlan = {
        ...prev,
        sesiones: prev.sesiones.map((session) => {
          if (session.id !== sessionId) return session;
          return {
            ...session,
            ejercicios: session.ejercicios.map((ejercicio) =>
              (ejercicio.ejercicioPlanId ?? ejercicio.id) === ejercicioId ? { ...ejercicio, nota } : ejercicio,
            ),
          };
        }),
      };
      return siguientePlan;
    });

    const sesionActualizada = siguientePlan?.sesiones.find((session) => String(session.id) === String(sessionId));
    setFeedback(null);

    try {
      await persistirSesion(sesionActualizada);
    } catch (error) {
      setFeedback({ type: 'error', message: error.message });
    }
  };

  const finalizarSesion = async () => {
    if (!selectedSession || !selectedSession.entrenamientoId) {
      setFeedback({ type: 'error', message: 'Primero iniciá la sesión para poder finalizarla.' });
      return;
    }

    try {
      const persistido = await persistirSesion(selectedSession);
      const sesionPersistida = persistido?.sesiones?.find((session) => String(session.id) === String(selectedSession.id)) ?? selectedSession;
      const pendientes = sesionPersistida.ejercicios.filter((ejercicio) => !esEjercicioCompleto(ejercicio)).map((ejercicio) => ejercicio.nombre);

      if (pendientes.length > 0) {
        setFeedback({
          type: 'error',
          message: `Completá los siguientes ejercicios antes de finalizar: ${pendientes.join(', ')}.`,
        });
        return;
      }

      const actualizado = await finalizarEntrenamiento(sesionPersistida.entrenamientoId);
      setPlan((prev) => mergeSesiones(prev, actualizado));
      setFeedback({ type: 'success', message: 'Sesión completada correctamente.' });
    } catch (error) {
      setFeedback({ type: 'error', message: error.message });
    }
  };

  if (cargando) {
    return <Panel className="p-5 text-sm text-ink-soft">Cargando entrenamiento...</Panel>;
  }

  if (!plan.activo) {
    return (
      <div className="animate-fade-in">
        <div className="mb-6 space-y-2">
          <div className="flex items-center justify-between gap-3">
            <h1 className="text-2xl font-bold text-ink">Mi entrenamiento</h1>
            <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-primary">Sin plan</span>
          </div>
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
        <div className="flex items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-ink">Mi entrenamiento</h1>
          <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-primary">Activo</span>
        </div>
        <p className="text-sm text-ink-soft">Completá los ejercicios de tu sesión y registrá tus datos</p>
      </div>

      <SelectorSesiones sesiones={plan.sesiones} selectedId={selectedSessionId} onSelect={setSelectedSessionId} />

      {selectedSession && (
        <>
          <SesionResumen planNombre={plan.nombre} sesion={selectedSession} />

          {!selectedSession.entrenamientoId && (
            <Panel className="mb-5 border-warning bg-warning-bg p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-ink">Esta sesión todavía no fue iniciada</p>
                  <p className="mt-1 text-xs text-ink-soft">Iniciá la sesión para guardar y completar tus series.</p>
                </div>
                <Button type="button" variant="primary" size="sm" onClick={() => iniciarSesion(selectedSession.id)}>
                  Iniciar sesión
                </Button>
              </div>
            </Panel>
          )}

          {selectedSession.ejercicios.map((ejercicio, index) => (
            <EjercicioCard
              key={`${selectedSession.id}-${ejercicio.ejercicioPlanId ?? ejercicio.id ?? `${index}-${ejercicio.nombre}`}`}
              ejercicio={ejercicio}
              onUpdateSerie={(numeroSerie, campo, valor) => actualizarSerie(selectedSession.id, ejercicio.ejercicioPlanId ?? ejercicio.id, numeroSerie, campo, valor)}
              onUpdateNota={(nota) => actualizarNota(selectedSession.id, ejercicio.ejercicioPlanId ?? ejercicio.id, nota)}
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
