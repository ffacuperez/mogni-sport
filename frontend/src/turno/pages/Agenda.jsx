import { useEffect, useRef, useState } from 'react';
import { CalendarDays, CalendarX, ChevronLeft, ChevronRight, CircleAlert } from 'lucide-react';

import { Button, Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import AgendaDia from '../components/AgendaDia';
import AgendaSemana from '../components/AgendaSemana';
import {
  lunesDe,
  obtenerAgendaDia,
  obtenerAgendaSemana,
  sumarDias,
  textoFechaLarga,
  textoSemana,
} from '../services/agendaService';

const botonClaro = cx('inline-flex h-10 cursor-pointer items-center justify-center rounded-lg border border-line bg-card px-3 text-sm font-medium text-ink transition-colors hover:bg-row-hover', FOCUS);

/** Turno que se selecciona al abrir un día: el que está en curso, si no el próximo, si no el primero. */
function turnoInicial(agenda) {
  const turnos = agenda.turnos;
  return (turnos.find(t => t.enCurso)
    ?? turnos.find(t => t.estado !== 'PASADO' && t.estado !== 'CANCELADO')
    ?? turnos[0])?.horaInicio ?? null;
}

/**
 * MOGNI-257: agenda de turnos del personal. Vista Día (alumnos de cada turno) y vista Semana (ocupación).
 * Se filtra por fecha con las flechas, el botón "Hoy" o el calendario. "Hoy" sale del backend.
 */
export default function Agenda() {
  const [vista, setVista] = useState('dia');
  // null hasta la primera respuesta: el backend decide qué día es hoy
  const [fecha, setFecha] = useState(null);
  const [hoy, setHoy] = useState(null);
  // Última respuesta y último error, cada uno con la vista y fecha que se pidieron
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState(null);
  const [seleccion, setSeleccion] = useState(null);
  const inputFecha = useRef(null);

  const pedido = `${vista}|${fecha}`;
  const cargando = datos?.pedido !== pedido && error?.pedido !== pedido;

  useEffect(() => {
    let vigente = true;
    (vista === 'dia' ? obtenerAgendaDia(fecha) : obtenerAgendaSemana(fecha))
      .then((respuesta) => {
        // Si mientras tanto se pidió otra fecha o vista, esta respuesta ya no sirve
        if (!vigente) return;
        setHoy(respuesta.hoy);
        setDatos({ pedido, vista, respuesta });
        if (vista === 'dia') {
          setSeleccion(prev => (respuesta.turnos.some(t => t.horaInicio === prev) ? prev : turnoInicial(respuesta)));
        }
      })
      .catch((err) => { if (vigente) setError({ pedido, mensaje: err.message }); });
    return () => { vigente = false; };
  }, [vista, fecha, pedido]);

  const fechaActual = fecha ?? hoy;
  // En otro día se vuelve a elegir el turno inicial; si la fecha no cambia, queda todo como está
  const cambiarFecha = (nueva) => {
    if (!nueva || nueva === fechaActual) return;
    setSeleccion(null);
    setFecha(nueva);
  };
  const mover = (sentido) => {
    if (fechaActual) cambiarFecha(sumarDias(fechaActual, sentido * (vista === 'dia' ? 1 : 7)));
  };
  const abrirCalendario = () => {
    try {
      inputFecha.current?.showPicker();
    } catch {
      inputFecha.current?.focus();
    }
  };
  // Desde la vista Semana: abre el día con ese turno seleccionado
  const abrirTurno = (fechaTurno, horaInicio) => {
    setSeleccion(horaInicio);
    setFecha(fechaTurno);
    setVista('dia');
  };

  let titulo = '…';
  if (fechaActual) titulo = vista === 'dia' ? textoFechaLarga(fechaActual) : textoSemana(lunesDe(fechaActual), sumarDias(lunesDe(fechaActual), 6));
  const vistaLista = datos?.vista === vista ? datos.respuesta : null;

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow="Operación diaria"
        title="Agenda"
        actions={
          <>
            <div role="group" aria-label="Vista" className="inline-flex h-10 rounded-lg bg-thead/60 p-1">
              {[['dia', 'Día'], ['semana', 'Semana']].map(([valor, texto]) => (
                <button
                  key={valor}
                  type="button"
                  aria-pressed={vista === valor}
                  onClick={() => setVista(valor)}
                  className={cx(
                    'cursor-pointer rounded-md px-4 text-sm font-medium transition-colors',
                    vista === valor ? 'bg-card font-semibold text-primary shadow-card' : 'text-ink-soft hover:text-ink',
                    FOCUS,
                  )}
                >
                  {texto}
                </button>
              ))}
            </div>

            <div className="inline-flex h-10 items-center gap-1 rounded-lg border border-line bg-card px-1">
              <button type="button" onClick={() => mover(-1)} aria-label={vista === 'dia' ? 'Día anterior' : 'Semana anterior'}
                className={cx('cursor-pointer rounded-md p-1.5 text-ink-soft hover:bg-row-hover hover:text-ink', FOCUS)}>
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="min-w-44 px-1 text-center text-sm font-semibold text-ink" aria-live="polite">{titulo}</span>
              <button type="button" onClick={() => mover(1)} aria-label={vista === 'dia' ? 'Día siguiente' : 'Semana siguiente'}
                className={cx('cursor-pointer rounded-md p-1.5 text-ink-soft hover:bg-row-hover hover:text-ink', FOCUS)}>
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>

            <button type="button" onClick={() => cambiarFecha(hoy)} disabled={!hoy} className={botonClaro}>Hoy</button>

            <span className="relative inline-flex">
              <button type="button" onClick={abrirCalendario} aria-label="Elegir fecha" className={botonClaro}>
                <CalendarDays className="h-4 w-4" />
              </button>
              <input
                ref={inputFecha}
                type="date"
                tabIndex={-1}
                aria-hidden="true"
                value={fechaActual ?? ''}
                onChange={(e) => cambiarFecha(e.target.value)}
                className="pointer-events-none absolute inset-0 opacity-0"
              />
            </span>

            {vista === 'dia' && (
              // TODO HU-258 (cancelar turnos): sin lógica ni endpoint en MOGNI-257
              <Button variant="secondary" disabled title="Disponible próximamente" className="h-10">
                <CalendarX className="h-4 w-4" /> Cancelar día
              </Button>
            )}
          </>
        }
      />

      {error?.pedido === pedido && (
        <div role="alert" className="mb-6 flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error.mensaje}</span>
        </div>
      )}

      {!vistaLista && cargando && <Cargando texto="Cargando agenda" />}

      {vistaLista && (
        <div className={cx('relative transition-opacity', cargando && 'opacity-60')} aria-busy={cargando}>
          {cargando && <div className="loading-bar absolute inset-x-0 -top-3 h-0.5" role="progressbar" aria-label="Cargando" />}
          {vista === 'dia' ? (
            <AgendaDia agenda={vistaLista} seleccion={seleccion} onSeleccionar={setSeleccion} />
          ) : (
            <Panel className="p-4 sm:p-5">
              <AgendaSemana semana={vistaLista} onAbrirTurno={abrirTurno} />
            </Panel>
          )}
        </div>
      )}
    </div>
  );
}
