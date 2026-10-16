import { useEffect, useState } from 'react';
import { CircleAlert, Info, Pencil, Plus, Repeat, Trash2 } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Button, Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import {
  actualizarFranja,
  aMinutos,
  cambiarCupoMaximo,
  crearFranja,
  DIAS_SEMANA,
  diaSemana,
  eliminarFranja,
  etiquetaTurno,
  listarFranjas,
  obtenerConfiguracionGimnasio,
} from '../services/franjasService';
import FranjaModal from './FranjaModal';
import CupoModal from './CupoModal';

/** Alto de una hora en la grilla semanal, en píxeles. */
const ALTO_HORA = 44;
/** Rango mínimo de la grilla; se amplía si alguna franja empieza antes o termina después. */
const HORA_DESDE = 7;
const HORA_HASTA = 21;

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

function BloqueFranja({ franja, desde, onEditar, onEliminar }) {
  const top = ((aMinutos(franja.horaInicio) - desde * 60) / 60) * ALTO_HORA;
  const alto = ((aMinutos(franja.horaFin) - aMinutos(franja.horaInicio)) / 60) * ALTO_HORA;
  const accion = cx('cursor-pointer rounded-md bg-card p-1 text-ink-soft shadow-card transition-colors hover:text-primary', FOCUS);
  return (
    <div
      className="group absolute inset-x-1 overflow-hidden rounded-md border-l-3 border-primary bg-error-bg px-2 py-1.5"
      style={{ top, height: alto - 2 }}
    >
      <p className="text-xs font-bold tabular-nums text-primary-hover">{franja.horaInicio} – {franja.horaFin}</p>
      {/* Un turno por línea: cada uno mide menos que la hora de la grilla, así que siempre entran en el bloque */}
      <ul className="text-[11px] leading-[14px] tabular-nums text-ink-soft" title={plural(franja.cantidadTurnos, 'turno', 'turnos')}>
        {franja.turnos.map(turno => <li key={turno.horaInicio}>{etiquetaTurno(turno)}</li>)}
      </ul>
      {/* En pantallas chicas (sin mouse) los botones se ven siempre */}
      <div className="absolute right-1 top-1 flex gap-1 md:opacity-0 md:transition-opacity md:group-hover:opacity-100 md:group-focus-within:opacity-100">
        <button type="button" onClick={() => onEditar(franja)} className={accion}
          aria-label={`Modificar franja del ${diaSemana(franja.diaSemana).singular} de ${franja.horaInicio} a ${franja.horaFin}`}>
          <Pencil className="h-3.5 w-3.5" />
        </button>
        <button type="button" onClick={() => onEliminar(franja)} className={accion}
          aria-label={`Eliminar franja del ${diaSemana(franja.diaSemana).singular} de ${franja.horaInicio} a ${franja.horaFin}`}>
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

/** Grilla de lunes a domingo con cada franja ubicada según su horario. */
function GrillaSemanal({ franjas, cupoMaximo, onEditar, onEliminar }) {
  const desde = Math.min(HORA_DESDE, ...franjas.map(f => Math.floor(aMinutos(f.horaInicio) / 60)));
  const hasta = Math.max(HORA_HASTA, ...franjas.map(f => Math.ceil(aMinutos(f.horaFin) / 60)));
  const horas = Array.from({ length: hasta - desde + 1 }, (_, i) => desde + i);
  const columnas = 'grid grid-cols-[3.5rem_repeat(7,minmax(0,1fr))]';
  const lineas = {
    height: (hasta - desde) * ALTO_HORA,
    backgroundImage: `repeating-linear-gradient(to bottom, var(--color-line) 0 1px, transparent 1px ${ALTO_HORA}px)`,
  };

  return (
    <div className="overflow-x-auto">
      {/* py-2: las etiquetas de hora van centradas sobre su línea y sobresalen un poco arriba y abajo */}
      <div className="min-w-[760px] py-2">
        <div className={cx(columnas, 'mb-3')}>
          <span />
          {DIAS_SEMANA.map(dia => {
            const turnosDelDia = franjas
              .filter(f => f.diaSemana === dia.valor)
              .reduce((total, f) => total + f.cantidadTurnos, 0);
            return (
              <div key={dia.valor} className="text-center">
                <p className={cx('text-sm font-semibold', turnosDelDia > 0 ? 'text-ink' : 'text-ink-muted')}>{dia.nombre}</p>
                {turnosDelDia > 0 && (
                  <p className="mt-0.5 text-[11px] leading-snug text-ink-soft">
                    {plural(turnosDelDia, 'turno', 'turnos')}
                    <br />
                    {plural(turnosDelDia * cupoMaximo, 'lugar', 'lugares')}
                  </p>
                )}
              </div>
            );
          })}
        </div>

        <div className={columnas}>
          <div className="relative" style={{ height: lineas.height }}>
            {horas.map(hora => (
              <span key={hora} className="absolute left-0 -translate-y-1/2 text-xs tabular-nums text-ink-muted"
                style={{ top: (hora - desde) * ALTO_HORA }}>
                {String(hora).padStart(2, '0')}:00
              </span>
            ))}
          </div>
          {DIAS_SEMANA.map(dia => {
            const delDia = franjas.filter(f => f.diaSemana === dia.valor);
            return (
              <div key={dia.valor} className="relative border-b border-l border-line" style={lineas}>
                {delDia.length === 0 && (
                  <p className="absolute inset-x-0 top-1/2 -translate-y-1/2 text-center text-sm text-ink-muted">Sin horarios</p>
                )}
                {delDia.map(franja => (
                  <BloqueFranja key={franja.id} franja={franja} desde={desde} onEditar={onEditar} onEliminar={onEliminar} />
                ))}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/**
 * HU-252: configuración de las franjas horarias semanales y del cupo máximo por turno. Solo propietario.
 * Los cambios no tocan los turnos ya generados (hoy y mañana, si alguien ya los consultó).
 */
export default function HorariosGimnasio({ onToast }) {
  const [franjas, setFranjas] = useState([]);
  const [cupoMaximo, setCupoMaximo] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  // null: cerrado; { franja: null } alta; { franja } modificación
  const [modalFranja, setModalFranja] = useState(null);
  const [editandoCupo, setEditandoCupo] = useState(false);
  const [aEliminar, setAEliminar] = useState(null);

  useEffect(() => {
    Promise.all([listarFranjas(), obtenerConfiguracionGimnasio()])
      .then(([lista, configuracion]) => {
        setFranjas(lista);
        setCupoMaximo(configuracion.cupoMaximo);
      })
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, []);

  // Se vuelve a pedir la lista para que quede ordenada igual que en el backend
  const recargarFranjas = async () => setFranjas(await listarFranjas());

  const guardarFranja = async (datos) => {
    const franja = modalFranja.franja;
    if (franja) await actualizarFranja(franja.id, datos);
    else await crearFranja(datos);
    setModalFranja(null);
    onToast(franja ? 'La franja fue modificada correctamente.' : 'La franja fue creada correctamente.');
    await recargarFranjas().catch((err) => onToast(err.message, true));
  };

  const confirmarEliminacion = async () => {
    const franja = aEliminar;
    setAEliminar(null);
    try {
      await eliminarFranja(franja.id);
      setFranjas(prev => prev.filter(f => f.id !== franja.id));
      onToast('La franja fue eliminada correctamente.');
    } catch (err) {
      onToast(err.message, true);
    }
  };

  const guardarCupo = async (cupo) => {
    const configuracion = await cambiarCupoMaximo(cupo);
    setCupoMaximo(configuracion.cupoMaximo);
    setEditandoCupo(false);
    onToast('El cupo máximo fue actualizado correctamente.');
  };

  if (cargando) return <Cargando texto="Cargando horarios" />;

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error}</span>
      </div>
    );
  }

  const diasConHorarios = new Set(franjas.map(f => f.diaSemana)).size;
  const turnosPorSemana = franjas.reduce((total, f) => total + f.cantidadTurnos, 0);

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow="Configuración"
        title="Horarios del gimnasio"
        actions={
          <Button variant="primary" onClick={() => setModalFranja({ franja: null })}>
            <Plus className="h-4 w-4" /> Agregar franja
          </Button>
        }
      />

      <div className="mb-6 inline-flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg bg-thead/60 px-4 py-2.5 text-sm">
        <Info className="h-4 w-4 shrink-0 text-ink-soft" />
        <span className="font-medium text-ink">Cada turno dura 1 hora</span>
        <span className="text-ink-muted" aria-hidden="true">·</span>
        <span className="font-medium text-ink">Cupo máximo: {plural(cupoMaximo, 'alumno', 'alumnos')} por turno</span>
        <Button variant="secondary" size="sm" onClick={() => setEditandoCupo(true)}>
          <Pencil className="h-3.5 w-3.5" /> Editar cupo
        </Button>
      </div>

      <Panel className="p-4 sm:p-5">
        <div className="mb-4 flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-base font-semibold text-ink">Franjas semanales</h2>
          <p className="text-sm text-ink-soft">
            {plural(diasConHorarios, 'día', 'días')} con horarios · {plural(turnosPorSemana, 'turno', 'turnos')} por semana
          </p>
        </div>

        <GrillaSemanal franjas={franjas} cupoMaximo={cupoMaximo} onEditar={(franja) => setModalFranja({ franja })} onEliminar={setAEliminar} />

        <p className="mt-4 flex items-center gap-2 text-sm text-ink-soft">
          <Repeat className="h-4 w-4 shrink-0" />
          Las franjas se repiten todas las semanas. El sistema divide cada franja en turnos de 1 hora. Debajo de cada día: turnos y lugares totales (turnos × cupo máximo).
        </p>
      </Panel>

      {modalFranja && (
        <FranjaModal
          franja={modalFranja.franja}
          franjas={franjas}
          onGuardar={guardarFranja}
          onCancelar={() => setModalFranja(null)}
        />
      )}

      {editandoCupo && (
        <CupoModal cupoActual={cupoMaximo} onGuardar={guardarCupo} onCancelar={() => setEditandoCupo(false)} />
      )}

      <ConfirmModal
        isOpen={Boolean(aEliminar)}
        title="Eliminar franja"
        message={aEliminar
          ? `¿Eliminar la franja del ${diaSemana(aEliminar.diaSemana).singular} de ${aEliminar.horaInicio} a ${aEliminar.horaFin}? Los turnos que ya fueron generados se mantienen.`
          : ''}
        danger
        onConfirm={confirmarEliminacion}
        onCancel={() => setAEliminar(null)}
      />
    </div>
  );
}
