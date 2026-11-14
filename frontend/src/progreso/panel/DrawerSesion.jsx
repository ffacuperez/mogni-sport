import { useEffect, useState } from 'react';
import { Check, ChevronDown, Info, MessageSquare } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Cargando, Drawer } from '../../shared/components/ui';
import { textoRepeticiones } from '../../plantilla/services/plantillasService';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import { serieDeReferencia, textoFechaCompleta, tituloSesion } from '../services/progresoService';
import { useSesionAbierta } from './useSesionAbierta';

const numero = (valor, decimales = 2) => (valor == null ? '—' : Number(valor).toLocaleString('es-AR', { maximumFractionDigits: decimales }));

const TH_INDICADO = 'bg-thead px-2 text-center font-medium text-ink-soft';
const TH_REALIZADO = 'bg-error-bg px-2 text-center font-medium text-primary';
const TD_INDICADO = 'px-2 py-3 text-center tabular-nums text-ink-soft';
const TD_REALIZADO = 'bg-error-bg/40 px-2 py-3 text-center text-[15px] font-semibold tabular-nums text-ink';

/**
 * Lo realizado de un ejercicio sale de la serie de referencia, la de más peso (MOGNI-269), que es de donde sale su
 * 1RM. Los registros anteriores a HU-269 no tienen series: quedan el peso y el RIR que se anotaron, sin repeticiones.
 */
function realizado(ejercicio) {
  const series = ejercicio.seriesRealizadas.map((s) => ({ ...s, peso: Number(s.peso), repeticiones: Number(s.repeticiones), rir: Number(s.rir) }));
  const referencia = series.length > 0 ? serieDeReferencia(series) : null;
  return {
    series,
    peso: referencia?.peso ?? ejercicio.kgUsados,
    repeticiones: referencia?.repeticiones ?? null,
    rir: referencia?.rir ?? ejercicio.rirReal,
  };
}

/** El RIR real; si se aleja más de un punto del objetivo, con un aviso de cuánto. */
function RirReal({ rir, diferencia }) {
  const lejos = diferencia != null && Math.abs(diferencia) > 1;
  return (
    <span className="inline-flex items-center gap-1">
      {numero(rir, 0)}
      {lejos && (
        <span className="inline-flex items-center gap-1 rounded-full bg-warning-bg px-1.5 py-0.5 text-[10px] font-semibold text-warning-text">
          <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-warning" />
          {diferencia > 0 ? '+' : '−'}{Math.abs(diferencia)}
          <span className="sr-only">vs. el objetivo</span>
        </span>
      )}
    </span>
  );
}

function FilaEjercicio({ ejercicio }) {
  const [abierta, setAbierta] = useState(false);
  const { series, peso, repeticiones, rir } = realizado(ejercicio);
  const desplegable = series.length > 0;

  return (
    <>
      <tr className="border-t border-line">
        <td className="py-3 pr-2 font-semibold text-ink">
          {desplegable ? (
            <button
              type="button"
              onClick={() => setAbierta((actual) => !actual)}
              aria-expanded={abierta}
              className={cx('flex cursor-pointer items-center gap-1 rounded text-left', FOCUS)}
            >
              <ChevronDown aria-hidden="true" className={cx('h-4 w-4 shrink-0 text-ink-muted transition-transform', abierta && 'rotate-180')} />
              {ejercicio.nombre}
              <span className="sr-only">{abierta ? ', ocultar series' : ', ver series'}</span>
            </button>
          ) : ejercicio.nombre}
        </td>
        <td className={TD_INDICADO}>{ejercicio.series}×{textoRepeticiones(ejercicio.repeticiones, ejercicio.repeticionesMax)}</td>
        <td className={TD_INDICADO}>{ejercicio.rirObjetivo ?? '—'}</td>
        <td className={TD_INDICADO}>{ejercicio.descansoMinutos != null ? `${Math.round(Number(ejercicio.descansoMinutos) * 60)} s` : '—'}</td>
        <td className={TD_REALIZADO}>{numero(peso)}</td>
        <td className={TD_REALIZADO}>{numero(repeticiones, 0)}</td>
        <td className={TD_REALIZADO}><RirReal rir={rir} diferencia={ejercicio.diferenciaRir} /></td>
        <td className={TD_REALIZADO}>{Number(ejercicio.rmEstimado) > 0 ? numero(ejercicio.rmEstimado, 0) : '—'}</td>
      </tr>
      {abierta && series.map((serie) => (
        <tr key={serie.orden} className="text-xs text-ink-soft">
          <td colSpan={4} className="py-1.5 pl-5 pr-2">Serie {serie.orden}</td>
          <td className="bg-error-bg/40 px-2 py-1.5 text-center tabular-nums">{numero(serie.peso)}</td>
          <td className="bg-error-bg/40 px-2 py-1.5 text-center tabular-nums">{numero(serie.repeticiones, 0)}</td>
          <td className="bg-error-bg/40 px-2 py-1.5 text-center tabular-nums">{numero(serie.rir, 0)}</td>
          <td className="bg-error-bg/40 px-2 py-1.5" />
        </tr>
      ))}
      {ejercicio.notas && (
        <tr>
          <td colSpan={8} className="pb-3">
            <p className="flex items-start gap-2 text-[13px] italic text-ink-soft">
              <MessageSquare aria-hidden="true" className="mt-0.5 h-3.5 w-3.5 shrink-0" />
              “{ejercicio.notas}”
            </p>
          </td>
        </tr>
      )}
    </>
  );
}

function Leyenda() {
  return (
    <div className="flex flex-col gap-1.5 rounded-lg bg-row-hover px-4 py-3 text-xs text-ink-soft">
      <p className="flex items-center gap-2">
        <span aria-hidden="true" className="h-1.5 w-1.5 shrink-0 rounded-full bg-warning" />
        RIR real a más de 1 punto del objetivo
      </p>
      <p className="flex items-center gap-2">
        <Info aria-hidden="true" className="h-3.5 w-3.5 shrink-0" />
        1RM estimado con Epley ajustado por RIR: peso × (1 + (reps + RIR) / 30)
      </p>
    </div>
  );
}

function Contenido({ sesion }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[480px] text-[13px]">
        <thead>
          <tr className="h-7 text-[11px] uppercase tracking-wide">
            <th scope="col" className="w-[28%]" />
            <th scope="colgroup" colSpan={3} className={cx(TH_INDICADO, 'rounded-tl-lg font-semibold')}>Indicado</th>
            <th scope="colgroup" colSpan={4} className={cx(TH_REALIZADO, 'rounded-tr-lg font-semibold')}>Realizado</th>
          </tr>
          <tr className="h-9 text-xs">
            <th scope="col" className="pr-2 text-left font-medium text-ink-soft">Ejercicio</th>
            <th scope="col" className={TH_INDICADO}>Series x reps</th>
            <th scope="col" className={TH_INDICADO}>RIR obj.</th>
            <th scope="col" className={TH_INDICADO}>Desc.</th>
            <th scope="col" className={TH_REALIZADO}>Peso (kg)</th>
            <th scope="col" className={TH_REALIZADO}>Reps</th>
            <th scope="col" className={TH_REALIZADO}>RIR real</th>
            <th scope="col" className={TH_REALIZADO}>1RM est.</th>
          </tr>
        </thead>
        <tbody>
          {sesion.ejercicios.map((ejercicio, i) => <FilaEjercicio key={i} ejercicio={ejercicio} />)}
        </tbody>
      </table>
    </div>
  );
}

/** Lo registrado en una sesión, lado a lado con lo indicado, en un panel lateral. Se abre con ?sesion=<id>. */
function DrawerSesion({ sesionId, alumno, onClose }) {
  const { consultas } = useProgreso();
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarSesion(sesionId)
      .then((sesion) => { if (vigente) setResultado({ sesionId, sesion }); })
      .catch((error) => { if (vigente) setResultado({ sesionId, error }); });
    return () => { vigente = false; };
  }, [consultas, sesionId]);

  // Mientras llega otra sesión, la anterior deja de valer
  const actual = resultado?.sesionId === sesionId ? resultado : null;
  const sesion = actual?.sesion;
  const hechos = sesion?.ejercicios.filter((e) => e.completo).length;

  const header = sesion ? (
    <>
      <h2 className="text-xl font-semibold text-ink">
        {tituloSesion(sesion.nombreSesion, sesion.numeroSesion)} · Semana {sesion.semana} · {textoFechaCompleta(sesion.fecha)}
      </h2>
      <p className="mt-0.5 text-sm text-ink-soft">{alumno.nombre} {alumno.apellido} · {sesion.plan.nombre}</p>
      <div className="mt-3 flex flex-wrap gap-2">
        <Badge tone="success" className="gap-1"><Check className="h-3 w-3" /> Registrada</Badge>
        <Badge>{hechos} / {sesion.ejercicios.length} ejercicios</Badge>
      </div>
    </>
  ) : <h2 className="text-xl font-semibold text-ink">Detalle de la sesión</h2>;

  return (
    <Drawer label="Detalle de la sesión" header={header} footer={sesion && <Leyenda />} onClose={onClose}>
      {actual?.error && <ErrorConsulta error={actual.error} />}
      {!actual && <Cargando texto="Cargando la sesión" />}
      {sesion && <Contenido sesion={sesion} />}
    </Drawer>
  );
}

/** El drawer de la sesión que indica la URL, si hay una. */
export default function DrawerSesionRuta({ alumno }) {
  const { sesionId, cerrar } = useSesionAbierta();
  if (!sesionId) return null;
  return <DrawerSesion sesionId={sesionId} alumno={alumno} onClose={cerrar} />;
}
