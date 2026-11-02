import { Fragment, useEffect, useState } from 'react';
import { ChevronRight, CircleAlert, Copy, Info, Lock, Pencil, Plus, Trash2, UserPlus } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Badge, Button, Cargando, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { duplicarPlantilla, eliminarPlantilla, listarPlantillas, textoNivel, textoObjetivo, tonoNivel } from '../services/plantillasService';
import TarjetasSesiones from '../components/TarjetasSesiones';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

function BotonAccion({ icono: Icono, etiqueta, onClick, peligro = false, disabled = false, title }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={etiqueta}
      title={title ?? etiqueta}
      className={cx(
        'rounded-lg p-2 transition-colors',
        disabled
          ? 'cursor-not-allowed text-line'
          : peligro
            ? 'cursor-pointer bg-error-bg text-primary hover:bg-primary hover:text-on-dark'
            : 'cursor-pointer text-ink-soft hover:bg-row-hover hover:text-ink',
        FOCUS,
      )}
    >
      <Icono className="h-4 w-4" />
    </button>
  );
}

/** Al desplegar una plantilla: sus sesiones con los ejercicios. */
function DetalleSesiones({ plantilla }) {
  return (
    <div className="px-4 pb-4 pl-12">
      <TarjetasSesiones sesiones={plantilla.sesiones} />
      {plantilla.asignacionesActivas > 0 && (
        <p className="mt-3 flex items-center gap-1.5 text-xs text-ink-soft">
          <Lock className="h-3.5 w-3.5" /> No se puede eliminar una plantilla con asignaciones activas.
        </p>
      )}
    </div>
  );
}

export default function PlantillasList({ onAdd, onEdit, onAsignar, onToast }) {
  const [plantillas, setPlantillas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [abiertas, setAbiertas] = useState({});
  const [aEliminar, setAEliminar] = useState(null);
  const [duplicando, setDuplicando] = useState(null);

  useEffect(() => {
    listarPlantillas()
      .then(setPlantillas)
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, []);

  const toggle = (id) => setAbiertas(prev => ({ ...prev, [id]: !prev[id] }));

  const duplicar = async (plantilla) => {
    setDuplicando(plantilla.id);
    try {
      const copia = await duplicarPlantilla(plantilla.id);
      // La copia es la última modificada: va primero, como el resto de la lista
      setPlantillas(prev => [copia, ...prev]);
      onToast(`Se creó la plantilla "${copia.nombre}".`);
    } catch (err) {
      onToast(err.message, true);
    } finally {
      setDuplicando(null);
    }
  };

  const confirmarEliminacion = async () => {
    const plantilla = aEliminar;
    setAEliminar(null);
    try {
      await eliminarPlantilla(plantilla.id);
      setPlantillas(prev => prev.filter(p => p.id !== plantilla.id));
      onToast('La plantilla fue eliminada correctamente.');
    } catch (err) {
      // 404: ya la habían eliminado desde otra pestaña o sesión; no tiene que quedar en la lista
      if (err.status === 404) setPlantillas(prev => prev.filter(p => p.id !== plantilla.id));
      onToast(err.message, true);
    }
  };

  return (
    <div className="mx-auto max-w-5xl animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${plural(plantillas.length, 'plantilla', 'plantillas')}`}
        title="Plantillas de plan de entrenamiento"
        actions={
          <>
            <Button variant="secondary" onClick={() => onAsignar()}>
              <UserPlus className="h-4 w-4" /> Asignar plan a un alumno
            </Button>
            <Button variant="primary" onClick={onAdd}>
              <Plus className="h-4 w-4" /> Agregar nueva plantilla
            </Button>
          </>
        }
      />

      {cargando ? (
        <Cargando texto="Cargando plantillas" />
      ) : error ? (
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      ) : (
        <>
          <Panel className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="border-b border-line">
                  <tr className="text-[11px] font-semibold uppercase tracking-wide text-ink-soft">
                    <th scope="col" className="h-11 w-10 px-3"><span className="sr-only">Desplegar</span></th>
                    <th scope="col" className="h-11 px-3">Nombre de la plantilla</th>
                    <th scope="col" className="h-11 w-32 px-3">Nivel</th>
                    <th scope="col" className="h-11 w-40 px-3">Objetivo</th>
                    <th scope="col" className="h-11 w-28 px-3">Sesiones</th>
                    <th scope="col" className="h-11 w-44 px-3">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line-soft">
                  {plantillas.length === 0 && (
                    <tr>
                      <td colSpan={6} className="px-3 py-12 text-center text-sm text-ink-muted">
                        Todavía no hay plantillas. Creá la primera con "Agregar nueva plantilla".
                      </td>
                    </tr>
                  )}
                  {plantillas.map(plantilla => {
                    const abierta = Boolean(abiertas[plantilla.id]);
                    const asignada = plantilla.asignacionesActivas > 0;
                    return (
                      <Fragment key={plantilla.id}>
                        <tr className={cx('align-middle', abierta && 'bg-row-hover/40')}>
                          <td className="px-3 py-3">
                            <button
                              type="button"
                              onClick={() => toggle(plantilla.id)}
                              aria-expanded={abierta}
                              aria-label={abierta ? `Ocultar sesiones de ${plantilla.nombre}` : `Ver sesiones de ${plantilla.nombre}`}
                              className={cx('cursor-pointer rounded-md p-1 text-ink-soft transition-colors hover:bg-row-hover hover:text-ink', FOCUS)}
                            >
                              <ChevronRight className={cx('h-4 w-4 transition-transform duration-200', abierta && 'rotate-90')} />
                            </button>
                          </td>
                          <td className="px-3 py-3">
                            <p className="font-semibold text-ink">{plantilla.nombre}</p>
                            {asignada
                              ? <Badge tone="error" className="mt-1 px-2 py-0.5 text-[11px]">{plural(plantilla.asignacionesActivas, 'asignación activa', 'asignaciones activas')}</Badge>
                              : <p className="mt-0.5 text-xs text-ink-muted">Sin asignaciones activas</p>}
                          </td>
                          <td className="px-3 py-3"><Badge tone={tonoNivel(plantilla.nivel)}>{textoNivel(plantilla.nivel)}</Badge></td>
                          <td className="px-3 py-3"><Badge>{textoObjetivo(plantilla.objetivo)}</Badge></td>
                          <td className="px-3 py-3 tabular-nums text-ink-soft">{plural(plantilla.sesiones.length, 'sesión', 'sesiones')}</td>
                          <td className="px-3 py-3">
                            <div className="flex items-center gap-1">
                              <BotonAccion icono={UserPlus} etiqueta={`Asignar ${plantilla.nombre} a un alumno`} title="Asignar a un alumno"
                                onClick={() => onAsignar(plantilla.id)} />
                              <BotonAccion icono={Copy} etiqueta={`Duplicar ${plantilla.nombre}`} title="Duplicar"
                                onClick={() => duplicar(plantilla)} disabled={duplicando === plantilla.id} />
                              <BotonAccion icono={Pencil} etiqueta={`Modificar ${plantilla.nombre}`} title="Modificar"
                                onClick={() => onEdit(plantilla.id)} />
                              <BotonAccion icono={Trash2} etiqueta={`Eliminar ${plantilla.nombre}`} peligro
                                disabled={asignada}
                                title={asignada ? 'No se puede eliminar: tiene asignaciones activas' : 'Eliminar'}
                                onClick={() => setAEliminar(plantilla)} />
                            </div>
                          </td>
                        </tr>
                        {abierta && (
                          <tr className="bg-row-hover/40">
                            <td colSpan={6}><DetalleSesiones plantilla={plantilla} /></td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {plantillas.length > 0 && (
              <div className="flex flex-wrap items-center justify-between gap-2 border-t border-line px-4 py-3 text-xs text-ink-soft">
                <span>Mostrando {plantillas.length} de {plural(plantillas.length, 'plantilla', 'plantillas')}</span>
                <span>Última actualización primero</span>
              </div>
            )}
          </Panel>

          <p className="mt-4 flex items-center gap-1.5 text-xs text-ink-soft">
            <Info className="h-3.5 w-3.5 shrink-0" /> Al duplicar una plantilla se crea una copia independiente, con sus propias sesiones y ejercicios.
          </p>
        </>
      )}

      <ConfirmModal
        isOpen={Boolean(aEliminar)}
        title="Eliminar plantilla"
        message={aEliminar && `¿Desea eliminar la plantilla "${aEliminar.nombre}"? Dejará de estar disponible para asignar.`}
        danger
        onConfirm={confirmarEliminacion}
        onCancel={() => setAEliminar(null)}
      />
    </div>
  );
}
