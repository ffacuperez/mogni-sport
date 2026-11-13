import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, CircleAlert, Info, Lock } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Badge, Button, Cargando, Field, PageHeader, Panel, SearchInput } from '../../shared/components/ui';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { listarAlumnos } from '../../alumno/services/alumnosService';
import { listarEjercicios } from '../../ejercicio/services/ejerciciosService';
import {
  cantidadEjercicios, cantidadSesiones, listarPlantillas, textoNivel, textoObjetivo, tonoNivel,
} from '../../plantilla/services/plantillasService';
import { DatosPlan, EditorSemanas } from '../../plantilla/components/EditorPlan';
import { VistaSemanas } from '../../plantilla/components/TarjetasSesiones';
import { formularioVacio, ubicacionDelError, validarFormulario } from '../../plantilla/components/formularioPlan';
import {
  asignarDesdeCero, asignarDesdePlantilla, fechaFinPorSemanas, hoyIso, obtenerPlanActivo, sumarDias, textoFecha,
} from '../services/planesService';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;
const iniciales = (alumno) => `${alumno.nombre?.[0] ?? ''}${alumno.apellido?.[0] ?? ''}`.toUpperCase();
const nombreCompleto = (alumno) => `${alumno.nombre} ${alumno.apellido}`;
const normalizar = (texto) => String(texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

const SIN_IDS_INACTIVOS = new Set();

function Avatar({ alumno }) {
  return (
    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-dark">
      {iniciales(alumno)}
    </span>
  );
}

/** Busca entre los alumnos activos por nombre, apellido, DNI o email; cada uno indica si ya tiene plan activo. */
function SelectorAlumno({ alumnos, onElegir }) {
  const [busqueda, setBusqueda] = useState('');
  const resultados = useMemo(() => {
    const texto = normalizar(busqueda.trim());
    const filtrados = texto
      ? alumnos.filter(a => [nombreCompleto(a), a.dni, a.email].some(campo => normalizar(campo).includes(texto)))
      : alumnos;
    // Todos, en orden alfabético; la lista tiene su propio scroll
    return [...filtrados].sort((a, b) => nombreCompleto(a).localeCompare(nombreCompleto(b), 'es'));
  }, [alumnos, busqueda]);

  return (
    <div className="p-4">
      <SearchInput value={busqueda} onChange={setBusqueda} placeholder="Buscar alumno por nombre, DNI o email" className="sm:w-full" />
      <ul className="mt-3 max-h-80 divide-y divide-line-soft overflow-y-auto">
        {resultados.length === 0 && (
          <li className="py-6 text-center text-sm text-ink-muted">
            {alumnos.length === 0 ? 'No hay alumnos activos.' : 'Ningún alumno coincide con la búsqueda.'}
          </li>
        )}
        {resultados.map(alumno => (
          <li key={alumno.id}>
            <button type="button" onClick={() => onElegir(alumno)}
              className={cx('flex w-full cursor-pointer items-center gap-3 rounded-md px-2 py-2.5 text-left transition-colors hover:bg-row-hover', FOCUS)}>
              <Avatar alumno={alumno} />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-ink">{nombreCompleto(alumno)}</span>
                <span className="block truncate text-xs text-ink-soft">{alumno.email} · DNI {alumno.dni}</span>
              </span>
              {alumno.planActivo
                ? <Badge tone="error" className="shrink-0 px-2 py-0.5 text-[11px]">Plan activo</Badge>
                : <span className="shrink-0 text-xs text-ink-soft">Sin plan</span>}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Vista previa de la plantilla elegida: datos y sus semanas, con las fechas que tendría el plan. */
function PrevisualizacionPlantilla({ plantilla, inicio }) {
  return (
    <div className="mt-4 rounded-lg border border-line p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="font-semibold text-ink">{plantilla.nombre}</p>
          <div className="mt-1.5 flex flex-wrap gap-1.5">
            <Badge tone={tonoNivel(plantilla.nivel)} className="px-2 py-0.5 text-[11px]">{textoNivel(plantilla.nivel)}</Badge>
            <Badge className="px-2 py-0.5 text-[11px]">{textoObjetivo(plantilla.objetivo)}</Badge>
          </div>
        </div>
        <p className="text-xs tabular-nums text-ink-soft">
          {plural(plantilla.semanas.length, 'semana', 'semanas')} · {plural(cantidadSesiones(plantilla), 'sesión', 'sesiones')} · {plural(cantidadEjercicios(plantilla), 'ejercicio', 'ejercicios')}
        </p>
      </div>
      <div className="mt-4">
        <VistaSemanas key={plantilla.id} semanas={plantilla.semanas} inicio={inicio} />
      </div>
    </div>
  );
}

function Pestania({ activa, onClick, children }) {
  return (
    <button type="button" onClick={onClick} aria-pressed={activa}
      className={cx('cursor-pointer rounded-md px-4 py-2 text-xs font-semibold transition-colors', FOCUS,
        activa ? 'bg-ink text-on-dark' : 'text-ink-soft hover:text-ink')}>
      {children}
    </button>
  );
}

/** HU-266: asignar un plan a un alumno, desde una plantilla o armándolo desde cero. */
export default function AsignarPlan({ plantillaInicialId, alumnoInicialId, onCancelar, onAsignado, onVerPlan, onToast }) {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState(null);
  const [alumno, setAlumno] = useState(null);
  // undefined: todavía no se sabe (verificando); null: no tiene plan activo
  const [planActivo, setPlanActivo] = useState(undefined);
  const [errorPlanActivo, setErrorPlanActivo] = useState(null);
  const [origen, setOrigen] = useState('plantilla');
  const [plantillaId, setPlantillaId] = useState(plantillaInicialId ? String(plantillaInicialId) : '');
  const [form, setForm] = useState(formularioVacio);
  // HU-266: la fecha de inicio es hoy (la de la asignación); el plan dura las semanas que tiene
  const [hoy] = useState(hoyIso);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  // La semana y la sesión del error de validación, para mostrarlas y llevar la pantalla hasta ahí
  const [irA, setIrA] = useState(null);

  useEffect(() => {
    Promise.all([listarAlumnos({ activo: true }), listarPlantillas(), listarEjercicios()])
      .then(([alumnos, plantillas, catalogo]) => {
        setDatos({ alumnos, plantillas, catalogo });
        const inicial = alumnos.find(a => String(a.id) === String(alumnoInicialId));
        if (inicial) setAlumno(inicial);
      })
      .catch((err) => setError(err.message));
  }, [alumnoInicialId]);

  // La validación real es del backend; acá solo se consulta para avisar antes de armar el plan
  useEffect(() => {
    if (!alumno) return undefined;
    let vigente = true;
    obtenerPlanActivo(alumno.id)
      .then((plan) => { if (vigente) setPlanActivo(plan); })
      // Sin poder verificarlo se deja seguir: si ya tiene un plan activo, el backend lo rechaza igual
      .catch((err) => { if (vigente) { setPlanActivo(null); setErrorPlanActivo(err.message); } });
    return () => { vigente = false; };
  }, [alumno]);

  if (error) {
    return (
      <div role="alert" className="mx-auto flex max-w-3xl items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error}</span>
      </div>
    );
  }
  if (!datos) return <Cargando texto="Cargando alumnos y plantillas" />;

  // Al cambiar de alumno se vuelve a verificar su plan activo (lo hace el efecto de arriba)
  const elegirAlumno = (elegido) => {
    setPlanActivo(undefined);
    setErrorPlanActivo(null);
    setAlumno(elegido);
  };

  const plantilla = datos.plantillas.find(p => String(p.id) === plantillaId) ?? null;
  const bloqueado = Boolean(planActivo);
  const verificando = alumno != null && planActivo === undefined;

  const handleAsignar = () => {
    if (!alumno) return onToast('Elegí el alumno al que le vas a asignar el plan.', true);
    if (origen === 'plantilla' && !plantilla) return onToast('Elegí la plantilla que le vas a asignar.', true);
    if (origen === 'cero') {
      const mensaje = validarFormulario(form, 'el plan');
      if (mensaje) {
        const ubicacion = ubicacionDelError(form, 'el plan');
        if (ubicacion) setIrA({ ...ubicacion, vez: Date.now() });
        return onToast(mensaje, true);
      }
    }
    setConfirmando(true);
  };

  const asignar = async () => {
    setConfirmando(false);
    setEnviando(true);
    try {
      const plan = origen === 'plantilla'
        ? await asignarDesdePlantilla(alumno.id, plantilla.id)
        : await asignarDesdeCero(alumno.id, form);
      onToast(`Plan asignado a ${nombreCompleto(alumno)}.`);
      onAsignado(plan);
    } catch (err) {
      onToast(err.message, true);
      setEnviando(false);
      // Si mientras tanto le asignaron otro plan, se muestra el aviso
      if (err.status === 409) obtenerPlanActivo(alumno.id).then(setPlanActivo).catch(() => {});
    }
  };

  const nombrePlan = origen === 'plantilla' ? plantilla?.nombre : form.nombre.trim();
  // La duración es la cantidad de semanas de la plantilla o del plan armado
  const semanas = origen === 'plantilla' ? (plantilla?.semanas.length ?? 0) : form.semanas.length;
  const fin = semanas > 0 ? fechaFinPorSemanas(hoy, semanas) : null;

  return (
    <div className="mx-auto max-w-3xl animate-fade-in">
      <PageHeader title="Asignar plan de entrenamiento" subtitle="Elegí el alumno y el plan que le vas a asignar." />

      <h2 className="mb-2 text-base font-semibold text-ink">Alumno</h2>
      <Panel className="mb-6">
        {alumno ? (
          <div className="flex items-center justify-between gap-3 p-4">
            <div className="flex min-w-0 items-center gap-3">
              <Avatar alumno={alumno} />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink">{nombreCompleto(alumno)}</p>
                <p className="truncate text-xs text-ink-soft">{alumno.email}</p>
              </div>
            </div>
            <button type="button" onClick={() => elegirAlumno(null)}
              className={cx('shrink-0 cursor-pointer rounded-md px-2 py-1 text-xs font-semibold text-primary hover:underline', FOCUS)}>
              Cambiar
            </button>
          </div>
        ) : (
          <SelectorAlumno alumnos={datos.alumnos} onElegir={elegirAlumno} />
        )}
      </Panel>
      {errorPlanActivo && (
        <p className="-mt-4 mb-6 text-xs text-error">No se pudo verificar si tiene un plan activo: {errorPlanActivo}</p>
      )}

      {bloqueado && (
        <div role="alert" className="mb-6 flex gap-3 rounded-lg border border-primary-light bg-error-bg p-4">
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-error" />
          <div className="min-w-0 text-sm">
            <p className="font-semibold text-error">{alumno.nombre} ya tiene un plan activo</p>
            <p className="mt-1 text-error">
              Tiene asignado el plan “{planActivo.nombre}” desde el {textoFecha(planActivo.fechaAsignacion)}
              {planActivo.fechaFin && ` hasta el ${textoFecha(planActivo.fechaFin)}`}.
              Para asignarle uno nuevo, primero finalizá su plan actual o esperá a que venza.
            </p>
            <button type="button" onClick={() => onVerPlan(planActivo.id)}
              className={cx('mt-2 inline-flex cursor-pointer items-center gap-1 rounded-md text-xs font-semibold text-primary hover:underline', FOCUS)}>
              Ir al plan activo de {alumno.nombre} <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Con un plan activo todo el formulario queda deshabilitado (el fieldset deshabilita cada control) */}
      <fieldset disabled={bloqueado} className={cx('min-w-0', bloqueado && 'opacity-50')}>
        <Panel className="p-6">
          <h2 className="mb-4 text-base font-semibold text-ink">Origen del plan</h2>
          <div className="inline-flex rounded-lg bg-row-hover p-1" role="group" aria-label="Origen del plan">
            <Pestania activa={origen === 'plantilla'} onClick={() => setOrigen('plantilla')}>Desde una plantilla</Pestania>
            <Pestania activa={origen === 'cero'} onClick={() => setOrigen('cero')}>Plan personalizado</Pestania>
          </div>

          {origen === 'plantilla' ? (
            <div className="mt-5">
              <Field label="Elegí una plantilla" htmlFor="asignar-plantilla">
                <select id="asignar-plantilla" value={plantillaId} onChange={(ev) => setPlantillaId(ev.target.value)} className={inputClass}>
                  <option value="">Seleccionar plantilla...</option>
                  {datos.plantillas.map(p => <option key={p.id} value={String(p.id)}>{p.nombre}</option>)}
                </select>
              </Field>
              {datos.plantillas.length === 0 && (
                <p className="mt-2 text-xs text-ink-muted">Todavía no hay plantillas. Podés armar el plan con «Plan personalizado».</p>
              )}
              {plantilla && <PrevisualizacionPlantilla plantilla={plantilla} inicio={hoy} />}
              <p className="mt-4 flex items-start gap-1.5 text-xs text-ink-soft">
                <Info className="mt-px h-3.5 w-3.5 shrink-0" />
                Se creará una copia independiente para {alumno ? alumno.nombre : 'el alumno'}. Los cambios que hagas en su plan no modificarán la plantilla original.
              </p>
            </div>
          ) : (
            <div className="mt-5">
              <h3 className="mb-4 text-sm font-semibold text-ink">Datos del plan</h3>
              <DatosPlan form={form} setForm={setForm} idPrefijo="plan" placeholderNombre="Ej. Plan de fuerza de Juan" />
              <div className="mt-8 border-t border-line pt-6">
                <EditorSemanas form={form} setForm={setForm} catalogo={datos.catalogo}
                  idsPermitidosInactivos={SIN_IDS_INACTIVOS} sujeto="el plan" plan={{ inicio: hoy }} irA={irA} />
              </div>
              <p className="mt-4 flex items-start gap-1.5 text-xs text-ink-soft">
                <Info className="mt-px h-3.5 w-3.5 shrink-0" />
                El plan queda guardado solo para {alumno ? alumno.nombre : 'el alumno'}; no se agrega a las plantillas.
              </p>
            </div>
          )}
        </Panel>

        {fin && (
          <p className="mt-4 text-xs text-ink-soft">
            Empieza hoy ({textoFecha(hoy)}) y dura {plural(semanas, 'semana', 'semanas')}: termina el {textoFecha(fin)} inclusive.
            Desde el {textoFecha(sumarDias(fin, 1))} pasa a Finalizado solo.
          </p>
        )}
      </fieldset>

      <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <Button variant="ghost" onClick={onCancelar} disabled={enviando}>Cancelar</Button>
        <Button variant="primary" onClick={handleAsignar} disabled={bloqueado || verificando || enviando}>
          {bloqueado && <Lock className="h-4 w-4" />}
          {enviando ? 'Asignando...' : 'Asignar plan'}
        </Button>
      </div>

      <ConfirmModal
        isOpen={confirmando}
        title="Confirmar asignación"
        message={confirmando && alumno && fin && `¿Desea asignar el plan “${nombrePlan}” a ${nombreCompleto(alumno)}? Son ${plural(semanas, 'semana', 'semanas')}, desde hoy hasta el ${textoFecha(fin)}.`}
        onConfirm={asignar}
        onCancel={() => setConfirmando(false)}
      />
    </div>
  );
}
