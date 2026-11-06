import { Plus } from 'lucide-react';
import { useState } from 'react';

import { describirMembresia } from '../services/alumnosService';
import { normalizar } from '../../shared/services/texto';
import { Badge, Button, EmptyRow, PageHeader, Panel, Row, SearchInput, Table } from '../../shared/components/ui';

const COLUMNAS_ACTIVOS = [
  { label: 'DNI' },
  { label: 'Apellido' },
  { label: 'Nombre' },
  { label: 'Correo electrónico' },
  { label: 'Membresía' },
  { label: 'Estado' },
];

const COLUMNAS_INACTIVOS = [
  ...COLUMNAS_ACTIVOS.slice(0, -1),
  { label: 'Acción' },
];

/**
 * MOGNI-74: del más reciente al más antiguo según el inicio de su inscripción (la activa o, en los inactivos, la
 * última). A igual fecha, el alta más nueva primero; los que nunca tuvieron inscripción, al final.
 */
const porInscripcionMasReciente = (a, b) => (b.inscripcion?.fechaInicio ?? '').localeCompare(a.inscripcion?.fechaInicio ?? '')
  || b.id - a.id;

export default function AlumnosList({ alumnos: sinOrdenar, cargando, onSelect, onAdd, onReactivate, search, setSearch }) {
  // Se ordena acá y no en el backend: los alumnos que se dan de alta se agregan al final de la lista en memoria
  const alumnos = [...sinOrdenar].sort(porInscripcionMasReciente);
  const [pestana, setPestana] = useState('activos');

  // HU-74: búsqueda parcial por nombre, apellido o DNI
  const termino = normalizar(search.trim());
  const coincide = (a) => !termino
    || normalizar(a.nombre).includes(termino)
    || normalizar(a.apellido).includes(termino)
    || normalizar(`${a.nombre} ${a.apellido}`).includes(termino)
    || a.dni.includes(termino);

  const alumnosActivos = alumnos.filter(a => a.estado === 'Activo' && coincide(a));
  const alumnosInactivos = alumnos.filter(a => a.estado !== 'Activo' && coincide(a));
  const cantidadVisible = alumnosActivos.length + alumnosInactivos.length;
  const alumnosMostrados = pestana === 'activos' ? alumnosActivos : alumnosInactivos;
  const columnasMostradas = pestana === 'activos' ? COLUMNAS_ACTIVOS : COLUMNAS_INACTIVOS;
  const estadoMostrado = pestana === 'activos' ? 'activos' : 'inactivos';

  const mensajeVacio = (estado) => {
    if (cargando) return 'Cargando alumnos...';
    if (search.trim()) return `No se encontraron alumnos ${estado} con ese nombre, apellido o DNI.`;
    return `No hay alumnos ${estado} para mostrar.`;
  };

  const filasComunes = (alumno) => (
    <>
      <td className="px-3 tabular-nums text-ink-soft">{alumno.dni}</td>
      <td className="px-3 font-semibold text-ink">{alumno.apellido}</td>
      <td className="px-3">{alumno.nombre}</td>
      <td className="px-3 text-ink-soft">{alumno.email}</td>
      <td className="px-3">{describirMembresia(alumno)}</td>
    </>
  );

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow={`Gestión · ${cantidadVisible} ${cantidadVisible === 1 ? 'registro' : 'registros'}`}
        title="Alumnos"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nuevo alumno
          </Button>
        }
      />

      <div className="mb-5">
        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nombre, apellido o DNI"
          label="Buscar alumno por nombre, apellido o DNI"
        />
      </div>

      <div className="mb-0 flex border-b border-line" role="tablist" aria-label="Estado de alumnos">
        <button
          type="button"
          role="tab"
          id="tab-alumnos-activos"
          aria-selected={pestana === 'activos'}
          aria-controls="panel-alumnos"
          onClick={() => setPestana('activos')}
          className={`-mb-px cursor-pointer border-b-2 px-5 py-3 text-sm font-semibold transition-colors ${
            pestana === 'activos' ? 'border-primary text-primary' : 'border-transparent text-ink-soft hover:text-ink'
          }`}
        >
          Activos ({alumnos.filter(a => a.estado === 'Activo').length})
        </button>
        <button
          type="button"
          role="tab"
          id="tab-alumnos-inactivos"
          aria-selected={pestana === 'inactivos'}
          aria-controls="panel-alumnos"
          onClick={() => setPestana('inactivos')}
          className={`-mb-px cursor-pointer border-b-2 px-5 py-3 text-sm font-semibold transition-colors ${
            pestana === 'inactivos' ? 'border-primary text-primary' : 'border-transparent text-ink-soft hover:text-ink'
          }`}
        >
          Inactivos ({alumnos.filter(a => a.estado !== 'Activo').length})
        </button>
      </div>

      <Panel className="mt-6 p-4 sm:p-5" id="panel-alumnos" role="tabpanel" aria-labelledby={`tab-alumnos-${pestana}`}>
        <Table columns={columnasMostradas} cargando={cargando}>
          {alumnosMostrados.map((alumno) => (
            <Row key={alumno.id} onActivate={() => onSelect(alumno.id)} title="Ver detalle del alumno">
              {filasComunes(alumno)}
              {pestana === 'activos' ? (
                <td className="px-3">
                  <Badge tone="success">{alumno.estado}</Badge>
                </td>
              ) : (
                <td className="px-3">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={(event) => {
                      event.stopPropagation();
                      onReactivate(alumno);
                    }}
                    onKeyDown={(event) => event.stopPropagation()}
                  >
                    Reactivar
                  </Button>
                </td>
              )}
            </Row>
          ))}
          {alumnosMostrados.length === 0 && (
            <EmptyRow colSpan={columnasMostradas.length}>{mensajeVacio(estadoMostrado)}</EmptyRow>
          )}
        </Table>
      </Panel>
    </div>
  );
}
