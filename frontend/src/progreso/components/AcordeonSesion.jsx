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

const crearEstadoEjercicio = (ejercicio = {}) => Array.from({ length: Number(ejercicio.series || 1) }, () => ({
  peso: '',
  repeticiones: '',
  rir: '',
  nota: '',
}));

/** Tarjeta de una sesión: al tocarla se expande y muestra sus ejercicios; al tocarla de nuevo se contrae. */
export default function AcordeonSesion({ sesion, planId, semanaNumero }) {
  const [abierta, setAbierta] = useState(false);
  const [registroId, setRegistroId] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [estado, setEstado] = useState(() => Object.fromEntries((sesion.ejercicios || []).map((ejercicio, idx) => [idx, crearEstadoEjercicio(ejercicio)])));
  const panelId = useId();
  const { tono, texto, icono: Icono } = ESTADOS[sesion.estado];

  const actualizarCampo = (index, serieIndex, campo, valor) => {
    setEstado((actual) => ({
      ...actual,
      [index]: (actual[index] || []).map((serie, idx) => (
        idx === serieIndex ? { ...serie, [campo]: valor } : serie
      )),
    }));
  };

  const guardarEjercicio = async (index) => {
    const series = (estado[index] || []).map((serie, serieIndex) => ({
      orden: serieIndex + 1,
      peso: serie.peso === '' ? null : Number(serie.peso),
      repeticiones: serie.repeticiones === '' ? null : Number(serie.repeticiones),
      rir: serie.rir === '' ? null : Number(serie.rir),
      nota: serie.nota?.trim() || '',
    }));

    const body = { series };

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

            <div className="space-y-3">
              {Array.from({ length: ejercicio.series || 1 }, (_, serieIndex) => (
                <div key={`${ejercicio.nombre}-serie-${serieIndex + 1}`} className="grid grid-cols-2 gap-2 rounded-lg border border-line bg-white/80 p-2 md:grid-cols-4">
                  <div className="flex items-center justify-center rounded-md bg-soft px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-ink-muted">
                    Serie {serieIndex + 1}
                  </div>
                  <label className="flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
                    Peso (kg)
                    <input
                      type="number"
                      step="0.5"
                      min="0"
                      value={estado[i]?.[serieIndex]?.peso ?? ''}
                      onChange={(event) => actualizarCampo(i, serieIndex, 'peso', event.target.value)}
                      className="rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                      placeholder="0"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[11px] font-medium text-ink-soft">
                    Reps
                    <input
                      type="number"
                      min="1"
                      value={estado[i]?.[serieIndex]?.repeticiones ?? ''}
                      onChange={(event) => actualizarCampo(i, serieIndex, 'repeticiones', event.target.value)}
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
                      value={estado[i]?.[serieIndex]?.rir ?? ''}
                      onChange={(event) => actualizarCampo(i, serieIndex, 'rir', event.target.value)}
                      className="rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                      placeholder="2"
                    />
                  </label>
                </div>
              ))}

              <div className="flex items-end justify-between gap-3 pt-1">
                <label className="flex flex-1 flex-col gap-1 text-[11px] font-medium text-ink-soft">
                  Nota opcional
                  <textarea
                    rows="2"
                    value={estado[i]?.[0]?.nota ?? ''}
                    onChange={(event) => {
                      const siguiente = [...(estado[i] || [])];
                      const base = siguiente.map((serie, idx) => ({ ...serie, nota: idx === 0 ? event.target.value : serie.nota ?? '' }));
                      setEstado((actual) => ({ ...actual, [i]: base }));
                    }}
                    className="resize-none rounded-lg border border-line bg-white px-2 py-2 text-sm text-ink outline-none ring-0 focus:border-primary"
                    placeholder="Cómo te sentiste, esfuerzo, cambios, etc."
                  />
                </label>
                <Button type="button" variant="primary" size="sm" onClick={() => guardarEjercicio(i)} disabled={guardando} className="shrink-0">
                  <Save className="h-3.5 w-3.5" />
                  Guardar
                </Button>
              </div>
            </div>
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
