import { useId, useState } from 'react';
import { Check, ChevronDown, Clock, Save, SquareCheckBig } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Button, Panel } from '../../shared/components/ui';
import { completarSesion, guardarEjercicioSesion, iniciarSesion, textoDetalleSesion } from '../services/progresoService';
import EjercicioIndicado from './EjercicioIndicado';

const ESTADOS = {
  REGISTRADA: { tono: 'success', texto: 'Registrada', icono: Check },
  PENDIENTE: { tono: 'neutral', texto: 'Pendiente', icono: Clock },
  NO_REGISTRADA: { tono: 'neutral', texto: 'No registrada' },
};

/** Tarjeta de una sesión: al tocarla se expande y muestra sus ejercicios; al tocarla de nuevo se contrae. */
export default function AcordeonSesion({ sesion, planId, semanaNumero }) {
  const [abierta, setAbierta] = useState(false);
  const [registroId, setRegistroId] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [estado, setEstado] = useState(() => Object.fromEntries((sesion.ejercicios || []).map((_, idx) => [idx, {
    peso: '',
    repeticionesRealizadas: '',
    rir: '',
    notas: '',
  }])));
  const panelId = useId();
  const { tono, texto, icono: Icono } = ESTADOS[sesion.estado];

  const actualizarCampo = (index, campo, valor) => {
    setEstado((actual) => ({
      ...actual,
      [index]: { ...(actual[index] || {}), [campo]: valor },
    }));
  };

  const guardarEjercicio = async (index) => {
    const entrada = estado[index] || {};
    const body = {
      series: [
        {
          orden: 1,
          peso: entrada.peso === '' ? null : Number(entrada.peso),
          repeticiones: entrada.repeticionesRealizadas === '' ? null : Number(entrada.repeticionesRealizadas),
          rir: entrada.rir === '' ? null : Number(entrada.rir),
          nota: entrada.notas?.trim() || '',
        },
      ],
    };

    try {
      setGuardando(true);
      setError('');
      let sessionId = registroId;
      if (!sessionId) {
        const respuesta = await iniciarSesion(planId, semanaNumero, sesion.numero);
        sessionId = respuesta.id;
        setRegistroId(sessionId);
      }
      await guardarEjercicioSesion(sessionId, index + 1, body);
    } catch (err) {
      setError(err?.message || 'No se pudo guardar el ejercicio');
    } finally {
      setGuardando(false);
    }
  };

  const cerrarSesion = async () => {
    if (!registroId) return;
    try {
      setGuardando(true);
      setError('');
      await completarSesion(registroId);
    } catch (err) {
      setError(err?.message || 'No se pudo cerrar la sesión');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Panel as="article">
      <button
        type="button"
        onClick={() => setAbierta((actual) => !actual)}
        aria-expanded={abierta}
        aria-controls={panelId}
        className={cx('flex min-h-11 w-full cursor-pointer items-center gap-2.5 rounded-lg py-3 pl-4 pr-3 text-left', FOCUS)}
      >
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="text-sm font-bold text-ink">Sesión {sesion.numero}</span>
          <span className="text-xs text-ink-soft">{textoDetalleSesion(sesion)}</span>
        </span>
        <Badge tone={tono} className="shrink-0 gap-1">
          {Icono && <Icono className="h-3 w-3" />}
          {texto}
        </Badge>
        <ChevronDown className={cx('h-5 w-5 shrink-0 text-ink-muted transition-transform duration-200', abierta && 'rotate-180')} />
      </button>

      <div id={panelId} hidden={!abierta} className="space-y-3 border-t border-line px-3 pb-3 pt-3">
        {sesion.ejercicios.map((ejercicio, i) => (
          <div key={`${ejercicio.nombre}-${i}`} className="rounded-xl border border-line bg-soft/60 p-3">
            <div className="mb-3 flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-ink">{ejercicio.nombre}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-[11px] text-ink-soft">
                  <span>{ejercicio.series} series</span>
                  {ejercicio.rirObjetivo != null && <span>RIR objetivo {ejercicio.rirObjetivo}</span>}
                  {ejercicio.repeticionesMax != null ? <span>{ejercicio.repeticiones}–{ejercicio.repeticionesMax} rep.</span> : <span>{ejercicio.repeticiones} rep.</span>}
                </div>
              </div>
              <span className="rounded-full bg-thead/70 px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-ink-muted">#{i + 1}</span>
            </div>

            <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
              <label className="flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
                Peso (kg)
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  value={estado[i]?.peso ?? ''}
                  onChange={(event) => actualizarCampo(i, 'peso', event.target.value)}
                  className="rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                  placeholder="0"
                />
              </label>
              <label className="flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
                Reps
                <input
                  type="number"
                  min="1"
                  value={estado[i]?.repeticionesRealizadas ?? ''}
                  onChange={(event) => actualizarCampo(i, 'repeticionesRealizadas', event.target.value)}
                  className="rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                  placeholder="8"
                />
              </label>
              <label className="flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
                RIR
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={estado[i]?.rir ?? ''}
                  onChange={(event) => actualizarCampo(i, 'rir', event.target.value)}
                  className="rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                  placeholder="2"
                />
              </label>
              <div className="flex items-end">
                <Button type="button" variant="primary" size="sm" onClick={() => guardarEjercicio(i)} disabled={guardando}>
                  <Save className="h-3.5 w-3.5" />
                  Guardar
                </Button>
              </div>
            </div>

            <label className="mt-3 flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
              Nota opcional
              <textarea
                rows="2"
                value={estado[i]?.notas ?? ''}
                onChange={(event) => actualizarCampo(i, 'notas', event.target.value)}
                className="resize-none rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                placeholder="Cómo te sentiste, esfuerzo, cambios, etc."
              />
            </label>
          </div>
        ))}

        {error && <p className="text-sm text-error">{error}</p>}

        <div className="flex justify-end pt-1">
          <Button type="button" variant="secondary" onClick={cerrarSesion} disabled={guardando || !registroId}>
            <SquareCheckBig className="h-4 w-4" />
            Cerrar sesión
          </Button>
        </div>
      </div>
    </Panel>
  );
}
