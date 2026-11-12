import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router';
import { CalendarDays, CalendarX, Check, ChevronRight, CircleAlert, Clock, Info, Lock } from 'lucide-react';

import { Button, Cargando, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import AccesoBloqueado from '../components/AccesoBloqueado';
import BottomSheet from '../components/BottomSheet';
import MisTurnos from './MisTurnos';
import {
  consultarTurnosDisponibles,
  partesFecha,
  reservarTurno,
  textoDiaCompleto,
  textoLugaresLibres,
  textoTurnosRestantes,
} from '../services/turnosService';

function SelectorDias({ dias, hoy, seleccionada, conReserva, onElegir }) {
  return (
    <div>
      <ul className="grid grid-cols-5 gap-2">
        {dias.map(({ fecha, habilitado }) => {
          const { dia, numero } = partesFecha(fecha);
          const elegido = fecha === seleccionada;
          const esHoy = fecha === hoy;
          return (
            <li key={fecha}>
              <button
                type="button"
                disabled={!habilitado}
                onClick={() => onElegir(fecha)}
                aria-pressed={elegido}
                aria-label={habilitado ? `${dia} ${numero}${esHoy ? ', hoy' : ''}` : `${dia} ${numero}, no disponible`}
                className={cx(
                  'flex h-[4.5rem] w-full flex-col items-center justify-center rounded-xl text-center transition-colors',
                  elegido && 'bg-primary text-on-dark shadow-card',
                  !elegido && habilitado && 'cursor-pointer border border-line bg-card text-ink hover:bg-row-hover',
                  !habilitado && 'cursor-not-allowed bg-thead/60 text-ink-muted',
                  FOCUS,
                )}
              >
                <span className="text-xs">{dia}</span>
                <span className="text-lg font-bold leading-tight tabular-nums">{numero}</span>
                {esHoy && <span className={cx('text-[10px] font-bold', elegido ? 'text-on-dark' : 'text-primary')}>HOY •</span>}
                {!esHoy && elegido && conReserva && <span className="text-[10px] font-bold leading-none text-on-dark" aria-label="con turno reservado">•</span>}
                {!habilitado && <Lock className="mt-0.5 h-3 w-3" aria-hidden="true" />}
              </button>
            </li>
          );
        })}
      </ul>
      <p className="mt-2 flex items-center gap-1.5 text-xs text-ink-muted">
        <Lock className="h-3 w-3" /> Solo podés reservar para hoy y mañana
      </p>
    </div>
  );
}

function TarjetaTurno({ turno, elegido, onElegir }) {
  const { estado, seleccionable } = turno;
  const reservado = estado === 'RESERVADO';
  const cancelado = estado === 'CANCELADO';
  const apagado = !seleccionable && !reservado;

  let detalle;
  if (reservado) {
    detalle = <span className="flex items-center gap-1 font-semibold text-primary"><Check className="h-4 w-4" /> Reservado</span>;
  } else if (cancelado) {
    detalle = <span className="font-medium text-error">Cancelado por el gimnasio</span>;
  } else if (estado === 'COMPLETO') {
    detalle = <span>Completo</span>;
  } else if (turno.faltaMenosDeUnaHora) {
    // MOGNI-254: se reserva hasta 1 hora antes; el turno se sigue viendo, pero ya no se puede elegir
    detalle = (
      <span className="flex items-center gap-1 text-right">
        <Clock className="h-3.5 w-3.5 shrink-0" aria-hidden="true" /> Falta menos de 1 h, ya no se puede reservar
      </span>
    );
  } else {
    const pocos = turno.lugaresLibres === 1;
    detalle = (
      <span className={cx('flex items-center gap-1.5 font-medium',
        elegido && 'text-on-dark',
        !elegido && seleccionable && (pocos ? 'text-warning-text' : 'text-success'))}>
        {seleccionable && !elegido && <span className={cx('h-1.5 w-1.5 rounded-full', pocos ? 'bg-warning' : 'bg-success')} aria-hidden="true" />}
        {textoLugaresLibres(turno.lugaresLibres)}
      </span>
    );
  }

  return (
    <li>
      <button
        type="button"
        disabled={!seleccionable}
        onClick={onElegir}
        aria-pressed={seleccionable ? elegido : undefined}
        className={cx(
          'flex w-full items-center justify-between gap-3 rounded-xl border px-4 py-3.5 text-left text-sm transition-colors',
          elegido && 'border-primary bg-primary text-on-dark shadow-card',
          !elegido && seleccionable && 'cursor-pointer border-line bg-card text-ink shadow-card hover:border-primary-light',
          reservado && 'border-primary bg-error-bg text-ink',
          apagado && 'border-transparent bg-thead/50 text-ink-muted',
          !seleccionable && 'cursor-default',
          FOCUS,
        )}
      >
        <span className={cx('font-semibold tabular-nums', cancelado && 'line-through')}>
          {turno.horaInicio} – {turno.horaFin}
        </span>
        <span className="flex items-center gap-1 text-xs">
          {detalle}
          {seleccionable && <ChevronRight className="h-4 w-4" aria-hidden="true" />}
        </span>
      </button>
    </li>
  );
}

/**
 * Confirmar la reserva (HU-254): primero "Confirmá tu reserva" y, una vez hecha, "¡Turno reservado!".
 * Si el backend la rechaza, el motivo se muestra acá y se puede cerrar.
 */
function ReservaSheet({ turno, fechaTexto, esUltimoTurno, onConfirmar, onCerrar }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const [reserva, setReserva] = useState(null);

  const confirmar = async () => {
    setEnviando(true);
    setError(null);
    try {
      setReserva(await onConfirmar());
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  if (reserva) {
    return (
      <BottomSheet label="Turno reservado" onClose={onCerrar}>
        <div className="flex flex-col items-center pb-2 pt-3 text-center">
          <span className="flex h-16 w-16 items-center justify-center rounded-full bg-success-bg text-success">
            <Check className="h-8 w-8" strokeWidth={3} />
          </span>
          <h2 className="mt-4 text-xl font-bold text-ink">¡Turno reservado!</h2>
          <p className="mt-1 text-sm text-ink-soft">
            {textoDiaCompleto(reserva.fecha)} · {reserva.horaInicio} – {reserva.horaFin} hs
          </p>
        </div>
        {/* "Ver mis turnos" llega con la HU de Mis turnos: por ahora el botón solo cierra */}
        <Button variant="primary" size="lg" className="mt-5 w-full" onClick={onCerrar} autoFocus>Listo</Button>
      </BottomSheet>
    );
  }

  return (
    <BottomSheet label="Confirmá tu reserva" onClose={onCerrar} cerrable={!enviando}>
      <h2 className="mb-4 text-xl font-bold text-ink">Confirmá tu reserva</h2>

      <div className="flex items-center gap-3 rounded-xl bg-row-hover p-3.5">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <CalendarDays className="h-5 w-5" />
        </span>
        <div>
          <p className="text-sm font-bold text-ink">{fechaTexto}</p>
          <p className="text-sm tabular-nums text-ink-soft">{turno.horaInicio} – {turno.horaFin} hs</p>
        </div>
      </div>

      {esUltimoTurno && !error && (
        <p className="mt-3 flex items-center gap-2 rounded-lg bg-warning-bg px-3 py-2.5 text-xs font-semibold text-warning-text">
          <Info className="h-4 w-4 shrink-0" /> Vas a usar tu último turno de esta semana
        </p>
      )}

      {error && (
        <p role="alert" className="mt-3 flex items-start gap-2 text-sm font-medium text-error">
          <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {error}
        </p>
      )}

      {!error && (
        <Button variant="primary" size="lg" className="mt-5 w-full" onClick={confirmar} disabled={enviando} autoFocus>
          {enviando ? 'Reservando...' : 'Confirmar reserva'}
        </Button>
      )}
      <button type="button" onClick={onCerrar} disabled={enviando}
        className={cx('mx-auto mt-3 block cursor-pointer px-4 py-2 text-sm font-semibold text-ink-soft hover:text-ink disabled:cursor-not-allowed disabled:opacity-50', FOCUS)}>
        {error ? 'Cerrar' : 'Cancelar'}
      </button>
    </BottomSheet>
  );
}

function Aviso({ icono: Icono, children }) {
  return (
    <p className="mt-3 flex items-center gap-2 text-xs text-ink-soft">
      <Icono className="h-4 w-4 shrink-0" /> {children}
    </p>
  );
}

/**
 * HU-259 y HU-254: turnos disponibles de hoy y mañana. Todo lo decide el backend (días, estados, contador, qué se
 * puede elegir); la pantalla solo lo dibuja. Elegir un turno abre la confirmación para reservarlo.
 */
function ReservarTurnos() {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  // Turno elegido para reservar (el objeto entero: la lista se vuelve a pedir mientras la hoja sigue abierta)
  const [elegido, setElegido] = useState(null);
  // Si se cambia de día rápido, solo vale la respuesta de la última consulta
  const ultimaConsulta = useRef(0);

  const consultar = (fecha) => {
    const numero = ++ultimaConsulta.current;
    consultarTurnosDisponibles(fecha)
      .then((respuesta) => {
        if (numero !== ultimaConsulta.current) return;
        setDatos(respuesta);
        setError(null);
      })
      .catch((err) => {
        if (numero === ultimaConsulta.current) setError(err);
      })
      .finally(() => {
        if (numero === ultimaConsulta.current) setCargando(false);
      });
  };

  useEffect(() => { consultar(); }, []);

  // Cambiar de día borra el turno elegido
  const elegirDia = (fecha) => {
    setCargando(true);
    setElegido(null);
    consultar(fecha);
  };

  // La lista y el contador se refrescan por detrás; la hoja sigue abierta mostrando "¡Turno reservado!"
  const confirmarReserva = async () => {
    const reserva = await reservarTurno(elegido.id);
    consultar(datos.fechaSeleccionada);
    return reserva;
  };

  // Si la reserva falló (por ejemplo, se llenó el turno) se vuelve a consultar para mostrar el estado real
  const cerrarReserva = () => {
    setElegido(null);
    consultar(datos.fechaSeleccionada);
  };

  // 403: alumno inactivo. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) return <AccesoBloqueado mensaje={error.message} />;

  if (error && !datos) {
    return (
      <div className="animate-fade-in">
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error.message}</span>
        </div>
      </div>
    );
  }

  if (!datos) return <Cargando texto="Cargando turnos" />;

  const { turnos, turnosRestantesSemana, turnosPorSemana, tieneReservaEnElDia } = datos;
  const sinTurnosEnLaSemana = turnosRestantesSemana === 0 && turnosPorSemana > 0;
  const grupos = [
    { titulo: 'Mañana', turnos: turnos.filter(t => t.horaInicio < '12:00') },
    { titulo: 'Tarde', turnos: turnos.filter(t => t.horaInicio >= '12:00') },
  ].filter(grupo => grupo.turnos.length > 0);

  return (
    <div className="animate-fade-in">
      <Panel className="mb-4 flex items-center gap-3 rounded-xl p-3.5">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <CalendarDays className="h-5 w-5" />
        </span>
        <p className="text-sm font-semibold text-ink">{textoTurnosRestantes(turnosRestantesSemana, turnosPorSemana)}</p>
      </Panel>

      <SelectorDias dias={datos.dias} hoy={datos.hoy} seleccionada={datos.fechaSeleccionada} conReserva={tieneReservaEnElDia} onElegir={elegirDia} />

      {error && (
        <p role="alert" className="mt-4 flex items-center gap-2 text-sm text-error">
          <CircleAlert className="h-4 w-4 shrink-0" /> {error.message}
        </p>
      )}

      {/* MOGNI-254/259: en mora ve los turnos pero no puede elegir ninguno (el backend los manda no seleccionables) */}
      {datos.bloqueadoPorMora && (
        <div role="alert" className="mt-4 flex items-start gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />
          <div>
            <p className="text-sm font-semibold">Tenés la cuota en mora</p>
            <p className="mt-1 text-xs leading-relaxed">
              No podés reservar turnos hasta regularizar el pago. Cuando se registre, vas a poder reservar en el momento.
            </p>
          </div>
        </div>
      )}

      {/* Día de gracia: la cuota venció ayer; hoy puede reservar, pero para mañana tiene que pagar */}
      {datos.bloqueadoPorCuotaVencida && (
        <div role="alert" className="mt-4 flex items-start gap-3 rounded-lg border border-warning bg-warning-bg p-4 text-warning-text">
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />
          <div>
            <p className="text-sm font-semibold">Pagá tu cuota para reservar este día</p>
            <p className="mt-1 text-xs leading-relaxed">
              Tu cuota venció y hoy es el último día para pagarla. Podés reservar los turnos de hoy; para reservar los
              de mañana, regularizá el pago. Cuando se registre, vas a poder reservar en el momento.
            </p>
          </div>
        </div>
      )}

      {/* MOGNI-281: con la semana completa, una clase extra todavía le deja reservar (como en el backend) */}
      {sinTurnosEnLaSemana && !datos.bloqueadoPorMora && !datos.bloqueadoPorCuotaVencida && (datos.tieneClaseExtra ? (
        <p className="mt-4 flex items-center gap-2 rounded-lg bg-info-bg px-3 py-2.5 text-xs font-semibold text-info">
          <Info className="h-4 w-4 shrink-0" /> Tenés una clase extra para usar
        </p>
      ) : (
        <p className="mt-4 flex items-center gap-2 rounded-lg bg-thead/60 px-3 py-2.5 text-xs text-ink-soft">
          <Info className="h-4 w-4 shrink-0" /> Podés volver a reservar desde el lunes
        </p>
      ))}

      <section aria-label="Turnos del día" aria-busy={cargando}
        aria-disabled={datos.bloqueadoPorMora || datos.bloqueadoPorCuotaVencida || undefined}
        className={cx('mt-5 transition-opacity',
          (cargando || datos.bloqueadoPorMora || datos.bloqueadoPorCuotaVencida) && 'opacity-50')}>
        {turnos.length === 0 ? (
          <div className="mt-10 flex flex-col items-center gap-3 text-center">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
              <CalendarX className="h-6 w-6" />
            </span>
            <p className="text-sm font-semibold text-ink">No hay turnos disponibles para este día</p>
          </div>
        ) : (
          grupos.map(grupo => (
            <div key={grupo.titulo} className="mb-4">
              <h2 className="mb-2 text-[11px] font-bold uppercase tracking-wide text-ink-muted">{grupo.titulo}</h2>
              <ul className="space-y-2.5">
                {grupo.turnos.map(turno => (
                  <TarjetaTurno
                    key={turno.id}
                    turno={turno}
                    elegido={elegido?.id === turno.id}
                    onElegir={() => setElegido(turno)}
                  />
                ))}
              </ul>
            </div>
          ))
        )}

        {tieneReservaEnElDia && <Aviso icono={Info}>Ya tenés un turno reservado este día</Aviso>}
      </section>

      {elegido && (
        <ReservaSheet
          turno={elegido}
          fechaTexto={textoDiaCompleto(datos.fechaSeleccionada)}
          esUltimoTurno={turnosRestantesSemana === 1}
          onConfirmar={confirmarReserva}
          onCerrar={cerrarReserva}
        />
      )}
    </div>
  );
}

const PESTANIAS = [
  { id: 'reservar', texto: 'Reservar' },
  { id: 'mis-turnos', texto: 'Mis turnos' },
];

/**
 * Pantalla de turnos del alumno: pestañas "Reservar" (HU-259 y HU-254) y "Mis turnos" (HU-256). MOGNI-281: el inicio
 * abre "Mis turnos" directo con ?tab=mis-turnos; después las pestañas se cambian sin tocar la URL.
 */
export default function TurnosAlumno() {
  const [params] = useSearchParams();
  const [pestania, setPestania] = useState(params.get('tab') === 'mis-turnos' ? 'mis-turnos' : 'reservar');

  return (
    <div>
      <h1 className="mb-5 text-2xl font-bold text-ink">Turnos</h1>

      <div className="mb-4 grid grid-cols-2 gap-1 rounded-xl bg-thead/70 p-1" role="tablist" aria-label="Turnos">
        {PESTANIAS.map(({ id, texto }) => {
          const activa = pestania === id;
          return (
            <button key={id} type="button" role="tab" aria-selected={activa} onClick={() => setPestania(id)}
              className={cx('cursor-pointer rounded-lg py-2 text-sm transition-colors', FOCUS,
                activa ? 'bg-card font-semibold text-primary shadow-card' : 'font-medium text-ink-muted hover:text-ink')}>
              {texto}
            </button>
          );
        })}
      </div>

      {/* Al cambiar de pestaña se vuelve a montar y a consultar: Mis turnos siempre muestra lo último reservado */}
      {pestania === 'reservar'
        ? <ReservarTurnos />
        : <MisTurnos onVerDisponibles={() => setPestania('reservar')} />}
    </div>
  );
}
