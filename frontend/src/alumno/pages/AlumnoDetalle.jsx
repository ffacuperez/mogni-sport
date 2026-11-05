import { useEffect, useState } from 'react';
import { ArrowLeft, ArrowRight, Pencil, Power, RefreshCw, TrendingUp, UserPlus } from 'lucide-react';

import { formatearFecha } from '../../shared/services/texto';
import { Badge, Button, PageHeader, Panel } from '../../shared/components/ui';
import { obtenerPlanActivo } from '../../plan/services/planesService';
import { textoNivel, textoObjetivo } from '../../plantilla/services/plantillasService';

const TONO_ESTADO_INSCRIPCION = {
  Activa: 'success',
  Cancelada: 'error',
};

function Dato({ label, children, ancho = false, mono = false }) {
  return (
    <div className={ancho ? 'md:col-span-2' : ''}>
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
function SeccionPlan({ alumnoId, isActivo, onVerPlan, onAsignarPlan, onVerProgreso }) {
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

  let accionPlan = null;
  if (plan) {
    accionPlan = (
      <Button variant="secondary" size="sm" onClick={() => onVerPlan(plan.id)}>
        Ver plan <ArrowRight className="h-3.5 w-3.5" />
      </Button>
    );
  } else if (plan === null && isActivo) {
    accionPlan = (
      <Button variant="secondary" size="sm" onClick={onAsignarPlan}>
        <UserPlus className="h-3.5 w-3.5" /> Asignar plan
      </Button>
    );
  }
  // MOGNI-268: siempre, también sin plan activo: el historial puede tener planes anteriores y sesiones
  const acciones = (
    <div className="flex flex-wrap gap-2">
      <Button variant="secondary" size="sm" onClick={onVerProgreso}>
        <TrendingUp className="h-3.5 w-3.5" /> Ver progreso
      </Button>
      {accionPlan}
    </div>
  );

  return (
    <Seccion titulo="Plan de entrenamiento" accion={acciones}>
      {error && <p role="alert" className="text-sm text-error">No se pudo consultar el plan: {error}</p>}
      {!error && plan === undefined && <p className="text-sm text-ink-soft">Cargando plan...</p>}
      {plan === null && <p className="text-sm text-ink-soft">Sin plan activo.</p>}
      {plan && (
        <dl className="grid grid-cols-1 gap-5 md:grid-cols-2">
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

export default function AlumnoDetalle({ alumno, onBack, onEdit, onEditMembresia, onToggleStatus, puedeVerPlanes, onVerPlan, onAsignarPlan, onVerProgreso }) {
  const isActivo = alumno.estado === 'Activo';
  const inscripcion = alumno.inscripcion;
  const iniciales = `${alumno.nombre?.[0] ?? ''}${alumno.apellido?.[0] ?? ''}`;

  return (
    <div className="mx-auto max-w-4xl animate-fade-in">
      <Button variant="ghost" size="sm" onClick={onBack} className="-ml-3 mb-4">
        <ArrowLeft className="h-4 w-4" /> Volver al listado
      </Button>

      <PageHeader
        eyebrow={`Alumno · DNI ${alumno.dni}`}
        title={`${alumno.nombre} ${alumno.apellido}`}
        actions={
          <Button variant={isActivo ? 'danger' : 'primary'} onClick={onToggleStatus}>
            <Power className="h-4 w-4" /> {isActivo ? 'Dar de baja' : 'Activar'}
          </Button>
        }
      >
        <div className="mt-3 flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-error-bg font-bold text-primary">
            {iniciales}
          </span>
          <Badge tone={isActivo ? 'success' : 'error'}>Alumno {alumno.estado.toLowerCase()}</Badge>
        </div>
      </PageHeader>

      <div className="grid grid-cols-1 gap-6">
        <Seccion
          titulo="Datos personales"
          accion={
            <Button variant="secondary" size="sm" onClick={onEdit}>
              <Pencil className="h-3.5 w-3.5" /> Modificar alumno
            </Button>
          }
        >
          <dl className="grid grid-cols-1 gap-5 md:grid-cols-2">
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
            <dl className="grid grid-cols-1 gap-5 md:grid-cols-2">
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
          <SeccionPlan alumnoId={alumno.id} isActivo={isActivo} onVerPlan={onVerPlan} onAsignarPlan={onAsignarPlan} onVerProgreso={onVerProgreso} />
        )}
      </div>
    </div>
  );
}
