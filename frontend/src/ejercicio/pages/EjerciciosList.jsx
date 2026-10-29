import { useState } from 'react';
import { Ban, ChevronDown, Pencil, Plus, RotateCcw, Trash2, X } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Button, PageHeader, Panel, Row, SelectDot } from '../../shared/components/ui';

const GRUPOS_MUSCULARES = ['Piernas', 'Espalda', 'Pecho', 'Hombros', 'Brazos', 'Core', 'Glúteos'];

const cantidad = (n) => `${n} ${n === 1 ? 'ejercicio' : 'ejercicios'}`;
const estaActivo = (ejercicio) => ejercicio.activo !== false;

function GrupoMuscular({ grupo, ejercicios, abierto, onToggle, selectedIds, onToggleSeleccion }) {
  const panelId = `grupo-${grupo}`;
  return (
    <div>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={abierto}
        aria-controls={panelId}
        className={cx(
          'flex h-12 w-full cursor-pointer items-center justify-between gap-3 bg-thead px-4.5 text-left transition hover:brightness-95',
          abierto ? 'rounded-t-lg' : 'rounded-lg',
          FOCUS,
        )}
      >
        <span className="flex min-w-0 items-center gap-2.5">
          <span className="truncate text-sm font-semibold text-ink">{grupo}</span>
          <span className="whitespace-nowrap rounded-full bg-card px-2.5 py-0.5 text-xs font-medium text-ink-soft">
            {cantidad(ejercicios.length)}
          </span>
        </span>
        <ChevronDown className={cx('h-4 w-4 shrink-0 text-ink-soft transition-transform duration-200', abierto && 'rotate-180')} />
      </button>

      {abierto && (
        <div id={panelId} className="overflow-x-auto rounded-b-lg border border-t-0 border-line bg-card">
          {ejercicios.length === 0 ? (
            <p className="px-4.5 py-8 text-center text-sm text-ink-muted">No hay ejercicios registrados en este grupo.</p>
          ) : (
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead className="border-b border-line bg-row-hover">
                <tr>
                  <th scope="col" className="h-10 w-12 px-3"><span className="sr-only">Selección</span></th>
                  <th scope="col" className="h-10 w-[28%] px-3 text-xs font-medium text-ink-soft">Nombre</th>
                  <th scope="col" className="h-10 px-3 text-xs font-medium text-ink-soft">Descripción</th>
                  <th scope="col" className="h-10 w-32 px-4.5 text-xs font-medium text-ink-soft">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line-soft">
                {ejercicios.map((ej) => {
                  const activo = estaActivo(ej);
                  const seleccionado = selectedIds.includes(ej.id);
                  return (
                    <Row key={ej.id} selected={seleccionado} onActivate={() => onToggleSeleccion(ej.id)}>
                      <td className="px-3"><SelectDot selected={seleccionado} /></td>
                      <td className={cx('px-3 py-3 font-semibold', activo ? 'text-ink' : 'text-ink-muted')}>{ej.nombre}</td>
                      <td className={cx('px-3 py-3', activo ? 'text-ink-soft' : 'text-ink-muted')}>{ej.descripcion || '—'}</td>
                      <td className="px-4.5 py-3">
                        <Badge tone={activo ? 'success' : 'neutral'}>{activo ? 'Activo' : 'Inactivo'}</Badge>
                      </td>
                    </Row>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

// Acciones sobre la selección. Con uno solo se puede modificar; con varios, solo baja/reactivación y eliminación.
function BarraSeleccion({ seleccionados, onEdit, onCambiarEstado, onDelete, onLimpiar }) {
  if (seleccionados.length === 0) {
    return <p className="text-sm text-ink-soft">Seleccioná un ejercicio para modificarlo, cambiar su estado o eliminarlo.</p>;
  }

  const activos = seleccionados.filter(estaActivo);
  const inactivos = seleccionados.filter(e => !estaActivo(e));
  const mezclados = activos.length > 0 && inactivos.length > 0;
  const unico = seleccionados.length === 1 ? seleccionados[0] : null;

  return (
    <>
      <span className="mr-2 text-sm text-ink-soft">
        {unico
          ? <>Seleccionado: <span className="font-semibold text-ink">{unico.nombre}</span></>
          : <><span className="font-semibold tabular-nums text-ink">{seleccionados.length}</span> seleccionados</>}
      </span>
      {unico && (
        <Button variant="secondary" size="sm" onClick={() => onEdit(unico.id)}>
          <Pencil className="h-3.5 w-3.5" /> Modificar
        </Button>
      )}
      {activos.length > 0 && (
        <Button variant="secondary" size="sm" onClick={() => onCambiarEstado(activos, false)}>
          <Ban className="h-3.5 w-3.5" /> Dar de baja{mezclados && ` (${activos.length})`}
        </Button>
      )}
      {inactivos.length > 0 && (
        <Button variant="success" size="sm" onClick={() => onCambiarEstado(inactivos, true)}>
          <RotateCcw className="h-3.5 w-3.5" /> Reactivar{mezclados && ` (${inactivos.length})`}
        </Button>
      )}
      <Button variant="danger" size="sm" onClick={() => onDelete(seleccionados)}>
        <Trash2 className="h-3.5 w-3.5" /> Eliminar
      </Button>
      <Button variant="ghost" size="sm" onClick={onLimpiar} className="sm:ml-auto">
        <X className="h-3.5 w-3.5" /> Limpiar selección
      </Button>
    </>
  );
}

export default function EjerciciosList({ ejercicios = [], selectedIds, setSelectedIds, onEdit, onCambiarEstado, onDelete, onAdd }) {
  const [gruposAbiertos, setGruposAbiertos] = useState({});

  const toggleGrupo = (grupo) => setGruposAbiertos(prev => ({ ...prev, [grupo]: !prev[grupo] }));
  const toggleSeleccion = (id) => setSelectedIds(prev => (prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]));

  // Se filtra contra la lista actual: un id que ya no existe no cuenta como seleccionado
  const seleccionados = ejercicios.filter(e => selectedIds.includes(e.id));

  return (
    <div className="mx-auto max-w-5xl animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${cantidad(ejercicios.length)}`}
        title="Ejercicios"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nuevo ejercicio
          </Button>
        }
      />

      <Panel className="p-4 sm:p-5">
        <div className="mb-4 flex min-h-9 flex-wrap items-center gap-2">
          <BarraSeleccion
            seleccionados={seleccionados}
            onEdit={onEdit}
            onCambiarEstado={onCambiarEstado}
            onDelete={onDelete}
            onLimpiar={() => setSelectedIds([])}
          />
        </div>

        <div className="space-y-2">
          {GRUPOS_MUSCULARES.map((grupo) => (
            <GrupoMuscular
              key={grupo}
              grupo={grupo}
              ejercicios={ejercicios.filter(e => e.grupoMuscular === grupo)}
              abierto={Boolean(gruposAbiertos[grupo])}
              onToggle={() => toggleGrupo(grupo)}
              selectedIds={selectedIds}
              onToggleSeleccion={toggleSeleccion}
            />
          ))}
        </div>
      </Panel>
    </div>
  );
}
