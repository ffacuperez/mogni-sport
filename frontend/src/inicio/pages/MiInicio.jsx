import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { CalendarDays, ChevronRight, CircleAlert, Clock, Dumbbell, Plus, TriangleAlert } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Button, Cargando, Panel } from '../../shared/components/ui';
import BarraSemanas from '../../progreso/components/BarraSemanas';
import { tituloPlan } from '../../progreso/services/progresoService';
import AccesoBloqueado from '../../turno/components/AccesoBloqueado';
import { formatearFecha } from '../../shared/services/texto';
import { partesFecha } from '../../turno/services/turnosService';
import { avisoCuota, consultarMiInicio, TEXTO_MOTIVO_NO_RESERVA, textoTurnosQueQuedan } from '../services/inicioService';

const ESTILO_AVISO = {
  warning: { clases: 'border-warning bg-warning-bg text-warning-text', icono: TriangleAlert },
  alert: { clases: 'border-alert bg-alert-bg text-alert', icono: TriangleAlert },
  error: { clases: 'border-error bg-error-bg text-error', icono: CircleAlert },
};

const sumarDias = (fechaISO, dias) => {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + dias)).toISOString().slice(0, 10);
};

/** "Hoy", "Mañana" o "Mié 14/10". hoy sale del backend, no del reloj del navegador. */
function textoDia(fechaISO, hoy) {
  if (fechaISO === hoy) return 'Hoy';
  if (fechaISO === sumarDias(hoy, 1)) return 'Mañana';
  const [, m, d] = fechaISO.split('-').map(Number);
  return `${partesFecha(fechaISO).dia} ${d}/${m}`;
}

function AvisoCuota({ cuota }) {
  const aviso = avisoCuota(cuota);
  if (!aviso) return null;
  const { clases, icono: Icono } = ESTILO_AVISO[aviso.tono];
  return (
    <div role="alert" className={cx('flex items-center gap-2.5 rounded-xl border px-4 py-3 text-sm font-semibold', clases)}>
      <Icono className="h-4 w-4 shrink-0" />
      {aviso.texto}
    </div>
  );
}

function ProximoTurno({ turno, hoy }) {
  if (!turno) {
    return (
      <Panel className="rounded-xl p-4">
        <div className="flex items-center gap-3 border-b border-line pb-3">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-row-hover text-ink-soft">
            <CalendarDays className="h-5 w-5" />
          </span>
          <div className="min-w-0">
            <p className="text-sm text-ink-soft">Tu próximo turno</p>
            <p className="text-lg font-bold text-ink">No tenés turnos agendados</p>
          </div>
        </div>
        <p className="pt-3 text-sm text-ink-soft">Reservá un turno para hoy o mañana y asegurá tu lugar.</p>
      </Panel>
    );
  }
  return (
    <Panel className="overflow-hidden rounded-xl border-l-4 border-l-primary p-4">
      <div className="flex items-center gap-3 border-b border-line pb-3">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <Clock className="h-5 w-5" />
        </span>
        <div className="min-w-0">
          <p className="text-sm text-ink-soft">Tu próximo turno</p>
          <p className="text-xl font-bold tabular-nums text-ink">
            {textoDia(turno.fecha, hoy)} · {turno.horaInicio} – {turno.horaFin} hs
          </p>
        </div>
      </div>
      <Link to="/mi-cuenta/turnos?tab=mis-turnos" className={cx('mt-3 inline-flex items-center gap-1 rounded text-sm font-semibold text-primary hover:underline', FOCUS)}>
        Ver mis turnos <ChevronRight className="h-4 w-4" />
      </Link>
    </Panel>
  );
}

function TurnosDeLaSemana({ usados, total }) {
  return (
    <Panel className="rounded-xl p-4">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-ink-soft">Turnos de esta semana</p>
          <p className="text-xl font-bold text-ink">{textoTurnosQueQuedan(usados, total)}</p>
        </div>
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
          <CalendarDays className="h-5 w-5" />
        </span>
      </div>
      {total > 0 && <BarraSemanas total={total} completas={Math.min(usados, total)} />}
      <p className="mt-3 text-xs text-ink-muted">Se renueva el lunes</p>
    </Panel>
  );
}

/** A cuántas clases fue en el período de su cuota, de cuántas podía ir, y cuál fue la última. */
function ClasesDeLaCuota({ clases }) {
  if (!clases) return null;
  const { presentes, total, ultima } = clases;
  const porcentaje = total > 0 ? Math.min(100, (presentes / total) * 100) : 0;
  return (
    <Panel className="rounded-xl p-4">
      <div className="mb-2 flex items-baseline justify-between gap-3">
        <p className="text-sm text-ink-soft">Clases de tu cuota</p>
        <p className="text-lg font-bold tabular-nums text-ink">{presentes}/{total}</p>
      </div>
      <div
        role="progressbar"
        aria-label="Clases de tu cuota"
        aria-valuemin={0}
        aria-valuemax={total}
        aria-valuenow={presentes}
        className="h-2 overflow-hidden rounded-full bg-line"
      >
        <div className="h-full rounded-full bg-primary transition-[width]" style={{ width: `${porcentaje}%` }} />
      </div>
      <p className="mt-3 flex items-center gap-2 rounded-lg bg-row-hover px-3 py-2.5 text-sm text-ink-soft">
        <Clock className="h-4 w-4 shrink-0" />
        {ultima
          ? <span>Última clase: <span className="font-semibold tabular-nums text-ink">{formatearFecha(ultima.fecha)} {ultima.horaInicio} hs</span></span>
          : 'Todavía no fuiste a ninguna clase'}
      </p>
    </Panel>
  );
}

/** Lleva a la reserva. Si no puede reservar queda deshabilitado y debajo se explica por qué. */
function ReservarTurno({ puedeReservar, motivo }) {
  const navigate = useNavigate();
  const motivoId = 'motivo-no-reserva';
  return (
    <div>
      <Button
        variant="primary"
        size="lg"
        className="w-full rounded-xl py-4 text-base"
        disabled={!puedeReservar}
        aria-describedby={!puedeReservar && motivo ? motivoId : undefined}
        onClick={() => navigate('/mi-cuenta/turnos')}
      >
        <Plus className="h-5 w-5" /> Reservar turno
      </Button>
      {!puedeReservar && motivo && (
        <p id={motivoId} className="mt-2 flex items-start gap-1.5 text-sm text-ink-soft">
          <CircleAlert className="mt-0.5 h-4 w-4 shrink-0 text-error" /> {TEXTO_MOTIVO_NO_RESERVA[motivo]}
        </p>
      )}
    </div>
  );
}

function TusRutinas({ plan }) {
  return (
    <Panel as={Link} to="/mi-cuenta/progreso" className={cx('flex items-center gap-3 rounded-xl p-4 transition-colors hover:bg-row-hover', FOCUS)}>
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
        <Dumbbell className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-base font-bold text-ink">Tus rutinas</p>
        <p className="truncate text-sm text-ink-soft">
          {plan ? `${tituloPlan(plan)} · Semana ${plan.semanaActual} de ${plan.cantidadSemanas}` : 'Todavía no tenés rutinas asignadas'}
        </p>
      </div>
      <ChevronRight className="h-5 w-5 shrink-0 text-ink-muted" />
    </Panel>
  );
}

/** MOGNI-281: el inicio del alumno. Una sola llamada; el backend decide todo lo que se muestra. */
export default function MiInicio() {
  const [inicio, setInicio] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarMiInicio()
      .then((respuesta) => { if (vigente) setInicio(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  // 403: cuenta inactiva. El mensaje con el motivo lo manda el backend
  if (error?.status === 403) return <AccesoBloqueado titulo="No podés acceder a tu inicio" mensaje={error.message} />;

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error.message}</span>
      </div>
    );
  }

  if (!inicio) return <Cargando texto="Cargando tu inicio" />;

  const { hoy, nombre, membresia, diasPorSemana, activo, cuota, proximoTurno, turnosSemana, clasesCuota, puedeReservar, motivoNoReserva, planActivo } = inicio;

  return (
    <div className="flex flex-col gap-4 animate-fade-in">
      <header>
        <h1 className="text-2xl font-bold text-ink">¡Hola, {nombre}!</h1>
        <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-ink-soft">
          <span>{membresia ? `${membresia} · ${diasPorSemana} ${diasPorSemana === 1 ? 'día' : 'días'} por semana` : 'Sin membresía activa'}</span>
          <Badge tone={activo ? 'success' : 'error'}>{activo ? 'Activo' : 'Inactivo'}</Badge>
        </div>
      </header>

      <AvisoCuota cuota={cuota} />
      <ProximoTurno turno={proximoTurno} hoy={hoy} />
      <TurnosDeLaSemana usados={turnosSemana.usados} total={turnosSemana.total} />
      <ClasesDeLaCuota clases={clasesCuota} />
      <ReservarTurno puedeReservar={puedeReservar} motivo={motivoNoReserva} />
      <TusRutinas plan={planActivo} />
    </div>
  );
}
