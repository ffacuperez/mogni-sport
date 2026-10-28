import { Pencil, Plus, Power, Trash2 } from 'lucide-react';
import { Button, EmptyRow, PageHeader, Panel, Row, SelectDot, Table } from '../../shared/components/ui';

const COLUMNAS = [
  { label: '', className: 'w-12' },
  { label: 'Nombre' },
  { label: 'Grupo Muscular' },
  { label: 'Descripción' },
  { label: 'Estado', className: 'text-center' },
];

export default function EjerciciosList({ ejercicios, cargando, selectedId, setSelectedId, onAdd, onEdit, onToggleEstado, onDelete }) {
  const selectedEjercicio = ejercicios.find(e => e.id === selectedId);

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${ejercicios.length} ${ejercicios.length === 1 ? 'ejercicio' : 'ejercicios'}`}
        title="Ejercicios"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nuevo ejercicio
          </Button>
        }
      />

      <Panel className="p-4 sm:p-5">
        <div className="mb-5 flex min-h-9 flex-wrap items-center gap-2">
          {selectedEjercicio ? (
            <>
              <span className="mr-2 text-sm text-ink-soft">
                Seleccionado: <span className="font-semibold text-ink">{selectedEjercicio.nombre}</span>
              </span>
              <Button variant="secondary" size="sm" onClick={onEdit}>
                <Pencil className="h-3.5 w-3.5" /> Modificar
              </Button>
              <Button variant="secondary" size="sm" onClick={() => onToggleEstado(selectedEjercicio)}>
                <Power className="h-3.5 w-3.5" /> {selectedEjercicio.activo ? 'Dar de baja' : 'Activar'}
              </Button>
              <Button variant="danger" size="sm" onClick={onDelete}>
                <Trash2 className="h-3.5 w-3.5" /> Eliminar
              </Button>
            </>
          ) : (
            <p className="text-sm text-ink-muted">Seleccioná un ejercicio para modificarlo, cambiar su estado o eliminarlo.</p>
          )}
        </div>

        <Table columns={COLUMNAS} cargando={cargando}>
          {ejercicios.map((ej) => {
            const seleccionada = selectedId === ej.id;
            return (
              <Row
                key={ej.id}
                selected={seleccionada}
                onActivate={() => setSelectedId(seleccionada ? null : ej.id)}
              >
                <td className="px-3"><SelectDot selected={seleccionada} /></td>
                <td className="px-3 font-semibold text-ink">{ej.nombre}</td>
                <td className="px-3 text-ink-soft">{ej.grupoMuscular}</td>
                <td className="px-3 text-ink-muted truncate max-w-xs">{ej.descripcion || '-'}</td>
                <td className="px-3 text-center">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                    ej.activo ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                  }`}>
                    {ej.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
              </Row>
            );
          })}
          {ejercicios.length === 0 && (
            <EmptyRow colSpan={COLUMNAS.length}>{cargando ? 'Cargando ejercicios...' : 'No hay ejercicios registrados.'}</EmptyRow>
          )}
        </Table>
      </Panel>
    </div>
  );
}