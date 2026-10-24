import { AlertTriangle, CircleCheck, Clock, History, OctagonAlert, UserRound, Wallet } from 'lucide-react';

import { avisoVencimiento, formatearFecha, formatearMonto, SITUACIONES, tonoEstadoCuota } from '../services/cuotasService';
import { normalizar } from '../../shared/services/texto';
import { Badge, Button, EmptyRow, PageHeader, Panel, Row, SearchInput, SelectDot, Stat, Table } from '../../shared/components/ui';

// Un filtro por cada situación que se muestra en la tabla (HU-46)
const FILTROS = ['Todas', SITUACIONES.EN_MORA, SITUACIONES.VENCIDA, SITUACIONES.PROXIMA_A_VENCER, SITUACIONES.AL_DIA];

const COLUMNAS = [
  { label: '', className: 'w-12' },
  { label: 'Alumno', className: 'text-center' },
  { label: 'DNI', className: 'text-center' },
  { label: 'Membresía', className: 'text-center' },
  { label: 'Vencimiento', className: 'text-center' },
  { label: 'Precio', className: 'text-center' },
  { label: 'Estado', className: 'text-center' },
];


/** Texto que explica por qué no se puede registrar el pago de la fila seleccionada. */
function motivoSinRenovacion(fila) {
  if (fila.renovableDesde) return `El pago se habilita desde el ${formatearFecha(fila.renovableDesde)}.`;
  return 'El alumno no tiene una membresía activa.';
}

export default function CuotasList({
  cuotas,
  cargando,
  selectedId,
  setSelectedId,
  onRegistrarPago,
  onVerHistorial,
  onVerAlumno,
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

  // Una fila por alumno activo; el orden (en mora, vencidas, por vencimiento) ya viene del backend
  const filas = cuotas
    .filter(coincide)
    .filter((cuota) => filtro === 'Todas' || cuota.estado === filtro);

  // La selección es por alumno: después de renovar, la fila sigue siendo la misma
  const seleccion = filas.find((c) => c.alumnoId === selectedId);

  const cantidad = (estado) => cuotas.filter((c) => c.estado === estado).length;
  const contador = (f) => (f === 'Todas' ? cuotas.length : cantidad(f));

  let vacio = 'No hay alumnos que coincidan con la búsqueda.';
  if (cargando) vacio = 'Cargando cuotas...';
  else if (cuotas.length === 0) vacio = 'No hay alumnos activos.';

  return (
    <div className="animate-fade-in">
      <PageHeader eyebrow="Cobranzas · Propietario" title="Cuotas" subtitle="Situación de pago de cada alumno activo según su última cuota." />

      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="En mora" value={cantidad(SITUACIONES.EN_MORA)} tone={cantidad(SITUACIONES.EN_MORA) ? 'error' : 'neutral'} icon={OctagonAlert} />
        <Stat label="Vencidas" value={cantidad(SITUACIONES.VENCIDA)} tone={cantidad(SITUACIONES.VENCIDA) ? 'alert' : 'neutral'} icon={AlertTriangle} />
        <Stat label="Próximas a vencer" value={cantidad(SITUACIONES.PROXIMA_A_VENCER)} tone={cantidad(SITUACIONES.PROXIMA_A_VENCER) ? 'warning' : 'neutral'} icon={Clock} />
        <Stat label="Al día" value={cantidad(SITUACIONES.AL_DIA)} tone="success" icon={CircleCheck} />
      </div>

      <Panel className="p-4 sm:p-5">
        <div className="mb-4 flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="primary" onClick={() => onRegistrarPago(seleccion)} disabled={!seleccion?.puedeRenovar}>
              <Wallet className="h-4 w-4" /> Registrar pago
            </Button>
            <Button onClick={() => onVerHistorial(seleccion)} disabled={!seleccion}>
              <History className="h-4 w-4" /> Ver historial
            </Button>
            <Button onClick={() => onVerAlumno(seleccion)} disabled={!seleccion}>
              <UserRound className="h-4 w-4" /> Ver alumno
            </Button>
            <p className="text-sm text-ink-muted">
              {!seleccion && 'Seleccioná un alumno para registrar el pago, ver el historial o ir a su ficha.'}
              {seleccion && !seleccion.puedeRenovar && motivoSinRenovacion(seleccion)}
            </p>
          </div>

          <SearchInput value={searchTerm} onChange={setSearchTerm} placeholder="Buscar por nombre o DNI" label="Buscar alumno por nombre o DNI" />
        </div>

        <div className="mb-5 flex flex-wrap gap-2" role="group" aria-label="Filtrar por situación">
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
              {f} <span className="tabular-nums opacity-80">({contador(f)})</span>
            </button>
          ))}
        </div>

        <Table columns={COLUMNAS} cargando={cargando}>
          {filas.map((cuota) => {
            const seleccionada = selectedId === cuota.alumnoId;
            const aviso = avisoVencimiento(cuota);
            return (
              <Row key={cuota.alumnoId} selected={seleccionada} onActivate={() => setSelectedId(seleccionada ? null : cuota.alumnoId)}>
                <td className="px-3"><SelectDot selected={seleccionada} /></td>
                <td className="px-3 text-center font-semibold text-ink">{cuota.apellido}, {cuota.nombre}</td>
                <td className="px-3 text-center tabular-nums text-ink-soft">{cuota.dni}</td>
                <td className="px-3 text-center">{cuota.membresia ?? '—'}</td>
                <td className="px-3 text-center tabular-nums">{formatearFecha(cuota.vencimiento)}</td>
                <td className="px-3 text-center tabular-nums">{cuota.precio != null ? formatearMonto(cuota.precio) : '—'}</td>
                <td className="px-3 py-2 text-center">
                  <Badge tone={tonoEstadoCuota(cuota.estado)}>{cuota.estado}</Badge>
                  {aviso && <span className="mt-1 block text-xs text-error">{aviso}</span>}
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
