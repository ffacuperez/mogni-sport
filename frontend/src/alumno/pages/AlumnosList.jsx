import { AlertTriangle, ArrowDown, ArrowUp, ArrowUpDown, CircleCheck, Clock, History, OctagonAlert, Plus, RefreshCw, TrendingUp, UserRound, Wallet } from 'lucide-react';

import TarjetasAlumnos, { BarraAcciones, SelectorOrden } from '../components/TarjetasAlumnos';
import { COLUMNAS_ACTIVOS, COLUMNAS_INACTIVOS, filtrarAlumnos, ordenarAlumnos, siguienteOrden } from '../services/listaAlumnos';
import { avisoVencimiento, formatearFecha, formatearMonto, motivoSinRenovacion, tonoEstadoCuota } from '../../cuota/services/cuotasService';
import { tituloPlan } from '../../progreso/services/progresoService';
import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Button, EmptyRow, PageHeader, Panel, Row, SearchInput, SelectDot, StatFiltro, Table } from '../../shared/components/ui';
import { usePantallaChica } from '../../shared/hooks/usePantallaChica';

const RECUADROS = [
  { situacion: 'EN_MORA', label: 'En mora', tone: 'error', icon: OctagonAlert },
  { situacion: 'VENCIDA', label: 'Vencidas', tone: 'alert', icon: AlertTriangle },
  { situacion: 'PROXIMA_A_VENCER', label: 'Próximas a vencer', labelCorto: 'Próximas', tone: 'warning', icon: Clock },
  { situacion: 'AL_DIA', label: 'Al día', tone: 'success', icon: CircleCheck },
];

const ARIA_SORT = { asc: 'ascending', desc: 'descending' };

function EncabezadoOrdenable({ label, direccion, onOrdenar }) {
  const Flecha = { asc: ArrowUp, desc: ArrowDown }[direccion] ?? ArrowUpDown;
  return (
    <button
      type="button"
      onClick={onOrdenar}
      title={`Ordenar por ${label}`}
      className={cx('inline-flex cursor-pointer items-center justify-center gap-1 rounded font-semibold', direccion && 'text-ink', FOCUS)}
    >
      {label}
      <Flecha aria-hidden="true" className={cx('h-3.5 w-3.5 shrink-0', direccion ? 'text-primary' : 'text-ink-muted')} />
    </button>
  );
}

function PestanaAlumnos({ id, activa, onElegir, children }) {
  return (
    <button
      type="button"
      role="tab"
      id={`tab-alumnos-${id}`}
      aria-selected={activa}
      aria-controls="panel-alumnos"
      onClick={onElegir}
      className={cx(
        '-mb-px flex-1 cursor-pointer border-b-2 px-5 py-3 text-sm font-semibold transition-colors xl:flex-none',
        activa ? 'border-primary text-primary' : 'border-transparent text-ink-soft hover:text-ink',
        FOCUS,
      )}
    >
      {children}
    </button>
  );
}

function CeldasActivo({ alumno }) {
  const { planActivo: plan, cuota } = alumno;
  const aviso = cuota && avisoVencimiento(cuota);
  return (
    <>
      <td className={cx('px-3 py-2 text-center whitespace-normal', !plan && 'text-ink-soft')}>{plan ? tituloPlan(plan) : 'Sin plan'}</td>
      <td className={cx('px-3 text-center tabular-nums', !plan && 'text-ink-soft')}>{plan ? `${plan.semanaActual} de ${plan.cantidadSemanas}` : '—'}</td>
      <td className="px-3 py-2 text-center whitespace-normal">
        {alumno.membresia}
        {alumno.membresiaProgramada && (
          <span className="mt-1 block text-xs text-ink-soft">
            → {alumno.membresiaProgramada} desde {formatearFecha(alumno.programadaDesde)}
          </span>
        )}
      </td>
      <td className="px-3 text-center tabular-nums">{cuota?.precio != null ? formatearMonto(cuota.precio) : '—'}</td>
      <td className="px-3 text-center tabular-nums">{formatearFecha(cuota?.vencimiento)}</td>
      <td className="px-3 py-2 text-center">
        <Badge tone={tonoEstadoCuota(cuota?.estado)}>{cuota?.estado ?? 'Sin cuotas'}</Badge>
        {aviso && <span className="mt-1 block text-xs text-error">{aviso}</span>}
      </td>
    </>
  );
}

function CeldasInactivo({ alumno }) {
  return (
    <>
      <td className="px-3 py-2 text-center whitespace-normal">{alumno.inscripcion?.membresia ?? '—'}</td>
      <td className="px-3 text-center tabular-nums">{formatearFecha(alumno.fechaBaja)}</td>
      <td className="px-3 text-center tabular-nums">{formatearFecha(alumno.vencimientoUltimaCuota)}</td>
      <td className="px-3 text-center">
        <Badge tone={alumno.reactivacionRequierePago ? 'alert' : 'success'}>{alumno.reactivacionRequierePago ? 'Con pago' : 'Sin pago'}</Badge>
      </td>
    </>
  );
}

/**
 * Gestión de alumnos: tocar una fila la selecciona y las acciones van en la botonera. El orden normal lo define el
 * backend; pestaña, búsqueda, filtro, orden y selección viven en MainApp para conservarse al volver de la ficha.
 */
export default function AlumnosList({
  alumnos,
  cargando,
  esPropietario,
  puedeVerPlanes,
  onAdd,
  onReactivate,
  onRegistrarPago,
  onVerAlumno,
  onVerCuotas,
  onVerProgreso,
  search,
  setSearch,
  pestana,
  onCambiarPestana,
  filtro,
  setFiltro,
  orden,
  setOrden,
  selectedId,
  setSelectedId,
}) {
  const pantallaChica = usePantallaChica();
  const esActivos = pestana === 'activos';
  const activos = alumnos.filter((a) => a.estado === 'Activo');
  const cantidadInactivos = alumnos.length - activos.length;

  // Las tarjetas de Inactivos no tienen selector: ahí queda el orden normal, aunque se haya elegido otro en la tabla
  const ordenEfectivo = pantallaChica && !esActivos ? null : orden;
  const filas = ordenarAlumnos(filtrarAlumnos(alumnos, { pestana, busqueda: search, filtro }), ordenEfectivo);
  // Si queda fuera de la búsqueda o del filtro, deja de contar como seleccionado
  const seleccion = filas.find((a) => a.id === selectedId);

  const definicion = esActivos ? COLUMNAS_ACTIVOS : COLUMNAS_INACTIVOS;
  const columnas = [
    { key: 'seleccion', label: '', className: 'w-12' },
    ...definicion.map(({ clave, label }) => {
      const direccion = orden?.columna === clave ? orden.direccion : undefined;
      return {
        key: clave,
        className: 'text-center',
        ariaSort: ARIA_SORT[direccion] ?? 'none',
        label: <EncabezadoOrdenable label={label} direccion={direccion} onOrdenar={() => setOrden(siguienteOrden(orden, clave))} />,
      };
    }),
  ];

  const motivo = seleccion && esActivos && esPropietario && !seleccion.cuota?.puedeRenovar ? motivoSinRenovacion(seleccion.cuota) : '';
  let ayuda = motivo;
  if (!seleccion) {
    if (!esActivos) ayuda = 'Seleccioná un alumno para reactivarlo o ver su información.';
    else if (esPropietario) ayuda = 'Seleccioná un alumno para registrar el pago o ver su información.';
    else ayuda = puedeVerPlanes ? 'Seleccioná un alumno para ver sus datos o su progreso.' : 'Seleccioná un alumno para ver sus datos.';
  }

  // La misma botonera para la tabla y para la barra de las tarjetas
  const acciones = [
    ...(esActivos && esPropietario ? [{ texto: 'Registrar pago', icono: Wallet, variante: 'primary', deshabilitado: !seleccion?.cuota?.puedeRenovar, onClick: () => onRegistrarPago(seleccion) }] : []),
    ...(!esActivos ? [{ texto: 'Reactivar', icono: RefreshCw, variante: 'success', deshabilitado: !seleccion, onClick: () => onReactivate(seleccion) }] : []),
    { texto: 'Ver alumno', icono: UserRound, variante: 'secondary', deshabilitado: !seleccion, onClick: () => onVerAlumno(seleccion) },
    ...(esPropietario ? [{ texto: 'Ver cuotas', icono: History, variante: 'secondary', deshabilitado: !seleccion, onClick: () => onVerCuotas(seleccion) }] : []),
    ...(puedeVerPlanes ? [{ texto: 'Ver progreso', icono: TrendingUp, variante: 'secondary', deshabilitado: !seleccion, onClick: () => onVerProgreso(seleccion) }] : []),
  ];

  let vacio = 'No hay alumnos que coincidan con la búsqueda o el filtro.';
  if (cargando) vacio = 'Cargando alumnos...';
  else if ((esActivos ? activos.length : cantidadInactivos) === 0) vacio = `No hay alumnos ${pestana} para mostrar.`;

  return (
    <div className="animate-fade-in">
      <PageHeader
        title="Alumnos"
        actions={
          <Button variant="primary" onClick={onAdd}>
            <Plus className="h-4 w-4" /> Agregar nuevo alumno
          </Button>
        }
      />

      <div className="flex border-b border-line" role="tablist" aria-label="Estado de alumnos">
        <PestanaAlumnos id="activos" activa={esActivos} onElegir={() => onCambiarPestana('activos')}>Activos ({activos.length})</PestanaAlumnos>
        <PestanaAlumnos id="inactivos" activa={!esActivos} onElegir={() => onCambiarPestana('inactivos')}>Inactivos ({cantidadInactivos})</PestanaAlumnos>
      </div>

      <div className="mt-5" id="panel-alumnos" role="tabpanel" aria-labelledby={`tab-alumnos-${pestana}`}>
        {esActivos && (
          <div className={cx('grid grid-cols-4', pantallaChica ? 'mb-3 gap-1.5' : 'mb-5 gap-3')} role="group" aria-label="Filtrar por situación de pago">
            {RECUADROS.map(({ situacion, label, labelCorto, tone, icon }) => (
              <StatFiltro
                key={situacion}
                label={pantallaChica ? (labelCorto ?? label) : label}
                compacto={pantallaChica}
                value={activos.filter((a) => a.cuota?.situacion === situacion).length}
                tone={tone}
                icon={icon}
                activo={filtro === situacion}
                onClick={() => setFiltro(filtro === situacion ? null : situacion)}
              />
            ))}
          </div>
        )}

        {pantallaChica ? (
          <>
            <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center">
              <SearchInput
                value={search}
                onChange={setSearch}
                placeholder="Buscar por nombre o DNI"
                label="Buscar alumno por nombre, apellido o DNI"
              />
              {esActivos && <SelectorOrden orden={orden} setOrden={setOrden} />}
            </div>
            <div className="relative">
              {cargando && <div className="loading-bar absolute inset-x-0 -top-1.5 h-0.5" role="progressbar" aria-label="Cargando" />}
              <TarjetasAlumnos alumnos={filas} esActivos={esActivos} selectedId={selectedId} setSelectedId={setSelectedId} vacio={vacio} />
            </div>
            {seleccion && (
              <>
                {/* Espacio del alto de la barra, para que no tape la última tarjeta */}
                <div aria-hidden="true" className="h-[calc(9rem+env(safe-area-inset-bottom))]" />
                <BarraAcciones alumno={seleccion} acciones={acciones} motivo={motivo} />
              </>
            )}
          </>
        ) : (
          <Panel className="p-5">
            <div className="mb-3 flex items-start justify-between gap-3">
              <div className="flex flex-wrap gap-2">
                {acciones.map(({ texto, icono: Icono, variante, deshabilitado, onClick }) => (
                  <Button key={texto} variant={variante} onClick={onClick} disabled={deshabilitado}>
                    <Icono className="h-4 w-4" /> {texto}
                  </Button>
                ))}
              </div>
              <SearchInput
                value={search}
                onChange={setSearch}
                placeholder="Buscar por nombre o DNI"
                label="Buscar alumno por nombre, apellido o DNI"
                className="sm:w-64 shrink-0"
              />
            </div>
            <p className="mb-4 min-h-5 text-sm text-ink-soft">{ayuda}</p>

            <Table columns={columnas} cargando={cargando}>
              {filas.map((alumno) => {
                const seleccionada = alumno.id === selectedId;
                return (
                  <Row key={alumno.id} selected={seleccionada} onActivate={() => setSelectedId(seleccionada ? null : alumno.id)}>
                    <td className="px-3"><SelectDot selected={seleccionada} /></td>
                    <td className="min-w-36 px-3 py-2 text-center font-semibold whitespace-normal text-ink">{alumno.apellido}, {alumno.nombre}</td>
                    {esActivos ? <CeldasActivo alumno={alumno} /> : <CeldasInactivo alumno={alumno} />}
                  </Row>
                );
              })}
              {filas.length === 0 && <EmptyRow colSpan={columnas.length}>{vacio}</EmptyRow>}
            </Table>
          </Panel>
        )}
      </div>
    </div>
  );
}
