import { Lock } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { aMinutos, etiquetaTurno } from '../services/franjasService';
import { ESTILO_TURNO, textoDiaCorto, textoDiaYFecha } from '../services/agendaService';

const COLUMNAS = 'grid grid-cols-[3.5rem_repeat(7,minmax(0,1fr))]';
/** Alto de cada fila de turno, en píxeles. */
const ALTO_FILA = 41;

const horaEntera = (minutos) => `${String(Math.floor(minutos / 60)).padStart(2, '0')}:00`;

/**
 * Filas de la grilla: un turno por cada hora de inicio que aparece en algún día de la semana. Entre dos turnos que
 * no son consecutivos queda un hueco "Sin franjas horarias" (por ejemplo, de 12 a 16).
 */
function armarFilas(dias) {
  const horas = [...new Set(dias.flatMap(d => d.turnos.map(t => t.horaInicio)))].sort();
  const filas = [];
  horas.forEach((hora, i) => {
    if (i > 0) {
      const finAnterior = aMinutos(horas[i - 1]) + 60;
      const inicio = aMinutos(hora);
      if (inicio > finAnterior) {
        const etiquetas = [];
        for (let m = finAnterior; m < inicio; m += 60) etiquetas.push(horaEntera(m));
        filas.push({ tipo: 'hueco', clave: `hueco-${hora}`, desde: finAnterior, hasta: inicio, etiquetas });
      }
    }
    filas.push({ tipo: 'turno', clave: hora, hora });
  });
  return filas;
}

function CabeceraDia({ dia, hoy }) {
  const esHoy = dia.fecha === hoy;
  let detalle;
  if (dia.estadoDia === 'RESERVAS_ABIERTAS') {
    detalle = <span className="rounded-full bg-success-bg px-2 py-0.5 font-medium text-success">Reservas abiertas</span>;
  } else if (dia.estadoDia === 'BLOQUEADO') {
    detalle = (
      <span className="flex items-center justify-center gap-1" title={`Se habilita el ${textoDiaYFecha(dia.fechaHabilitacion)}`}>
        <Lock className="h-3 w-3 shrink-0" /> Se habilita el día anterior
      </span>
    );
  } else {
    detalle = dia.estadoDia === 'PASADO' ? 'Pasado' : 'Sin horarios';
  }
  return (
    <div className={cx('flex flex-col items-center justify-center gap-1 border-l border-line px-1 py-2.5 text-center', esHoy && 'bg-error-bg/60')}>
      <p className={cx('text-sm font-semibold', esHoy ? 'text-primary' : dia.estadoDia === 'PASADO' ? 'text-ink-soft' : 'text-ink')}>
        {textoDiaCorto(dia.fecha)}{esHoy && ' · Hoy'}
      </p>
      <div className="text-[10px] leading-tight text-ink-soft">{detalle}</div>
    </div>
  );
}

function CeldaTurno({ dia, turno, onAbrir }) {
  const estilo = ESTILO_TURNO[turno.estado];
  const bloqueado = turno.estado === 'BLOQUEADO';
  const cancelado = turno.estado === 'CANCELADO';
  const descripcion = bloqueado ? 'reservas no habilitadas'
    : cancelado ? 'cancelado'
      : `${turno.ocupados} de ${turno.cupoMaximo} lugares ocupados`;
  return (
    <button
      type="button"
      onClick={() => onAbrir(dia.fecha, turno.horaInicio)}
      aria-label={`${textoDiaCorto(dia.fecha)}, ${turno.horaInicio} a ${turno.horaFin}: ${descripcion}. Ver el día.`}
      className={cx(
        'relative flex h-full w-full cursor-pointer items-center justify-between gap-1 rounded-md px-2 text-xs transition-opacity hover:opacity-80',
        estilo.celda,
        turno.enCurso && 'ring-2 ring-primary',
        FOCUS,
      )}
    >
      {turno.enCurso && (
        <span className="absolute -top-2 right-1 rounded-full bg-primary px-1.5 text-[9px] font-semibold leading-4 text-on-dark">Ahora</span>
      )}
      <span className={cx('tabular-nums', cancelado && 'line-through')}>{etiquetaTurno(turno)}</span>
      {bloqueado && <Lock className="h-3 w-3 shrink-0" />}
      {cancelado && <span className="font-medium text-error">Cancelado</span>}
      {!bloqueado && !cancelado && <span className="text-sm font-bold tabular-nums">{turno.ocupados}/{turno.cupoMaximo}</span>}
    </button>
  );
}

function Leyenda() {
  return (
    <ul className="mb-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-ink-soft">
      {Object.entries(ESTILO_TURNO).map(([estado, { celda, leyenda }]) => (
        <li key={estado} className="flex items-center gap-1.5">
          <span aria-hidden="true" className={cx('h-3 w-3 rounded-sm', celda)} />
          <span className={cx(estado === 'CANCELADO' && 'line-through')}>{leyenda}</span>
        </li>
      ))}
    </ul>
  );
}

/**
 * MOGNI-257: vista Semana de la agenda, días × horas con la ocupación de cada turno. Al hacer clic en un turno se
 * abre la vista Día de esa fecha con ese turno seleccionado.
 */
export default function AgendaSemana({ semana, onAbrirTurno }) {
  const filas = armarFilas(semana.dias);
  const filasDeTurnos = filas.filter(f => f.tipo === 'turno');
  // En los días sin horarios el texto va en la fila del medio del primer bloque de turnos
  const primerHueco = filas.findIndex(f => f.tipo === 'hueco');
  const primerBloque = primerHueco === -1 ? filas.length : primerHueco;
  const filaSinHorarios = filas[Math.floor((primerBloque - 1) / 2)]?.clave;

  return (
    <div>
      <Leyenda />
      {filasDeTurnos.length === 0 ? (
        <p className="rounded-lg border border-line px-4 py-12 text-center text-sm text-ink-muted">
          No hay turnos ni franjas horarias para esta semana.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <div className="min-w-[860px] overflow-hidden rounded-lg border border-line">
            <div className={cx(COLUMNAS, 'border-b border-line')}>
              <span />
              {semana.dias.map(dia => <CabeceraDia key={dia.fecha} dia={dia} hoy={semana.hoy} />)}
            </div>

            {filas.map(fila => (
              fila.tipo === 'hueco' ? (
                <div key={fila.clave} className={cx(COLUMNAS, 'border-b border-line')}>
                  <div className="flex flex-col">
                    {fila.etiquetas.map(etiqueta => (
                      <span key={etiqueta} className="px-2 pt-1 text-[11px] tabular-nums text-ink-muted" style={{ height: ALTO_FILA }}>{etiqueta}</span>
                    ))}
                  </div>
                  <div
                    className="col-span-7 flex items-center justify-center border-l border-line text-xs font-medium text-ink-soft"
                    style={{ backgroundImage: 'repeating-linear-gradient(135deg, var(--color-line-soft) 0 8px, var(--color-thead) 8px 9px)' }}
                  >
                    Sin franjas horarias · {Math.floor(fila.desde / 60)} a {Math.floor(fila.hasta / 60)} hs
                  </div>
                </div>
              ) : (
                <div key={fila.clave} className={cx(COLUMNAS, 'border-b border-line last:border-b-0')}>
                  <span className="px-2 pt-1 text-[11px] tabular-nums text-ink-muted">{fila.hora}</span>
                  {semana.dias.map(dia => {
                    const turno = dia.turnos.find(t => t.horaInicio === fila.hora);
                    return (
                      <div key={dia.fecha} className={cx('border-l border-line p-0.5', dia.fecha === semana.hoy && 'bg-error-bg/30')} style={{ height: ALTO_FILA }}>
                        {turno && <CeldaTurno dia={dia} turno={turno} onAbrir={onAbrirTurno} />}
                        {!turno && dia.estadoDia === 'SIN_HORARIOS' && fila.clave === filaSinHorarios && (
                          <p className="flex h-full items-center justify-center text-xs text-ink-muted">Sin horarios</p>
                        )}
                      </div>
                    );
                  })}
                </div>
              )
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
