import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { CalendarCheck, CalendarDays, Check, ChevronDown, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Cargando, EmptyRow, Panel, Row, Table } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import { nombreCortoPlan, textoFechaCompleta, textoRangoPlan, tituloSesion } from '../services/progresoService';
import EstadoVacioPanel, { ContenidoVacio } from './EstadoVacioPanel';
import { useSesionAbierta } from './useSesionAbierta';

const COLUMNAS = [
  { label: 'Fecha', className: 'pl-4' },
  { label: 'Plan · Semana' },
  { label: 'Sesión' },
  { label: 'Ejercicios registrados' },
  { label: '', className: 'pr-4' },
];

const CONTROL = 'flex h-10 max-w-full items-center gap-2 rounded-lg border border-line bg-card px-3 text-sm';

const textoOpcion = (plan) => (plan.estado === 'ACTIVO' ? `${nombreCortoPlan(plan)} (actual)` : `${nombreCortoPlan(plan)} (${textoRangoPlan(plan)})`);

function EjerciciosRegistrados({ sesion }) {
  if (sesion.ejerciciosRegistrados < sesion.ejerciciosTotales) {
    return <Badge tone="warning" className="font-semibold">{sesion.ejerciciosRegistrados} / {sesion.ejerciciosTotales} · incompleta</Badge>;
  }
  return (
    <span className="inline-flex items-center gap-1.5 font-semibold tabular-nums">
      <Check className="h-4 w-4 text-success" /> {sesion.ejerciciosRegistrados} / {sesion.ejerciciosTotales}
    </span>
  );
}

/**
 * Vista "Historial de sesiones": las sesiones registradas del plan elegido, filtrables por fechas. Plan y fechas
 * quedan en la URL (?plan=&desde=&hasta=). Por defecto, el plan activo o, si no hay, el anterior más reciente.
 * Cada fila abre la sesión en el drawer.
 */
export default function HistorialSesiones() {
  const { consultas, textos } = useProgreso();
  const { abrir } = useSesionAbierta();
  const [params, setParams] = useSearchParams();
  const [planes, setPlanes] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlanes()
      .then((respuesta) => { if (vigente) setPlanes([respuesta.activo, ...respuesta.anteriores].filter(Boolean)); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas]);

  const plan = planes?.find((p) => String(p.id) === params.get('plan')) ?? planes?.[0];
  const planId = plan?.id;

  useEffect(() => {
    if (!planId) return undefined;
    let vigente = true;
    consultas.consultarSesiones(planId)
      .then((sesiones) => { if (vigente) setResultado({ planId, sesiones }); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas, planId]);

  if (error) return <ErrorConsulta error={error} />;

  if (!planes) return <Cargando texto={textos.cargandoSesiones} />;

  // Sin ningún plan no hay nada que filtrar: solo el estado vacío
  if (!plan) return <EstadoVacioPanel icono={CalendarCheck} {...textos.sinSesiones} />;

  const desde = params.get('desde') ?? '';
  const hasta = params.get('hasta') ?? '';
  const hayFiltros = params.has('plan') || desde || hasta;

  // Cambia un filtro y conserva los demás (y la sesión abierta, si la hay)
  const filtrar = (cambios) => setParams((actuales) => {
    const nuevos = new URLSearchParams(actuales);
    Object.entries(cambios).forEach(([clave, valor]) => (valor ? nuevos.set(clave, valor) : nuevos.delete(clave)));
    return nuevos;
  }, { replace: true });

  // Al cambiar de plan, lo anterior deja de valer hasta que llega la respuesta nueva
  const sesiones = resultado?.planId === planId ? resultado.sesiones : null;
  // Las fechas ISO se comparan bien como texto
  const visibles = sesiones?.filter((s) => (!desde || s.fecha >= desde) && (!hasta || s.fecha <= hasta));

  // Con un solo plan y nada registrado, los filtros no tienen sentido
  if (planes.length === 1 && sesiones?.length === 0) return <EstadoVacioPanel icono={CalendarCheck} {...textos.sinSesiones} />;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <label className={cx(CONTROL, 'relative min-w-0 pr-8 focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-primary')}>
          <span className="text-ink-soft">Plan:</span>
          <select
            value={planId}
            onChange={(e) => filtrar({ plan: e.target.value })}
            className="min-w-0 cursor-pointer appearance-none truncate bg-transparent font-semibold text-ink outline-none"
          >
            {planes.map((p) => <option key={p.id} value={p.id}>{textoOpcion(p)}</option>)}
          </select>
          <ChevronDown aria-hidden="true" className="pointer-events-none absolute right-2.5 h-4 w-4 text-ink-soft" />
        </label>

        <div className={cx(CONTROL, 'w-full sm:w-auto')}>
          <CalendarDays aria-hidden="true" className="h-4 w-4 text-ink-soft" />
          <span className="hidden text-ink-soft sm:inline">Fechas:</span>
          <input
            type="date"
            aria-label="Desde"
            value={desde}
            max={hasta || undefined}
            onChange={(e) => filtrar({ desde: e.target.value })}
            className={cx('min-w-0 flex-1 rounded bg-transparent font-semibold tabular-nums text-ink sm:flex-none', FOCUS)}
          />
          <span className="text-ink-muted">–</span>
          <input
            type="date"
            aria-label="Hasta"
            value={hasta}
            min={desde || undefined}
            onChange={(e) => filtrar({ hasta: e.target.value })}
            className={cx('min-w-0 flex-1 rounded bg-transparent font-semibold tabular-nums text-ink sm:flex-none', FOCUS)}
          />
        </div>

        {hayFiltros && (
          <button
            type="button"
            onClick={() => filtrar({ plan: null, desde: null, hasta: null })}
            className={cx('cursor-pointer rounded text-sm font-semibold text-primary hover:underline', FOCUS)}
          >
            Limpiar filtros
          </button>
        )}
      </div>

      <Panel className="p-5">
        {!sesiones && <Cargando texto={textos.cargandoSesiones} className="py-12" />}
        {sesiones?.length === 0 && <ContenidoVacio icono={CalendarCheck} {...textos.sinSesiones} className="min-h-[320px] px-6 py-12" />}
        {sesiones?.length > 0 && (
          <Table columns={COLUMNAS}>
            {visibles.length === 0 && <EmptyRow colSpan={COLUMNAS.length}>No hay sesiones registradas entre esas fechas.</EmptyRow>}
            {visibles.map((sesion) => (
              <Row key={sesion.id} onActivate={() => abrir(sesion.id)} title="Ver lo registrado en la sesión">
                <td className="py-3 pl-4 pr-3 font-semibold tabular-nums">{textoFechaCompleta(sesion.fecha)}</td>
                <td className="px-3">{nombreCortoPlan(plan)} · Semana {sesion.semana}</td>
                <td className="px-3">{tituloSesion(sesion.nombreSesion, sesion.numeroSesion)}</td>
                <td className="px-3"><EjerciciosRegistrados sesion={sesion} /></td>
                <td className="pl-3 pr-4 text-right">
                  <span className="inline-flex items-center gap-0.5 font-semibold text-primary">
                    Ver detalle <ChevronRight className="h-4 w-4" />
                  </span>
                </td>
              </Row>
            ))}
          </Table>
        )}
      </Panel>
    </div>
  );
}
