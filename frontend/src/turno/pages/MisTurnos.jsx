import { useEffect, useState } from 'react';
import { CalendarDays, CircleAlert } from 'lucide-react';

import { Button, Cargando } from '../../shared/components/ui';
import AccesoBloqueado from '../components/AccesoBloqueado';
import { consultarMisTurnos, etiquetaFechaTurno, partesFecha } from '../services/turnosService';

function TarjetaMiTurno({ turno, hoy }) {
  const { dia, numero } = partesFecha(turno.fecha);
  const esHoy = turno.fecha === hoy;
  return (
    <li className="flex items-center gap-4 rounded-xl border border-line bg-card p-4 shadow-card">
      <div className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-lg bg-error-bg text-primary">
        <span className="text-[10px] font-bold uppercase leading-none">{esHoy ? 'Hoy' : dia}</span>
        <span className="mt-0.5 text-xl font-bold leading-none tabular-nums">{numero}</span>
      </div>
      <div className="min-w-0">
        <p className="text-sm font-bold text-ink">{etiquetaFechaTurno(turno.fecha, hoy)}</p>
        <p className="text-sm tabular-nums text-ink-soft">{turno.horaInicio} – {turno.horaFin} hs</p>
        <span className="mt-1.5 inline-block rounded-full bg-success-bg px-2.5 py-0.5 text-[11px] font-semibold text-success">
          Reservado
        </span>
      </div>
    </li>
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
 * Todo lo decide el backend (qué turnos, el orden y el "hoy"); la pantalla solo lo dibuja.
 */
export default function MisTurnos({ onVerDisponibles }) {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarMisTurnos()
      .then((respuesta) => { if (vigente) setDatos(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  // 403: alumno inactivo u online. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) return <AccesoBloqueado mensaje={error.message} />;

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error.message}</span>
      </div>
    );
  }

  if (!datos) return <Cargando texto="Cargando tus turnos" />;

  if (datos.turnos.length === 0) return <SinTurnos onVerDisponibles={onVerDisponibles} />;

  return (
    <section aria-label="Mis turnos" className="animate-fade-in">
      <h2 className="mb-3 text-[11px] font-bold uppercase tracking-wide text-ink-muted">Próximos turnos</h2>
      <ul className="space-y-3">
        {datos.turnos.map(turno => <TarjetaMiTurno key={turno.reservaId} turno={turno} hoy={datos.hoy} />)}
      </ul>
    </section>
  );
}
