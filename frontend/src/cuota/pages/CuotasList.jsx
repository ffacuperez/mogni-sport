import { AlertTriangle, Clock, History, Receipt, Wallet } from 'lucide-react';

import { avisoVencimiento, formatearFecha, formatearMonto, tonoEstadoCuota } from '../services/cuotasService';
import { normalizar } from '../../shared/services/texto';
import { Badge, Button, EmptyRow, PageHeader, Panel, Row, SearchInput, SelectDot, Stat, Table } from '../../shared/components/ui';

// Un filtro por cada estado que se muestra en la tabla
const FILTROS = ['Todas', 'Al día', 'Próxima a vencer', 'Vencida'];

const COLUMNAS = [
  { label: '', className: 'w-12' },
  { label: 'Alumno' },
  { label: 'DNI' },
  { label: 'Membresía' },
  { label: 'Monto', className: 'text-right' },
  { label: 'Vencimiento' },
  { label: 'Estado' },
];

export default function CuotasList({
  cuotas,
  cargando,
  selectedId,
  setSelectedId,
  onRegistrarPago,
  onVerHistorial,
  searchTerm,
  setSearchTerm,
  filtro,
  setFiltro,
}) {
  // Misma búsqueda que en Alumnos: sin tildes ni mayúsculas, también por "nombre apellido"
  const termino = normalizar(searchTerm.trim());
  const coincide = (c) => !termino
    || c.dni.includes(termino)
    || normalizar(c.nombre).includes(termino)
    || normalizar(c.apellido).includes(termino)
    || normalizar(`${c.nombre} ${c.apellido}`).includes(termino);

  // El backend ya devuelve solo cuotas de alumnos activos con inscripción activa.
  const filas = cuotas
    .filter(coincide)
    .filter((cuota) => filtro === 'Todas' || cuota.estado === filtro);

  const seleccion = filas.find((c) => c.id === selectedId);

  const porCobrar = cuotas.filter((c) => c.pagable);
  const cantidad = (estado) => cuotas.filter((c) => c.estado === estado).length;
  const totalPorCobrar = porCobrar.reduce((total, c) => total + c.monto, 0);

  let vacio = 'No hay cuotas que coincidan con la búsqueda.';
  if (cargando) vacio = 'Cargando cuotas...';
  else if (cuotas.length === 0) vacio = 'No hay cuotas registradas.';

  return (
    <div className="animate-fade-in">
      <PageHeader eyebrow="Cobranzas · Propietario" title="Cuotas" />

      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Por cobrar" value={porCobrar.length} icon={Receipt} />
        <Stat label="Vencidas" value={cantidad('Vencida')} tone={cantidad('Vencida') ? 'error' : 'neutral'} icon={AlertTriangle} />
        <Stat label="Próximas a vencer" value={cantidad('Próxima a vencer')} tone={cantidad('Próxima a vencer') ? 'warning' : 'neutral'} icon={Clock} />
        <Stat label="Monto pendiente" value={formatearMonto(totalPorCobrar)} tone="primary" icon={Wallet} />
      </div>

      <Panel className="p-4 sm:p-5">
        <div className="mb-4 flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="primary" onClick={() => onRegistrarPago(seleccion)} disabled={!seleccion?.pagable}>
              <Wallet className="h-4 w-4" /> Registrar pago
            </Button>
            <Button onClick={() => onVerHistorial(seleccion)} disabled={!seleccion}>
              <History className="h-4 w-4" /> Ver historial
            </Button>
            <p className="text-sm text-ink-muted">
              {!seleccion && 'Seleccioná una cuota para registrar el pago o ver el historial.'}
              {seleccion && !seleccion.pagable && 'La cuota seleccionada ya está pagada.'}
            </p>
          </div>

          <SearchInput value={searchTerm} onChange={setSearchTerm} placeholder="Buscar por nombre o DNI" label="Buscar cuota por nombre o DNI" />
        </div>

        <div className="mb-5 flex flex-wrap gap-2" role="group" aria-label="Filtrar por estado">
          {FILTROS.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFiltro(f)}
              aria-pressed={filtro === f}
              className={`cursor-pointer rounded-full border px-3 py-1.5 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
                filtro === f
                  ? 'border-primary bg-primary text-on-dark'
                  : 'border-line bg-card text-ink-soft hover:bg-row-hover hover:text-ink'
              }`}
            >
              {f}
            </button>
          ))}
        </div>

        <Table columns={COLUMNAS} cargando={cargando}>
          {filas.map((cuota) => {
            const seleccionada = selectedId === cuota.id;
            return (
              <Row key={cuota.id} selected={seleccionada} onActivate={() => setSelectedId(seleccionada ? null : cuota.id)}>
                <td className="px-3"><SelectDot selected={seleccionada} /></td>
                <td className="px-3 font-semibold text-ink">{cuota.apellido}, {cuota.nombre}</td>
                <td className="px-3 tabular-nums text-ink-soft">{cuota.dni}</td>
                <td className="px-3">{cuota.membresia}</td>
                <td className="px-3 text-right tabular-nums">{formatearMonto(cuota.monto)}</td>
                <td className="px-3">
                  <span className="tabular-nums">{formatearFecha(cuota.vencimiento)}</span>
                  {avisoVencimiento(cuota) && (
                    <span className={`block text-xs ${{ 'Vencida': 'text-error', 'Próxima a vencer': 'text-warning-text' }[cuota.estado] ?? 'text-success'}`}>
                      {avisoVencimiento(cuota)}
                    </span>
                  )}
                </td>
                <td className="px-3">
                  <Badge tone={tonoEstadoCuota(cuota.estado)}>{cuota.estado}</Badge>
                </td>
              </Row>
            );
          })}
          {filas.length === 0 && <EmptyRow colSpan={COLUMNAS.length}>{vacio}</EmptyRow>}
        </Table>
      </Panel>
    </div>
  );
}
