import { useEffect, useRef, useState } from 'react';
import { CalendarDays, CalendarX, Check, ChevronRight, CircleAlert, Info, Lock } from 'lucide-react';

import { Button, Cargando, Modal, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import {
  consultarTurnosDisponibles,
  partesFecha,
  reservarTurno,
  textoLugaresLibres,
  textoTurnosRestantes,
} from '../services/turnosService';

function SelectorDias({ dias, hoy, seleccionada, onElegir }) {
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

/** Confirmar la reserva (HU-254). Si el backend la rechaza, el motivo se muestra acá y el modal queda abierto. */
function ConfirmarReservaModal({ turno, etiquetaDia, onConfirmar, onCancelar }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  const confirmar = async () => {
    setEnviando(true);
    setError(null);
    try {
      await onConfirmar();
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <Modal title="Reservar turno" onClose={onCancelar} cerrable={!enviando}>
      <p className="text-sm text-ink-soft">{etiquetaDia}</p>
      <p className="mt-1 text-2xl font-bold tabular-nums text-ink">{turno.horaInicio} – {turno.horaFin}</p>
      <p className="mt-1 text-sm text-ink-soft">{textoLugaresLibres(turno.lugaresLibres)}</p>

      {error && (
        <p role="alert" className="mt-4 flex items-start gap-2 text-sm font-medium text-error">
          <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {error}
        </p>
      )}

      <div className="mt-6 flex justify-end gap-3">
        <Button variant="ghost" onClick={onCancelar} disabled={enviando}>{error ? 'Cerrar' : 'Cancelar'}</Button>
        {!error && (
          <Button variant="primary" onClick={confirmar} disabled={enviando} autoFocus>
            {enviando ? 'Reservando...' : 'Confirmar reserva'}
          </Button>
        )}
      </div>
    </Modal>
  );
}

/** "Hoy · Mar 13" / "Mañana · Mié 14" */
function etiquetaDia({ hoy, fechaSeleccionada }) {
  const { dia, numero } = partesFecha(fechaSeleccionada);
  return `${fechaSeleccionada === hoy ? 'Hoy' : 'Mañana'} · ${dia} ${numero}`;
}

function Aviso({ icono: Icono, children }) {
  return (
    <p className="mt-3 flex items-center gap-2 text-xs text-ink-soft">
      <Icono className="h-4 w-4 shrink-0" /> {children}
    </p>
  );
}

/**
 * HU-259: turnos disponibles de hoy y mañana. Todo lo decide el backend (días, estados, contador, qué se puede
 * elegir); la pantalla solo lo dibuja. Elegir un turno abre la confirmación y reservarlo es la HU-254.
 */
export default function TurnosAlumno() {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [elegido, setElegido] = useState(null);
  const [exito, setExito] = useState(null);
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
    setExito(null);
    consultar(fecha);
  };

  const confirmarReserva = async () => {
    const reserva = await reservarTurno(elegido);
    setElegido(null);
    setExito(`Reservaste el turno de ${reserva.horaInicio} a ${reserva.horaFin}.`);
    consultar(datos.fechaSeleccionada);
  };

  // Si la reserva falla (por ejemplo, se llenó el turno) se vuelve a consultar para mostrar el estado real
  const cerrarConfirmacion = () => {
    setElegido(null);
    consultar(datos.fechaSeleccionada);
  };

  const titulo = <h1 className="mb-5 text-2xl font-bold text-ink">Turnos</h1>;

  // 403: alumno inactivo u online. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) {
    return (
      <div className="animate-fade-in">
        {titulo}
        <div className="mt-12 flex flex-col items-center gap-3 text-center">
          <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
            <Lock className="h-6 w-6" />
          </span>
          <p className="text-sm font-semibold text-ink">No podés acceder a los turnos</p>
          <p className="max-w-64 text-xs text-ink-soft">{error.message}</p>
        </div>
      </div>
    );
  }

  if (error && !datos) {
    return (
      <div className="animate-fade-in">
        {titulo}
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error.message}</span>
        </div>
      </div>
    );
  }

  if (!datos) return <div>{titulo}<Cargando texto="Cargando turnos" /></div>;

  const { turnos, turnosRestantesSemana, tieneReservaEnElDia } = datos;
  const turnoElegido = turnos.find(t => t.id === elegido);

  return (
    <div className="animate-fade-in">
      {titulo}

      <div className="mb-4 grid grid-cols-2 gap-1 rounded-xl bg-thead/70 p-1" role="tablist" aria-label="Turnos">
        <button type="button" role="tab" aria-selected="true"
          className={cx('rounded-lg bg-card py-2 text-sm font-semibold text-primary shadow-card', FOCUS)}>
          Reservar
        </button>
        {/* Mis turnos es la HU-256 */}
        <button type="button" role="tab" aria-selected="false" disabled title="Próximamente"
          className="cursor-not-allowed rounded-lg py-2 text-sm font-medium text-ink-muted">
          Mis turnos <span className="text-[10px] font-normal">(próximamente)</span>
        </button>
      </div>

      <Panel className="mb-4 flex items-center gap-3 rounded-xl p-3.5">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <CalendarDays className="h-5 w-5" />
        </span>
        <p className="text-sm font-semibold text-ink">{textoTurnosRestantes(turnosRestantesSemana)}</p>
      </Panel>

      <SelectorDias dias={datos.dias} hoy={datos.hoy} seleccionada={datos.fechaSeleccionada} onElegir={elegirDia} />

      {error && (
        <p role="alert" className="mt-4 flex items-center gap-2 text-sm text-error">
          <CircleAlert className="h-4 w-4 shrink-0" /> {error.message}
        </p>
      )}

      {exito && (
        <p role="status" className="mt-4 flex items-center gap-2 text-sm font-medium text-success">
          <Check className="h-4 w-4 shrink-0" /> {exito}
        </p>
      )}

      <section aria-label="Turnos del día" aria-busy={cargando} className={cx('mt-5 transition-opacity', cargando && 'opacity-50')}>
        {turnos.length === 0 ? (
          <div className="mt-10 flex flex-col items-center gap-3 text-center">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-card text-ink-muted shadow-card">
              <CalendarX className="h-6 w-6" />
            </span>
            <p className="text-sm font-semibold text-ink">No hay turnos disponibles para este día</p>
          </div>
        ) : (
          <ul className="space-y-2.5">
            {turnos.map(turno => (
              <TarjetaTurno
                key={turno.id}
                turno={turno}
                elegido={elegido === turno.id}
                onElegir={() => { setExito(null); setElegido(turno.id); }}
              />
            ))}
          </ul>
        )}

        {tieneReservaEnElDia && <Aviso icono={Info}>Ya tenés un turno reservado este día</Aviso>}
        {turnosRestantesSemana === 0 && turnos.length > 0 && (
          <Aviso icono={Info}>Ya usaste todos los turnos de tu membresía en esta semana, así que no podés elegir otro.</Aviso>
        )}
      </section>

      {turnoElegido && (
        <ConfirmarReservaModal
          turno={turnoElegido}
          etiquetaDia={etiquetaDia(datos)}
          onConfirmar={confirmarReserva}
          onCancelar={cerrarConfirmacion}
        />
      )}
    </div>
  );
}
