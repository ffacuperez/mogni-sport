import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { CalendarDays, CalendarX, ChevronLeft, ChevronRight, CircleAlert } from 'lucide-react';

import { Button, Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import AgendaDia from '../components/AgendaDia';
import AgendaSemana from '../components/AgendaSemana';
import FichaAlumnoPanel from '../components/FichaAlumnoPanel';
import {
  cancelarDiaAgenda,
  cancelarTurnoAgenda,
  lunesDe,
  obtenerAgendaDia,
  obtenerAgendaSemana,
  reactivarTurno,
  resumenCancelarDia,
  resumenCancelarTurno,
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
export default function Agenda({ esPropietario, alumnos, alumnosCargados }) {
  const [vista, setVista] = useState('dia');
  // null hasta la primera respuesta: el backend decide qué día es hoy
  const [fecha, setFecha] = useState(null);
  const [hoy, setHoy] = useState(null);
  // Última respuesta y último error, cada uno con la vista y fecha que se pidieron
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState(null);
  const [seleccion, setSeleccion] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);
  const [cancelando, setCancelando] = useState(false);
  // Turno cancelado que el propietario está por reactivar (pide confirmación antes de avisar a los alumnos)
  const [reactivacion, setReactivacion] = useState(null);
  const [reactivando, setReactivando] = useState(false);
  const [version, setVersion] = useState(0);
  const [mensajeExito, setMensajeExito] = useState(null);
  // MOGNI-260: alumno del turno cuya ficha rápida está abierta (no cambia el turno seleccionado)
  const [alumnoFicha, setAlumnoFicha] = useState(null);
  const inputFecha = useRef(null);

  const pedido = `${vista}|${fecha}|${version}`;
  const cargando = datos?.pedido !== pedido && error?.pedido !== pedido;

  useEffect(() => {
    let vigente = true;
    (vista === 'dia' ? obtenerAgendaDia(fecha) : obtenerAgendaSemana(fecha))
      .then((respuesta) => {
        // Si mientras tanto se pidió otra fecha o vista, esta respuesta ya no sirve
        if (!vigente) return;
        setError(null);
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
  const turnosCancelables = (agenda) => agenda.turnos.filter(
    turno => turno.estado !== 'PASADO' && turno.estado !== 'CANCELADO' && !turno.enCurso,
  );
  const pedirCancelacionTurno = async (turno) => {
    setMensajeExito(null);
    setConfirmacion({
      tipo: 'turno',
      fecha: vistaLista.fecha,
      horaInicio: turno.horaInicio,
      cantidadTurnos: 1,
      reservas: null,
      cargando: true,
      error: null,
      tipoError: null,
    });
    try {
      const resumen = await resumenCancelarTurno(vistaLista.fecha, turno.horaInicio);
      setConfirmacion(actual => actual ? { ...actual, ...resumen, cargando: false, error: null, tipoError: null } : actual);
    } catch (err) {
      setConfirmacion(actual => actual
        ? { ...actual, cargando: false, tipoError: 'resumen', error: `${err.message}${err.status ? ` (HTTP ${err.status})` : ''}` }
        : actual);
    }
  };
  const pedirCancelacionDia = async () => {
    setMensajeExito(null);
    setConfirmacion({
      tipo: 'dia',
      fecha: vistaLista.fecha,
      reservas: null,
      cargando: true,
      error: null,
      tipoError: null,
    });
    try {
      const resumen = await resumenCancelarDia(vistaLista.fecha);
      setConfirmacion(actual => actual ? { ...actual, ...resumen, cargando: false, error: null, tipoError: null } : actual);
    } catch (err) {
      setConfirmacion(actual => actual
        ? { ...actual, cargando: false, tipoError: 'resumen', error: `${err.message}${err.status ? ` (HTTP ${err.status})` : ''}` }
        : actual);
    }
  };
  const confirmarCancelacion = async () => {
    if (!confirmacion || cancelando) return;
    setCancelando(true);
    try {
      const resultado = confirmacion.tipo === 'dia'
        ? await cancelarDiaAgenda(confirmacion.fecha)
        : await cancelarTurnoAgenda(confirmacion.fecha, confirmacion.horaInicio);
      const turnosText = resultado.turnosCancelados === 1 ? '1 turno' : `${resultado.turnosCancelados} turnos`;
      const reservasText = resultado.reservasCanceladas === 1 ? '1 reserva' : `${resultado.reservasCanceladas} reservas`;
      setMensajeExito(`Se cancelaron ${turnosText} y ${reservasText}. Se acreditaron ${resultado.clasesExtraOtorgadas} clases extra.`);
      setConfirmacion(null);
      setError(null);
      setVersion(actual => actual + 1);
    } catch (err) {
      setConfirmacion(actual => actual
        ? { ...actual, tipoError: 'cancelacion', error: `${err.message}${err.status ? ` (HTTP ${err.status})` : ''}` }
        : actual);
    } finally {
      setCancelando(false);
    }
  };

  const pedirReactivacion = (turno) => {
    setMensajeExito(null);
    setReactivacion({ turnoId: turno.turnoId, fecha: vistaLista.fecha, horaInicio: turno.horaInicio, error: null });
  };
  const confirmarReactivacion = async () => {
    if (!reactivacion || reactivando) return;
    setReactivando(true);
    try {
      const resultado = await reactivarTurno(reactivacion.turnoId);
      const avisados = resultado.alumnosAvisados === 1 ? '1 alumno' : `${resultado.alumnosAvisados} alumnos`;
      setMensajeExito(`Se reactivó el turno de las ${reactivacion.horaInicio}.${resultado.alumnosAvisados > 0 ? ` Se avisó a ${avisados} con reserva cancelada.` : ''}`);
      setReactivacion(null);
      setError(null);
      setVersion(actual => actual + 1);
    } catch (err) {
      setReactivacion(actual => actual
        ? { ...actual, error: `${err.message}${err.status ? ` (HTTP ${err.status})` : ''}` }
        : actual);
    } finally {
      setReactivando(false);
    }
  };

  const reintentarResumen = () => {
    if (!confirmacion || confirmacion.cargando) return;
    if (confirmacion.tipo === 'dia') {
      pedirCancelacionDia();
    } else {
      const turno = vistaLista?.turnos.find(item => item.horaInicio === confirmacion.horaInicio);
      if (turno) pedirCancelacionTurno(turno);
    }
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

            {vista === 'dia' && esPropietario && (
              <Button
                variant="danger"
                className="h-10"
                onClick={pedirCancelacionDia}
                disabled={!vistaLista || turnosCancelables(vistaLista).length === 0}
              >
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
      {mensajeExito && (
        <p role="status" className="mb-6 rounded-lg border border-success bg-success-bg p-4 text-sm text-success">
          {mensajeExito}
        </p>
      )}

      {!vistaLista && cargando && <Cargando texto="Cargando agenda" />}

      {vistaLista && (
        <div className={cx('relative transition-opacity', cargando && 'opacity-60')} aria-busy={cargando}>
          {cargando && <div className="loading-bar absolute inset-x-0 -top-3 h-0.5" role="progressbar" aria-label="Cargando" />}
          {vista === 'dia' ? (
            <AgendaDia
              agenda={vistaLista}
              seleccion={seleccion}
              onSeleccionar={setSeleccion}
              onSeleccionarAlumno={setAlumnoFicha}
              onCancelarTurno={esPropietario ? pedirCancelacionTurno : undefined}
              onReactivarTurno={esPropietario ? pedirReactivacion : undefined}
            />
          ) : (
            <Panel className="p-4 sm:p-5">
              <AgendaSemana semana={vistaLista} onAbrirTurno={abrirTurno} />
            </Panel>
          )}
        </div>
      )}

      {alumnoFicha && (
        <FichaAlumnoPanel
          alumnoAgenda={alumnoFicha}
          alumnos={alumnos}
          alumnosCargados={alumnosCargados}
          hoy={hoy}
          onClose={() => setAlumnoFicha(null)}
        />
      )}

      {/* Portal a body: el animate-fade-in de la página (transform) haría que el fixed quede encerrado en el contenido */}
      {confirmacion && createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="presentation">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirmar-cancelacion-titulo"
            className="w-full max-w-md rounded-xl border border-line bg-card p-6 shadow-xl"
          >
            <h2 id="confirmar-cancelacion-titulo" className="text-lg font-bold text-ink">
              {confirmacion.tipo === 'dia' ? 'Cancelar turnos del día' : 'Cancelar turno'}
            </h2>
            {/* Si falló el resumen no hay cantidades para mostrar: queda solo el error */}
            {confirmacion.tipoError !== 'resumen' && (
              <p className="mt-3 text-sm leading-relaxed text-ink-soft">
                {confirmacion.cargando
                  ? 'Consultando las reservas activas…'
                  : confirmacion.tipo === 'dia'
                    ? `Se cancelarán ${confirmacion.turnosAfectados} turnos futuros y ${confirmacion.reservasAfectadas} reservas activas del ${textoFechaLarga(confirmacion.fecha)}.`
                    : `Se cancelará el turno de las ${confirmacion.horaInicio} del ${textoFechaLarga(confirmacion.fecha)} y ${confirmacion.reservasAfectadas} reservas activas.`}
                {!confirmacion.cargando && (
                  <> Cada alumno afectado recibirá una clase extra válida durante esta semana o la siguiente, además de una notificación en la app.</>
                )}
              </p>
            )}
            {confirmacion.error && (
              <div role="alert" className="mt-3 rounded-lg border border-line bg-error-bg p-3 text-sm text-error">
                <p>{confirmacion.error}</p>
                <p className="mt-1">No se canceló ningún turno. Intentá nuevamente.</p>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-2">
              {confirmacion.error && !confirmacion.cargando && (
                <Button
                  variant="secondary"
                  onClick={confirmacion.tipoError === 'resumen' ? reintentarResumen : confirmarCancelacion}
                  disabled={cancelando}
                >
                  {confirmacion.tipoError === 'resumen' ? 'Reintentar consulta' : 'Reintentar cancelación'}
                </Button>
              )}
              <Button variant="ghost" onClick={() => setConfirmacion(null)} disabled={cancelando}>Volver</Button>
              <Button
                variant="danger"
                onClick={confirmarCancelacion}
                disabled={confirmacion.cargando || cancelando || confirmacion.tipoError === 'resumen'}
              >
                {cancelando ? 'Cancelando…' : 'Confirmar cancelación'}
              </Button>
            </div>
          </section>
        </div>,
        document.body,
      )}

      {reactivacion && createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="presentation">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirmar-reactivacion-titulo"
            className="w-full max-w-md rounded-xl border border-line bg-card p-6 shadow-xl"
          >
            <h2 id="confirmar-reactivacion-titulo" className="text-lg font-bold text-ink">Reactivar turno</h2>
            <p className="mt-3 text-sm leading-relaxed text-ink-soft">
              {`El turno de las ${reactivacion.horaInicio} del ${textoFechaLarga(reactivacion.fecha)} volverá a estar disponible para reservar.`}
              {' '}Las reservas canceladas no se restauran: los alumnos conservan su clase extra, reciben un aviso en la app y tienen que reservar de nuevo si quieren.
            </p>
            {reactivacion.error && (
              <div role="alert" className="mt-3 rounded-lg border border-line bg-error-bg p-3 text-sm text-error">
                <p>{reactivacion.error}</p>
                <p className="mt-1">No se reactivó el turno.</p>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-2">
              <Button variant="ghost" onClick={() => setReactivacion(null)} disabled={reactivando}>Volver</Button>
              <Button onClick={confirmarReactivacion} disabled={reactivando}>
                {reactivando ? 'Reactivando…' : 'Confirmar reactivación'}
              </Button>
            </div>
          </section>
        </div>,
        document.body,
      )}
    </div>
  );
}
