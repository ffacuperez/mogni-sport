import { Plus } from 'lucide-react';

import { describirMembresia } from '../services/alumnosService';
import { normalizar } from '../../shared/services/texto';
import { Badge, Button, EmptyRow, PageHeader, Panel, Row, SearchInput, Table } from '../../shared/components/ui';

const COLUMNAS = [
  { label: 'DNI' },
  { label: 'Apellido' },
  { label: 'Nombre' },
  { label: 'Correo electrónico' },
  { label: 'Membresía' },
  { label: 'Estado' },
];

export default function AlumnosList({ alumnos, cargando, onSelect, onAdd, search, setSearch, mostrarInactivos, onToggleMostrarInactivos }) {
  // HU-74: búsqueda parcial por nombre, apellido o DNI
  const termino = normalizar(search.trim());
  const coincide = (a) => !termino
    || normalizar(a.nombre).includes(termino)
    || normalizar(a.apellido).includes(termino)
    || normalizar(`${a.nombre} ${a.apellido}`).includes(termino)
    || a.dni.includes(termino);

  // HU-246: por defecto solo activos; un alumno reactivado vuelve a verse sin activar el filtro
  const filteredAlumnos = alumnos
    .filter(a => mostrarInactivos || a.estado === 'Activo')
    .filter(coincide);

  let vacio = 'No hay alumnos para mostrar.';
  if (cargando) vacio = 'Cargando alumnos...';
  else if (search.trim()) vacio = 'No se encontraron alumnos con ese nombre, apellido o DNI.';

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${filteredAlumnos.length} ${filteredAlumnos.length === 1 ? 'registro' : 'registros'}`}
        title="Alumnos"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nuevo alumno
          </Button>
        }
      />

      <Panel className="p-4 sm:p-5">
        <div className="mb-5 flex flex-col-reverse gap-4 sm:flex-row sm:items-center sm:justify-between">
          <label className="flex cursor-pointer select-none items-center gap-2 text-sm text-ink-soft hover:text-ink">
            <input
              type="checkbox"
              checked={mostrarInactivos}
              onChange={onToggleMostrarInactivos}
              className="h-4 w-4 cursor-pointer accent-primary"
            />
            Mostrar inactivos
          </label>

          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Buscar por nombre, apellido o DNI"
            label="Buscar alumno por nombre, apellido o DNI"
          />
        </div>

        <Table columns={COLUMNAS} cargando={cargando}>
          {filteredAlumnos.map((alumno) => (
            <Row key={alumno.id} onActivate={() => onSelect(alumno.id)} title="Ver detalle del alumno">
              <td className="px-3 tabular-nums text-ink-soft">{alumno.dni}</td>
              <td className="px-3 font-semibold text-ink">{alumno.apellido}</td>
              <td className="px-3">{alumno.nombre}</td>
              <td className="px-3 text-ink-soft">{alumno.email}</td>
              <td className="px-3">{describirMembresia(alumno)}</td>
              <td className="px-3">
                <Badge tone={alumno.estado === 'Activo' ? 'success' : 'error'}>{alumno.estado}</Badge>
              </td>
            </Row>
          ))}
          {filteredAlumnos.length === 0 && <EmptyRow colSpan={COLUMNAS.length}>{vacio}</EmptyRow>}
        </Table>
      </Panel>
    </div>
  );
}
