import { Pencil, Plus, Trash2 } from 'lucide-react';

import { formatearMonto } from '../../shared/services/texto';
import { Button, EmptyRow, PageHeader, Panel, Row, SelectDot, Table } from '../../shared/components/ui';

const COLUMNAS = [
  { label: '', className: 'w-12' },
  { label: 'Nombre' },
  { label: 'Días por semana' },
  { label: 'Precio (ARS)', className: 'text-right' },
];

export default function MembresiasList({ membresias, cargando, selectedId, setSelectedId, onAdd, onEdit, onDelete }) {
  const selectedMembresia = membresias.find(m => m.id === selectedId);

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${membresias.length} ${membresias.length === 1 ? 'plan' : 'planes'}`}
        title="Membresías"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nueva membresía
          </Button>
        }
      />

      <Panel className="p-4 sm:p-5">
        <div className="mb-5 flex min-h-9 flex-wrap items-center gap-2">
          {selectedMembresia ? (
            <>
              <span className="mr-2 text-sm text-ink-soft">
                Seleccionada: <span className="font-semibold text-ink">{selectedMembresia.nombre}</span>
              </span>
              <Button variant="secondary" size="sm" onClick={onEdit}>
                <Pencil className="h-3.5 w-3.5" /> Modificar
              </Button>
              <Button variant="danger" size="sm" onClick={onDelete}>
                <Trash2 className="h-3.5 w-3.5" /> Eliminar
              </Button>
            </>
          ) : (
            <p className="text-sm text-ink-muted">Seleccioná una membresía para modificarla o eliminarla.</p>
          )}
        </div>

        <Table columns={COLUMNAS} cargando={cargando}>
          {membresias.map((membresia) => {
            const seleccionada = selectedId === membresia.id;
            return (
              <Row
                key={membresia.id}
                selected={seleccionada}
                onActivate={() => setSelectedId(seleccionada ? null : membresia.id)}
              >
                <td className="px-3"><SelectDot selected={seleccionada} /></td>
                <td className="px-3 font-semibold text-ink">{membresia.nombre}</td>
                <td className="px-3">
                  <span className="tabular-nums">{membresia.dias}</span>
                  <span className="text-ink-soft"> {membresia.dias === '1' ? 'día' : 'días'}</span>
                </td>
                <td className="px-3 text-right tabular-nums">{formatearMonto(membresia.precio)}</td>
              </Row>
            );
          })}
          {membresias.length === 0 && (
            <EmptyRow colSpan={COLUMNAS.length}>{cargando ? 'Cargando membresías...' : 'No hay membresías registradas.'}</EmptyRow>
          )}
        </Table>
      </Panel>
    </div>
  );
}
