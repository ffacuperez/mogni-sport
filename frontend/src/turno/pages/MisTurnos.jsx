import { useEffect, useState } from 'react';
import { CalendarDays, Check, CircleAlert, Clock } from 'lucide-react';

import { Button, Cargando } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import AccesoBloqueado from '../components/AccesoBloqueado';
import BottomSheet from '../components/BottomSheet';
import {
  cancelarReserva,
  consultarMisTurnos,
  etiquetaFechaTurno,
  partesFecha,
  textoDiaCompleto,
} from '../services/turnosService';

function TarjetaMiTurno({ turno, hoy, onCancelar }) {
  const { dia, numero } = partesFecha(turno.fecha);
  const esHoy = turno.fecha === hoy;
  return (
    <li className="rounded-xl border border-line bg-card p-4 shadow-card">
      <div className="flex items-center gap-4">
        <div className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-lg bg-error-bg text-primary">
          <span className="text-[10px] font-bold uppercase leading-none">{esHoy ? 'Hoy' : dia}</span>
          <span className="mt-0.5 text-xl font-bold leading-none tabular-nums">{numero}</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-bold text-ink">{etiquetaFechaTurno(turno.fecha, hoy)}</p>
          <p className="text-sm tabular-nums text-ink-soft">{turno.horaInicio} – {turno.horaFin} hs</p>
          <span className="mt-1.5 inline-block rounded-full bg-success-bg px-2.5 py-0.5 text-[11px] font-semibold text-success">
            Reservado
          </span>
        </div>
        <button
          type="button"
          disabled={!turno.puedeCancelar}
          onClick={() => onCancelar(turno)}
          className={cx(
            'shrink-0 rounded-lg border px-4 py-2 text-sm font-semibold transition-colors',
            turno.puedeCancelar
              ? 'cursor-pointer border-primary bg-card text-primary hover:bg-error-bg'
              : 'cursor-not-allowed border-line bg-card text-ink-muted',
            FOCUS,
          )}
        >
          Cancelar
        </button>
      </div>
      {!turno.puedeCancelar && (
        <p className="mt-3 flex items-center gap-2 border-t border-line pt-3 text-xs text-ink-soft">
          <Clock className="h-3.5 w-3.5 shrink-0" /> Ya no se puede cancelar (menos de 2 hs)
        </p>
      )}
    </li>
  );
}

/**
 * Confirmar la cancelación (HU-255). Si el backend la rechaza (por ejemplo, justo pasaron las 2 horas), el motivo se
 * muestra acá y se puede cerrar. onConfirmar devuelve una promesa.
 */
function CancelarSheet({ turno, onConfirmar, onCerrar }) {
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
    <BottomSheet label="Cancelar tu turno" onClose={onCerrar} cerrable={!enviando}>
      <h2 className="mb-4 text-xl font-bold text-ink">¿Cancelar tu turno?</h2>

      <div className="flex items-center gap-3 rounded-xl bg-row-hover p-3.5">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <CalendarDays className="h-5 w-5" />
        </span>
        <div>
          <p className="text-sm font-bold text-ink">{textoDiaCompleto(turno.fecha)}</p>
          <p className="text-sm tabular-nums text-ink-soft">{turno.horaInicio} – {turno.horaFin} hs</p>
        </div>
      </div>

      {error ? (
        <p role="alert" className="mt-3 flex items-start gap-2 text-sm font-medium text-error">
          <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {error}
        </p>
      ) : (
        <p className="mt-3 text-xs text-ink-soft">Tu lugar va a quedar libre para otro alumno.</p>
      )}

      {!error && (
        <Button variant="primary" size="lg" className="mt-5 w-full" onClick={confirmar} disabled={enviando}>
          {enviando ? 'Cancelando...' : 'Sí, cancelar turno'}
        </Button>
      )}
      <button type="button" onClick={onCerrar} disabled={enviando}
        className={cx('mx-auto mt-3 block cursor-pointer px-4 py-2 text-sm font-semibold text-ink-soft hover:text-ink disabled:cursor-not-allowed disabled:opacity-50', FOCUS)}>
        {error ? 'Cerrar' : 'Volver'}
      </button>
    </BottomSheet>
  );
}

function SinTurnos({ onVerDisponibles }) {
  return (
    <div className="mt-12 flex flex-col items-center gap-3 text-center">
      <span className="flex h-24 w-24 items-center justify-center rounded-full bg-error-bg text-primary">
        <CalendarDays className="h-10 w-10" />
      </span>
      <p className="mt-2 text-base font-bold text-ink">No tenés turnos reservados</p>
      <p className="text-sm text-ink-soft">Reservá tu lugar en la próxima clase.</p>
      <Button variant="primary" size="lg" className="mt-3" onClick={onVerDisponibles}>
        Ver turnos disponibles
      </Button>
    </div>
  );
}

/**
 * HU-256: turnos que el alumno tiene reservados y todavía no terminaron, del más cercano al más lejano.
 * HU-255: cada uno se puede cancelar hasta 2 horas antes. Todo lo decide el backend (qué turnos, el orden, el
 * "hoy" y si todavía se puede cancelar); la pantalla solo lo dibuja.
 */
export default function MisTurnos({ onVerDisponibles }) {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState(null);
  const [aCancelar, setACancelar] = useState(null);
  const [aviso, setAviso] = useState(null);

  const cargar = () => consultarMisTurnos()
    .then((respuesta) => { setDatos(respuesta); setError(null); })
    .catch((err) => setError(err));

  useEffect(() => {
    let vigente = true;
    consultarMisTurnos()
      .then((respuesta) => { if (vigente) setDatos(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  const confirmarCancelacion = async () => {
    await cancelarReserva(aCancelar.reservaId);
    setAviso(`Cancelaste tu turno del ${textoDiaCompleto(aCancelar.fecha)}. Tu lugar quedó libre.`);
    setACancelar(null);
    await cargar();
  };

  // Si la cancelación falló (por ejemplo, justo pasó el límite) se vuelve a consultar para mostrar el estado real
  const cerrarCancelacion = () => {
    setACancelar(null);
    cargar();
  };

  // 403: alumno inactivo. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) return <AccesoBloqueado mensaje={error.message} />;

  if (error && !datos) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error.message}</span>
      </div>
    );
  }

  if (!datos) return <Cargando texto="Cargando tus turnos" />;

  return (
    <section aria-label="Mis turnos" className="animate-fade-in">
      {aviso && (
        <p role="status" className="mb-4 flex items-center gap-2 rounded-lg bg-success-bg px-3 py-2.5 text-sm font-medium text-success">
          <Check className="h-4 w-4 shrink-0" /> {aviso}
        </p>
      )}

      {error && (
        <p role="alert" className="mb-4 flex items-center gap-2 text-sm text-error">
          <CircleAlert className="h-4 w-4 shrink-0" /> {error.message}
        </p>
      )}

      {datos.turnos.length === 0 ? (
        <SinTurnos onVerDisponibles={onVerDisponibles} />
      ) : (
        <>
          <h2 className="mb-3 text-[11px] font-bold uppercase tracking-wide text-ink-muted">Próximos turnos</h2>
          <ul className="space-y-3">
            {datos.turnos.map(turno => (
              <TarjetaMiTurno key={turno.reservaId} turno={turno} hoy={datos.hoy} onCancelar={(t) => { setAviso(null); setACancelar(t); }} />
            ))}
          </ul>
        </>
      )}

      {aCancelar && (
        <CancelarSheet turno={aCancelar} onConfirmar={confirmarCancelacion} onCerrar={cerrarCancelacion} />
      )}
    </section>
  );
}
