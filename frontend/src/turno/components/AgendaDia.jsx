import { CalendarCheck, CalendarX, CalendarOff, Check, CircleAlert, Clock, Info, Lock, Users, X } from 'lucide-react';

import { Badge, Button, Panel, Row, Table } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { textoDiaCompleto } from '../services/turnosService';
import {
  ESTILO_TURNO,
  esDeManiana,
  estadoCuotaAgenda,
  porcentajeOcupado,
  textoAsistencia,
  textoDiaYFecha,
  textoOcupacion,
} from '../services/agendaService';

// HU-262: se agrega «Asistencia»; se mantiene «Membresía» (decisión del equipo sobre el mockup)
const COLUMNAS_ALUMNOS = [{ label: 'Alumno' }, { label: 'Membresía' }, { label: 'Cuota' }, { label: 'Asistencia' }];

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

/** Segunda línea de la fila del turno (HU-262): presentes y ausentes, o por qué todavía no hay asistencia. */
function DetalleFila({ turno }) {
  if (turno.estado === 'BLOQUEADO') {
    return <span className="flex items-center gap-1"><Lock className="h-3 w-3" aria-hidden="true" /> Reservas no habilitadas</span>;
  }
  if (turno.estado === 'CANCELADO') return 'Turno cancelado';
  if (!turno.comenzo) {
    return <span className="flex items-center gap-1"><Lock className="h-3 w-3" aria-hidden="true" /> Todavía no comenzó</span>;
  }
  if (turno.ocupados === 0) return 'Sin reservas';
  return textoAsistencia(turno);
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
          'flex min-h-14 w-full cursor-pointer items-center justify-between gap-3 rounded-lg border-l-3 px-3 py-1.5 text-left transition-colors',
          seleccionado ? 'border-primary bg-error-bg/60' : 'border-transparent hover:bg-row-hover',
          FOCUS,
        )}
      >
        <span className="min-w-0">
          <span className={cx('block text-sm font-semibold tabular-nums', apagado ? 'text-ink-muted' : 'text-ink', turno.estado === 'CANCELADO' && 'line-through')}>
            {turno.horaInicio} – {turno.horaFin}
          </span>
          <span className="mt-0.5 block truncate text-xs text-ink-soft">
            <DetalleFila turno={turno} />
          </span>
        </span>
        {!bloqueado && (
          <span className="flex shrink-0 items-center gap-2 text-[13px] font-medium tabular-nums text-ink-soft">
            {turno.ocupados}/{turno.cupoMaximo}
            {badge && <Badge tone={badge.tono}>{badge.texto}</Badge>}
          </span>
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

/** Contador de presentes, ausentes y sin marcar (HU-262). */
function TarjetaAsistencia({ titulo, valor, className }) {
  return (
    <div className={cx('flex-1 rounded-lg px-4 py-3', className)}>
      <p className="text-xs font-medium">{titulo}</p>
      <p className="mt-0.5 text-2xl font-bold tabular-nums">{valor}</p>
    </div>
  );
}

function Aviso({ icono: Icono, children }) {
  return (
    <p className="flex items-start gap-2.5 rounded-lg border border-line bg-input px-3.5 py-3 text-[13px] leading-relaxed text-ink-soft">
      <Icono className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </p>
  );
}

const ESTILO_MARCA = {
  PRESENTE: { texto: 'Presente', icono: Check, activo: 'border-success bg-success text-on-dark hover:bg-success', tono: 'success' },
  AUSENTE: { texto: 'Ausente', icono: X, activo: 'border-error bg-error text-on-dark hover:bg-error', tono: 'error' },
};

/**
 * Botones Presente / Ausente de una reserva (HU-262). No propagan el clic ni el teclado a la fila, que abre la ficha
 * del alumno (MOGNI-260).
 */
function MarcaAsistencia({ alumno, guardando, onMarcar }) {
  const nombre = `${alumno.nombre} ${alumno.apellido}`;
  return (
    <span
      role="group"
      aria-label={`Asistencia de ${nombre}`}
      className="flex gap-2 sm:inline-flex"
      onClick={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
    >
      {Object.entries(ESTILO_MARCA).map(([valor, estilo]) => {
        const activo = alumno.asistencia === valor;
        const Icono = estilo.icono;
        return (
          <button
            key={valor}
            type="button"
            aria-pressed={activo}
            disabled={guardando}
            onClick={() => onMarcar(alumno, valor)}
            className={cx(
              'inline-flex h-10 flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-lg border px-3 text-[13px] font-semibold transition-colors sm:flex-none',
              'disabled:cursor-wait disabled:opacity-60',
              activo ? estilo.activo : 'border-line bg-card text-ink-soft hover:bg-row-hover',
              FOCUS,
            )}
          >
            <Icono className="h-4 w-4" aria-hidden="true" /> {estilo.texto}
          </button>
        );
      })}
    </span>
  );
}

/** Celda de asistencia según el momento del turno (HU-262). */
function CeldaAsistencia({ turno, alumno, guardando, onMarcar }) {
  if (turno.asistenciaEditable && onMarcar) {
    return <MarcaAsistencia alumno={alumno} guardando={guardando} onMarcar={onMarcar} />;
  }
  // Día terminado (o sin permiso para marcar): solo se muestra la marca
  const marca = ESTILO_MARCA[alumno.asistencia];
  if (marca) return <Badge tone={marca.tono}>{marca.texto}</Badge>;
  return <span className="text-ink-muted">{turno.comenzo ? 'Sin marcar' : `Desde las ${turno.horaInicio}`}</span>;
}

function DetalleTurno({ agenda, turno, onSeleccionarAlumno, onCancelarTurno, onReactivarTurno, onMarcarAsistencia, guardandoAsistencia, errorAsistencia }) {
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
    <Panel className="flex min-w-0 flex-col p-4 sm:p-6">
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

      {/* HU-262: asistencia del turno (solo si tiene alumnos) */}
      {!vacio && turno.comenzo && (
        <div className="mt-5 flex gap-3">
          <TarjetaAsistencia titulo="Presentes" valor={turno.presentes} className="bg-success-bg text-success" />
          <TarjetaAsistencia titulo="Ausentes" valor={turno.ausentes} className="bg-error-bg text-error" />
          {turno.asistenciaEditable && (
            <TarjetaAsistencia titulo="Sin marcar" valor={turno.sinMarcar} className="bg-page text-ink" />
          )}
        </div>
      )}
      {!vacio && (
        <div className="mt-4">
          {turno.asistenciaEditable ? (
            <Aviso icono={Info}>
              Podés corregir las marcas hasta el final del día. Las reservas que queden sin marcar al terminar el día
              pasan a «Ausente»: cuentan para el límite semanal y no generan clase extra.
            </Aviso>
          ) : turno.comenzo ? (
            <Aviso icono={Lock}>
              El día de este turno ya terminó y la asistencia quedó cerrada. Las reservas que no se marcaron pasaron a «Ausente».
            </Aviso>
          ) : (
            <Aviso icono={Clock}>
              La asistencia se registra desde que empieza el turno, a las {turno.horaInicio}.
            </Aviso>
          )}
        </div>
      )}
      {!vacio && errorAsistencia && (
        <div role="alert" className="mt-3 flex items-center gap-2.5 rounded-lg border border-line bg-error-bg px-3.5 py-3 text-sm text-error">
          <CircleAlert className="h-4 w-4 shrink-0" aria-hidden="true" />
          <span>{errorAsistencia}</span>
        </div>
      )}

      <div className="mt-5 flex min-w-0 flex-1 flex-col">
        {vacio ? <EstadoVacio {...vacio} /> : (
          <>
          {/* Celular (guía 6: en pantallas chicas la tabla pasa a lista de tarjetas) */}
          <ul className="space-y-3 sm:hidden">
            {turno.alumnos.map(alumno => {
              const cuota = estadoCuotaAgenda(alumno.cuota);
              return (
                <li key={alumno.reservaId} className="rounded-lg border border-line p-3">
                  <button
                    type="button"
                    onClick={onSeleccionarAlumno ? () => onSeleccionarAlumno(alumno) : undefined}
                    className={cx('flex w-full cursor-pointer items-center gap-3 rounded-md text-left', FOCUS)}
                  >
                    <span aria-hidden="true" className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-error-bg text-xs font-semibold text-primary">
                      {iniciales(alumno)}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-ink">{alumno.nombre} {alumno.apellido}</span>
                      <span className="block truncate text-xs text-ink-soft">{alumno.membresiaNombre ?? 'Sin membresía'}</span>
                    </span>
                    {cuota && <Badge tone={cuota.tono} className="shrink-0">{cuota.texto}</Badge>}
                  </button>
                  <div className="mt-3 text-sm">
                    <CeldaAsistencia
                      turno={turno}
                      alumno={alumno}
                      guardando={guardandoAsistencia?.has(alumno.reservaId) ?? false}
                      onMarcar={onMarcarAsistencia}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
          <div className="hidden sm:block">
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
                  <td className="min-w-24 whitespace-normal px-3 text-ink-soft">{alumno.membresiaNombre ?? 'Sin membresía'}</td>
                  <td className="px-3">{cuota ? <Badge tone={cuota.tono}>{cuota.texto}</Badge> : <span className="text-ink-muted">—</span>}</td>
                  <td className="px-3 py-2">
                    <CeldaAsistencia
                      turno={turno}
                      alumno={alumno}
                      guardando={guardandoAsistencia?.has(alumno.reservaId) ?? false}
                      onMarcar={onMarcarAsistencia}
                    />
                  </td>
                </Row>
              );
            })}
          </Table>
          </div>
          </>
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
 * HU-262: en los turnos que ya comenzaron se marca la asistencia (onMarcarAsistencia(alumno, 'PRESENTE' | 'AUSENTE')).
 * guardandoAsistencia: Set con los reservaId que se están guardando. errorAsistencia: { turnoId, mensaje } o null.
 */
export default function AgendaDia({
  agenda, seleccion, onSeleccionar, onSeleccionarAlumno, onCancelarTurno, onReactivarTurno,
  onMarcarAsistencia, guardandoAsistencia, errorAsistencia,
}) {
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
        <div className="grid gap-6 lg:grid-cols-[minmax(0,4fr)_minmax(0,8fr)]">
          <ListaTurnos agenda={agenda} seleccion={seleccion} onSeleccionar={onSeleccionar} />
          {turnoSeleccionado && (
            <DetalleTurno
              agenda={agenda}
              turno={turnoSeleccionado}
              onSeleccionarAlumno={onSeleccionarAlumno}
              onCancelarTurno={onCancelarTurno}
              onReactivarTurno={onReactivarTurno}
              onMarcarAsistencia={onMarcarAsistencia}
              guardandoAsistencia={guardandoAsistencia}
              errorAsistencia={errorAsistencia?.turnoId === turnoSeleccionado.turnoId ? errorAsistencia.mensaje : null}
            />
          )}
        </div>
      )}
    </div>
  );
}
