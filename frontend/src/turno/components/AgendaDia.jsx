import { CalendarCheck, CalendarX, CalendarOff, Lock, Users } from 'lucide-react';

import { Badge, Button, Panel, Row, Table } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { textoDiaCompleto } from '../services/turnosService';
import {
  ESTILO_TURNO,
  esDeManiana,
  estadoCuotaAgenda,
  porcentajeOcupado,
  textoDiaYFecha,
  textoOcupacion,
} from '../services/agendaService';

const COLUMNAS_ALUMNOS = [{ label: 'Alumno' }, { label: 'Membresía' }, { label: 'Cuota' }];

/** Badge del turno en la lista y en el detalle (null si no lleva). */
function badgeTurno(turno) {
  if (turno.enCurso) return { tono: 'success', texto: 'Ahora' };
  if (turno.estado === 'COMPLETO') return { tono: 'error', texto: 'Completo' };
  if (turno.estado === 'CANCELADO') return { tono: 'neutral', texto: 'Cancelado' };
  return null;
}

const iniciales = (alumno) => `${alumno.nombre?.[0] ?? ''}${alumno.apellido?.[0] ?? ''}`.toUpperCase();

function TarjetaResumen({ titulo, valor, detalle }) {
  return (
    <Panel className="p-4">
      <p className="text-xs font-medium text-ink-soft">{titulo}</p>
      <p className={cx('mt-1 text-2xl font-bold tabular-nums', valor === '—' ? 'text-ink-muted' : 'text-ink')}>{valor}</p>
      {detalle && <p className="mt-1 text-xs text-ink-muted">{detalle}</p>}
    </Panel>
  );
}

function BarraOcupacion({ turno, className }) {
  return (
    <div className={cx('h-2 overflow-hidden rounded-full bg-line', className)} aria-hidden="true">
      <div className={cx('h-full rounded-full', ESTILO_TURNO[turno.estado].barra)} style={{ width: `${porcentajeOcupado(turno)}%` }} />
    </div>
  );
}

function FilaTurno({ turno, seleccionado, onSeleccionar }) {
  const bloqueado = turno.estado === 'BLOQUEADO';
  const apagado = turno.estado === 'PASADO' || turno.estado === 'CANCELADO';
  const badge = badgeTurno(turno);
  return (
    <li>
      <button
        type="button"
        onClick={() => onSeleccionar(turno.horaInicio)}
        aria-pressed={seleccionado}
        className={cx(
          'flex min-h-12 w-full cursor-pointer items-center gap-3 rounded-lg border-l-3 px-3 py-2 text-left text-sm transition-colors',
          seleccionado ? 'border-primary bg-error-bg/60' : 'border-transparent hover:bg-row-hover',
          FOCUS,
        )}
      >
        <span className={cx('w-28 shrink-0 font-medium tabular-nums', apagado ? 'text-ink-muted' : 'text-ink', turno.estado === 'CANCELADO' && 'line-through')}>
          {turno.horaInicio} – {turno.horaFin}
        </span>
        {bloqueado ? (
          <span className="flex items-center gap-1.5 text-ink-muted">
            <Lock className="h-3.5 w-3.5" /> Reservas no habilitadas
          </span>
        ) : (
          <>
            <BarraOcupacion turno={turno} className="w-full max-w-40 flex-1" />
            <span className={cx('w-12 shrink-0 text-right font-semibold tabular-nums', apagado ? 'text-ink-muted' : 'text-ink')}>
              {turno.ocupados}/{turno.cupoMaximo}
            </span>
            <span className="ml-auto w-24 shrink-0 text-right">
              {badge && <Badge tone={badge.tono}>{badge.texto}</Badge>}
            </span>
          </>
        )}
      </button>
    </li>
  );
}

function ListaTurnos({ agenda, seleccion, onSeleccionar }) {
  const grupos = [
    { titulo: 'Mañana', turnos: agenda.turnos.filter(esDeManiana) },
    { titulo: 'Tarde', turnos: agenda.turnos.filter(t => !esDeManiana(t)) },
  ].filter(grupo => grupo.turnos.length > 0);

  return (
    <Panel className="p-4 sm:p-5">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 className="text-base font-semibold text-ink">Turnos del día</h2>
        {agenda.cupoPorTurno && <p className="text-xs text-ink-soft">Cupo: {agenda.cupoPorTurno} por turno</p>}
      </div>
      {grupos.map(grupo => (
        <section key={grupo.titulo} className="mt-3 first-of-type:mt-0">
          <h3 className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-ink-muted">{grupo.titulo}</h3>
          <ul className="space-y-0.5">
            {grupo.turnos.map(turno => (
              <FilaTurno key={turno.horaInicio} turno={turno} seleccionado={turno.horaInicio === seleccion} onSeleccionar={onSeleccionar} />
            ))}
          </ul>
        </section>
      ))}
    </Panel>
  );
}

function EstadoVacio({ icono: Icono, titulo, texto }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-2 px-4 py-12 text-center">
      <span className="mb-1 flex h-14 w-14 items-center justify-center rounded-full bg-row-hover text-ink-soft">
        <Icono className="h-6 w-6" />
      </span>
      <p className="text-base font-semibold text-ink">{titulo}</p>
      {texto && <p className="max-w-sm text-sm text-ink-soft">{texto}</p>}
    </div>
  );
}

function DetalleTurno({ agenda, turno, onSeleccionarAlumno, onCancelarTurno, onReactivarTurno }) {
  const bloqueado = turno.estado === 'BLOQUEADO';
  const badge = bloqueado ? { tono: 'neutral', texto: 'Bloqueado' }
    : turno.estado === 'PASADO' ? { tono: 'neutral', texto: 'Pasado' }
      : badgeTurno(turno);

  let vacio = null;
  if (bloqueado) {
    vacio = { icono: Lock, titulo: 'Todavía no hay alumnos anotados', texto: `Las reservas para este día se habilitan el ${textoDiaYFecha(agenda.fechaHabilitacion)}.` };
  } else if (turno.estado === 'CANCELADO') {
    vacio = {
      icono: CalendarX,
      titulo: 'Turno cancelado',
      texto: turno.canceladoPorFranja
        ? 'Este turno se canceló porque su horario ya no está en la agenda.'
        : 'Este turno fue cancelado por el gimnasio.',
    };
  } else if (turno.alumnos.length === 0) {
    vacio = turno.estado === 'PASADO'
      ? { icono: Users, titulo: 'Sin reservas', texto: 'Nadie reservó este turno.' }
      : { icono: Users, titulo: 'Sin reservas', texto: 'Todavía nadie reservó este turno.' };
  }

  return (
    <Panel className="flex flex-col p-4 sm:p-6">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-lg font-bold text-ink sm:text-xl">
            {textoDiaCompleto(agenda.fecha)} · {turno.horaInicio} – {turno.horaFin} hs
          </h2>
          <p className="mt-1 text-sm text-ink-soft">
            {bloqueado ? `Cupo: ${turno.cupoMaximo} alumnos · reservas todavía no habilitadas` : textoOcupacion(turno)}
          </p>
        </div>
        {badge && <Badge tone={badge.tono} className="shrink-0">{badge.texto}</Badge>}
      </div>

      {!bloqueado && <BarraOcupacion turno={turno} className="mt-4 h-2.5" />}

      <div className="mt-5 flex flex-1 flex-col">
        {vacio ? <EstadoVacio {...vacio} /> : (
          <Table columns={COLUMNAS_ALUMNOS}>
            {turno.alumnos.map(alumno => {
              const cuota = estadoCuotaAgenda(alumno.cuota);
              return (
                <Row key={alumno.reservaId} onActivate={onSeleccionarAlumno ? () => onSeleccionarAlumno(alumno) : undefined}>
                  <td className="px-3">
                    <span className="flex items-center gap-3">
                      <span aria-hidden="true" className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-error-bg text-xs font-semibold text-primary">
                        {iniciales(alumno)}
                      </span>
                      <span className="font-medium">{alumno.nombre} {alumno.apellido}</span>
                    </span>
                  </td>
                  <td className="px-3 text-ink-soft">{alumno.membresiaNombre ?? 'Sin membresía'}</td>
                  <td className="px-3">{cuota ? <Badge tone={cuota.tono}>{cuota.texto}</Badge> : <span className="text-ink-muted">—</span>}</td>
                </Row>
              );
            })}
          </Table>
        )}
      </div>

      <div className="mt-6 flex justify-end border-t border-line pt-4">
        {onCancelarTurno && turno.estado !== 'PASADO' && turno.estado !== 'CANCELADO' && !turno.enCurso && (
          <Button variant="danger" onClick={() => onCancelarTurno(turno)}>
            <CalendarX className="h-4 w-4" /> Cancelar este turno
          </Button>
        )}
        {onReactivarTurno && turno.puedeReactivarse && (
          <Button variant="secondary" onClick={() => onReactivarTurno(turno)}>
            <CalendarCheck className="h-4 w-4" /> Reactivar turno
          </Button>
        )}
      </div>
    </Panel>
  );
}

/**
 * MOGNI-257: vista Día de la agenda. Tarjetas de resumen, turnos agrupados en mañana y tarde, y el detalle del turno
 * seleccionado con sus alumnos. seleccion: horaInicio del turno elegido.
 */
export default function AgendaDia({ agenda, seleccion, onSeleccionar, onSeleccionarAlumno, onCancelarTurno, onReactivarTurno }) {
  const bloqueado = agenda.estadoDia === 'BLOQUEADO';
  const resumen = agenda.resumen;
  const turnoSeleccionado = agenda.turnos.find(t => t.horaInicio === seleccion);
  const cantidadTurnos = resumen?.cantidadTurnos ?? agenda.turnos.filter(t => t.estado !== 'CANCELADO').length;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
        <TarjetaResumen
          titulo="Turnos del día"
          valor={cantidadTurnos}
          detalle={agenda.cupoPorTurno ? `Cupo por turno: ${agenda.cupoPorTurno} alumnos` : null}
        />
        <TarjetaResumen titulo="Reservas" valor={resumen?.reservas ?? '—'} />
        <TarjetaResumen titulo="Turnos completos" valor={resumen?.turnosCompletos ?? '—'} />
        <TarjetaResumen titulo="Lugares libres" valor={resumen?.lugaresLibres ?? '—'} />
      </div>

      {bloqueado && (
        <p role="status" className="flex items-center gap-3 rounded-lg bg-thead/60 px-4 py-3 text-sm text-ink-soft">
          <Lock className="h-4 w-4 shrink-0" />
          Los alumnos todavía no pueden reservar este día. Las reservas se habilitan el {textoDiaYFecha(agenda.fechaHabilitacion)}.
        </p>
      )}

      {agenda.estadoDia === 'SIN_HORARIOS' ? (
        <Panel className="flex">
          <EstadoVacio
            icono={CalendarOff}
            titulo="Sin horarios"
            texto={agenda.fecha < agenda.hoy
              ? 'No hay turnos registrados para este día.'
              : 'No hay franjas horarias cargadas para este día.'}
          />
        </Panel>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
          <ListaTurnos agenda={agenda} seleccion={seleccion} onSeleccionar={onSeleccionar} />
          {turnoSeleccionado && (
            <DetalleTurno
              agenda={agenda}
              turno={turnoSeleccionado}
              onSeleccionarAlumno={onSeleccionarAlumno}
              onCancelarTurno={onCancelarTurno}
              onReactivarTurno={onReactivarTurno}
            />
          )}
        </div>
      )}
    </div>
  );
}
