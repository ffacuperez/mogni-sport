import { useEffect, useState } from 'react';
import { ArrowRight, Pencil, Power, RefreshCw, UserPlus } from 'lucide-react';

import { formatearFecha } from '../../shared/services/texto';
import { Badge, Button, Panel } from '../../shared/components/ui';
import { obtenerPlanActivo } from '../../plan/services/planesService';
import { textoNivel, textoObjetivo } from '../../plantilla/services/plantillasService';

const TONO_ESTADO_INSCRIPCION = {
  Activa: 'success',
  Cancelada: 'error',
};

function Dato({ label, children, ancho = false, mono = false }) {
  return (
    <div className={ancho ? 'sm:col-span-2' : ''}>
      <dt className="text-xs font-medium text-ink-soft">{label}</dt>
      <dd className={`mt-1 break-words text-ink ${mono ? 'tabular-nums' : ''}`}>{children || '—'}</dd>
    </div>
  );
}

function Seccion({ titulo, badge, accion, children }) {
  return (
    <Panel className="p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-line pb-4">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-base font-semibold text-ink">{titulo}</h2>
          {badge}
        </div>
        {accion}
      </div>
      {children}
    </Panel>
  );
}

/**
 * HU-266: el plan activo del alumno. Se consulta acá y no se toma del alumno porque el listado queda en memoria y
 * puede ser anterior a la última asignación. La baja finaliza el plan, así que se vuelve a consultar al cambiar el estado.
 * Un alumno dado de baja antes de esa regla puede tener todavía un plan activo: se puede ver y finalizar.
 */
function SeccionPlan({ alumnoId, isActivo, onVerPlan, onAsignarPlan }) {
  // undefined: cargando; null: no tiene plan activo
  const [plan, setPlan] = useState(undefined);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    obtenerPlanActivo(alumnoId)
      .then((activo) => { if (vigente) setPlan(activo); })
      .catch((err) => { if (vigente) setError(err.message); });
    return () => { vigente = false; };
  }, [alumnoId, isActivo]);

  let accion = null;
  if (plan) {
    accion = (
      <Button variant="secondary" size="sm" onClick={() => onVerPlan(plan.id)}>
        Ver plan <ArrowRight className="h-3.5 w-3.5" />
      </Button>
    );
  } else if (plan === null && isActivo) {
    accion = (
      <Button variant="secondary" size="sm" onClick={onAsignarPlan}>
        <UserPlus className="h-3.5 w-3.5" /> Asignar plan
      </Button>
    );
  }

  return (
    <Seccion titulo="Plan de entrenamiento" accion={accion}>
      {error && <p role="alert" className="text-sm text-error">No se pudo consultar el plan: {error}</p>}
      {!error && plan === undefined && <p className="text-sm text-ink-soft">Cargando plan...</p>}
      {plan === null && <p className="text-sm text-ink-soft">Sin plan activo.</p>}
      {plan && (
        <dl className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <Dato label="Plan" ancho>{plan.nombre}</Dato>
          <Dato label="Nivel">{textoNivel(plan.nivel)}</Dato>
          <Dato label="Objetivo">{textoObjetivo(plan.objetivo)}</Dato>
          <Dato label="Fecha de inicio" mono>{formatearFecha(plan.fechaAsignacion)}</Dato>
          {/* HU-266: los planes asignados antes de la fecha fin no la tienen */}
          <Dato label="Fecha fin" mono>{plan.fechaFin ? formatearFecha(plan.fechaFin) : 'Sin fecha fin'}</Dato>
        </dl>
      )}
    </Seccion>
  );
}

/**
 * Tab "Datos" de la ficha: datos personales a la izquierda; membresía y plan a la derecha. Las acciones del alumno
 * (modificar, dar de baja o activar) van en sus datos, porque el encabezado de la ficha solo muestra el estado.
 */
export default function DatosAlumno({ alumno, onEdit, onEditMembresia, onToggleStatus, puedeVerPlanes, onVerPlan, onAsignarPlan }) {
  const isActivo = alumno.estado === 'Activo';
  const inscripcion = alumno.inscripcion;

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
      <Seccion
        titulo="Datos personales"
        accion={
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" onClick={onEdit}>
              <Pencil className="h-3.5 w-3.5" /> Modificar alumno
            </Button>
            <Button variant={isActivo ? 'danger' : 'primary'} size="sm" onClick={onToggleStatus}>
              <Power className="h-3.5 w-3.5" /> {isActivo ? 'Dar de baja' : 'Activar'}
            </Button>
          </div>
        }
      >
        <dl className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <Dato label="Nombre">{alumno.nombre}</Dato>
          <Dato label="Apellido">{alumno.apellido}</Dato>
          <Dato label="DNI" mono>{alumno.dni}</Dato>
          <Dato label="Email">{alumno.email}</Dato>
          <Dato label="Fecha de nacimiento" mono>{formatearFecha(alumno.fechaNacimiento)}</Dato>
          <Dato label="Edad">{alumno.edad != null ? `${alumno.edad} años` : ''}</Dato>
          <Dato label="Teléfono" mono>{alumno.telefono}</Dato>
          <Dato label="Sexo">{alumno.sexo}</Dato>
          <Dato label="Objetivo" ancho>{alumno.objetivo}</Dato>
          <Dato label="Antecedentes de salud" ancho>{alumno.antecedentes}</Dato>
        </dl>
      </Seccion>

      <div className="grid grid-cols-1 gap-6">
        <Seccion
          titulo="Membresía"
          badge={inscripcion && (
            <Badge tone={TONO_ESTADO_INSCRIPCION[inscripcion.estado] ?? 'neutral'}>
              Inscripción {inscripcion.estado.toLowerCase()}
            </Badge>
          )}
          // La membresía de un alumno inactivo se elige al reactivarlo (regla M9)
          accion={isActivo && (
            <Button variant="secondary" size="sm" onClick={onEditMembresia}>
              <RefreshCw className="h-3.5 w-3.5" /> Modificar membresía
            </Button>
          )}
        >
          {inscripcion ? (
            <dl className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Dato label="Membresía" ancho>{inscripcion.membresia}</Dato>
              {alumno.membresiaProgramada && (
                <Dato label="Próxima membresía (ya pagada)" ancho>
                  {alumno.membresiaProgramada} a partir del {formatearFecha(alumno.programadaDesde)}
                </Dato>
              )}
              <Dato label="Fecha de inicio" mono>{formatearFecha(inscripcion.fechaInicio)}</Dato>
              {/* fechaFin solo existe si la inscripción se canceló */}
              {inscripcion.fechaFin && <Dato label="Fecha de cancelación" mono>{formatearFecha(inscripcion.fechaFin)}</Dato>}
            </dl>
          ) : (
            <p className="text-sm text-ink-soft">Sin membresía: el alumno nunca tuvo una inscripción.</p>
          )}
        </Seccion>

        {puedeVerPlanes && (
          <SeccionPlan alumnoId={alumno.id} isActivo={isActivo} onVerPlan={onVerPlan} onAsignarPlan={onAsignarPlan} />
        )}
      </div>
    </div>
  );
}
