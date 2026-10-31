import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CircleAlert, Plus, Trash2, X } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Button, Cargando, Field, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { listarEjercicios } from '../../ejercicio/services/ejerciciosService';
import {
  actualizarPlantilla, crearPlantilla, LIMITES, NIVELES, OBJETIVOS, obtenerPlantilla,
} from '../services/plantillasService';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

// Claves locales para las listas del formulario (las sesiones y ejercicios nuevos todavía no tienen id)
let ultimaClave = 0;
const nuevaClave = () => `n${++ultimaClave}`;

const ejercicioVacio = () => ({ clave: nuevaClave(), ejercicioId: '', series: '', repeticiones: '', peso: '', rir: '' });
const sesionVacia = () => ({ clave: nuevaClave(), nombre: '', ejercicios: [ejercicioVacio()] });

/** Plantilla del backend -> estado del formulario. */
const aFormulario = (plantilla) => ({
  nombre: plantilla.nombre,
  nivel: plantilla.nivel,
  objetivo: plantilla.objetivo,
  sesiones: plantilla.sesiones.map(s => ({
    clave: nuevaClave(),
    nombre: s.nombre,
    ejercicios: s.ejercicios.map(e => ({
      clave: nuevaClave(),
      ejercicioId: String(e.ejercicioId),
      series: String(e.series),
      repeticiones: String(e.repeticiones),
      peso: e.pesoSugerido == null ? '' : String(e.pesoSugerido).replace('.', ','),
      rir: e.rirObjetivo == null ? '' : String(e.rirObjetivo),
    })),
  })),
});

// Los campos enteros no filtran lo que se escribe: si no, "8.5" quedaría como 85 sin ningún aviso. Se valida al confirmar.
const esEntero = (texto) => /^\d+$/.test(texto.trim());
const PESO = /^\d{0,3}([.,]\d{0,2})?$/;

/** Primer error del formulario, o null. Mismos límites que el backend. */
function validar(form) {
  if (!form.nombre.trim()) return 'El nombre de la plantilla es obligatorio.';
  if (!form.nivel) return 'Elegí el nivel de la plantilla.';
  if (!form.objetivo) return 'Elegí el objetivo de la plantilla.';
  if (form.sesiones.length === 0) return 'La plantilla tiene que tener al menos una sesión.';
  for (const [i, sesion] of form.sesiones.entries()) {
    const nSesion = `Sesión ${i + 1}`;
    if (sesion.ejercicios.length === 0) return `${nSesion}: agregá al menos un ejercicio.`;
    for (const [j, e] of sesion.ejercicios.entries()) {
      const donde = `${nSesion}, ejercicio ${j + 1}`;
      if (!e.ejercicioId) return `${donde}: elegí un ejercicio del catálogo.`;
      const series = Number(e.series);
      if (!esEntero(e.series) || series < 1 || series > LIMITES.seriesMax) return `${donde}: las series tienen que ser un número entero entre 1 y ${LIMITES.seriesMax}.`;
      const reps = Number(e.repeticiones);
      if (!esEntero(e.repeticiones) || reps < 1 || reps > LIMITES.repeticionesMax) return `${donde}: las repeticiones objetivo tienen que ser un número entero entre 1 y ${LIMITES.repeticionesMax}.`;
      if (e.peso.trim()) {
        const peso = Number(e.peso.replace(',', '.'));
        if (!(peso > 0) || peso > LIMITES.pesoMax) return `${donde}: el peso sugerido tiene que ser mayor a 0 y hasta ${LIMITES.pesoMax} kg.`;
      }
      if (e.rir.trim() && (!esEntero(e.rir) || Number(e.rir) > LIMITES.rirMax)) return `${donde}: el RIR objetivo tiene que ser un número entero entre 0 y ${LIMITES.rirMax}.`;
    }
  }
  return null;
}

/** Selector de ejercicio agrupado por grupo muscular. Los dados de baja solo aparecen si ya estaban elegidos. */
function SelectorEjercicio({ id, value, onChange, catalogo, idsPermitidosInactivos }) {
  const grupos = useMemo(() => {
    const visibles = catalogo.filter(e => e.activo !== false || idsPermitidosInactivos.has(String(e.id)));
    const porGrupo = new Map();
    for (const e of visibles) {
      if (!porGrupo.has(e.grupoMuscular)) porGrupo.set(e.grupoMuscular, []);
      porGrupo.get(e.grupoMuscular).push(e);
    }
    return [...porGrupo.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [catalogo, idsPermitidosInactivos]);

  return (
    <select id={id} value={value} onChange={(ev) => onChange(ev.target.value)} className={cx(inputClass, 'bg-card')}>
      <option value="">Seleccionar ejercicio...</option>
      {grupos.map(([grupo, ejercicios]) => (
        <optgroup key={grupo} label={grupo}>
          {ejercicios.map(e => (
            <option key={e.id} value={String(e.id)}>{e.nombre}{e.activo === false ? ' (dado de baja)' : ''}</option>
          ))}
        </optgroup>
      ))}
    </select>
  );
}

function TarjetaSesion({ sesion, numero, puedeQuitar, catalogo, idsPermitidosInactivos, onCambiar, onQuitar }) {
  const cambiarEjercicio = (clave, campo, valor) =>
    onCambiar({ ...sesion, ejercicios: sesion.ejercicios.map(e => (e.clave === clave ? { ...e, [campo]: valor } : e)) });
  const quitarEjercicio = (clave) => onCambiar({ ...sesion, ejercicios: sesion.ejercicios.filter(e => e.clave !== clave) });
  const agregarEjercicio = () => onCambiar({ ...sesion, ejercicios: [...sesion.ejercicios, ejercicioVacio()] });

  const pref = `sesion-${sesion.clave}`;
  return (
    <section className="rounded-lg bg-row-hover p-4 sm:p-5" aria-label={`Sesión ${numero}`}>
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-sm font-bold text-ink">
          Sesión {numero} <span className="ml-1.5 text-xs font-normal text-ink-soft">{plural(sesion.ejercicios.length, 'ejercicio', 'ejercicios')}</span>
        </p>
        <button
          type="button"
          onClick={onQuitar}
          disabled={!puedeQuitar}
          title={puedeQuitar ? undefined : 'La plantilla tiene que tener al menos una sesión'}
          className={cx('flex cursor-pointer items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-primary transition-colors hover:bg-error-bg disabled:cursor-not-allowed disabled:text-ink-muted disabled:hover:bg-transparent', FOCUS)}
        >
          <Trash2 className="h-3.5 w-3.5" /> Quitar sesión
        </button>
      </div>

      <Field label="Nombre de la sesión (opcional)" htmlFor={`${pref}-nombre`} className="mb-4">
        <input id={`${pref}-nombre`} type="text" value={sesion.nombre} maxLength={LIMITES.nombreSesionMax}
          onChange={(ev) => onCambiar({ ...sesion, nombre: ev.target.value })}
          placeholder="Ej. Full body A" className={cx(inputClass, 'bg-card')} />
      </Field>

      <ul className="divide-y divide-line">
        {sesion.ejercicios.map((e, j) => {
          const idE = `${pref}-ej-${e.clave}`;
          return (
            <li key={e.clave} className="py-4 first:pt-0">
              <Field label="Ejercicio del catálogo" htmlFor={`${idE}-ejercicio`}>
                <div className="flex items-center gap-2">
                  <SelectorEjercicio id={`${idE}-ejercicio`} value={e.ejercicioId} catalogo={catalogo}
                    idsPermitidosInactivos={idsPermitidosInactivos}
                    onChange={(valor) => cambiarEjercicio(e.clave, 'ejercicioId', valor)} />
                  <button type="button" onClick={() => quitarEjercicio(e.clave)} aria-label={`Quitar ejercicio ${j + 1} de la sesión ${numero}`}
                    className={cx('shrink-0 cursor-pointer rounded-md p-2 text-ink-muted transition-colors hover:bg-card hover:text-primary', FOCUS)}>
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </Field>
              <div className="mt-3 grid grid-cols-1 gap-3 pr-0 sm:grid-cols-2 sm:pr-10 lg:grid-cols-4">
                <Field label="Series" htmlFor={`${idE}-series`}>
                  <input id={`${idE}-series`} type="text" inputMode="numeric" value={e.series} placeholder="Ej. 4"
                    onChange={(ev) => cambiarEjercicio(e.clave, 'series', ev.target.value)}
                    className={cx(inputClass, 'bg-card tabular-nums')} />
                </Field>
                <Field label="Repeticiones objetivo" htmlFor={`${idE}-reps`}>
                  <input id={`${idE}-reps`} type="text" inputMode="numeric" value={e.repeticiones} placeholder="Ej. 10"
                    onChange={(ev) => cambiarEjercicio(e.clave, 'repeticiones', ev.target.value)}
                    className={cx(inputClass, 'bg-card tabular-nums')} />
                </Field>
                <Field label="Peso sugerido (opcional)" htmlFor={`${idE}-peso`}>
                  <div className="relative">
                    <input id={`${idE}-peso`} type="text" inputMode="decimal" value={e.peso} placeholder="Sin especificar"
                      onChange={(ev) => PESO.test(ev.target.value) && cambiarEjercicio(e.clave, 'peso', ev.target.value)}
                      className={cx(inputClass, 'bg-card pr-9 tabular-nums')} />
                    <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-ink-muted">kg</span>
                  </div>
                </Field>
                <Field label="RIR objetivo (opcional)" htmlFor={`${idE}-rir`}>
                  <input id={`${idE}-rir`} type="text" inputMode="numeric" value={e.rir} maxLength={2} placeholder="Sin especificar"
                    title="Repeticiones en reserva: cuántas repeticiones le deberían quedar al terminar la serie (0 a 10)"
                    onChange={(ev) => cambiarEjercicio(e.clave, 'rir', ev.target.value)}
                    className={cx(inputClass, 'bg-card tabular-nums')} />
                </Field>
              </div>
            </li>
          );
        })}
      </ul>

      <button type="button" onClick={agregarEjercicio}
        className={cx('mt-1 flex cursor-pointer items-center gap-1 rounded-md px-1 py-1 text-xs font-semibold text-primary hover:underline', FOCUS)}>
        <Plus className="h-3.5 w-3.5" /> Agregar ejercicio
      </button>
    </section>
  );
}

/** Alta (sin plantillaId) o modificación de una plantilla. */
export default function PlantillaForm({ plantillaId, onVolver, onGuardada, onToast }) {
  const esEdicion = plantillaId != null;
  const [form, setForm] = useState(esEdicion ? null : { nombre: '', nivel: '', objetivo: '', sesiones: [sesionVacia()] });
  const [catalogo, setCatalogo] = useState(null);
  const [idsPermitidosInactivos, setIdsPermitidosInactivos] = useState(() => new Set());
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([listarEjercicios(), esEdicion ? obtenerPlantilla(plantillaId) : null])
      .then(([ejercicios, plantilla]) => {
        setCatalogo(ejercicios);
        if (plantilla) {
          setForm(aFormulario(plantilla));
          // Ejercicios dados de baja que ya estaban en la plantilla: se pueden conservar
          setIdsPermitidosInactivos(new Set(plantilla.sesiones.flatMap(s => s.ejercicios.map(e => String(e.ejercicioId)))));
        }
      })
      .catch((err) => setError(err.message));
  }, [plantillaId, esEdicion]);

  const botonVolver = (
    <button type="button" onClick={onVolver}
      className={cx('mb-3 flex cursor-pointer items-center gap-1.5 rounded-md text-xs font-medium text-ink-soft transition-colors hover:text-ink', FOCUS)}>
      <ArrowLeft className="h-3.5 w-3.5" /> Volver a plantillas
    </button>
  );

  if (error) {
    return (
      <div className="mx-auto max-w-3xl">
        {botonVolver}
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      </div>
    );
  }
  if (!form || !catalogo) return <Cargando texto={esEdicion ? 'Cargando plantilla' : 'Cargando ejercicios'} />;

  const totalEjercicios = form.sesiones.reduce((total, s) => total + s.ejercicios.length, 0);
  const cambiarSesion = (clave, sesion) => setForm(prev => ({ ...prev, sesiones: prev.sesiones.map(s => (s.clave === clave ? sesion : s)) }));
  const quitarSesion = (clave) => setForm(prev => ({ ...prev, sesiones: prev.sesiones.filter(s => s.clave !== clave) }));
  const agregarSesion = () => setForm(prev => ({ ...prev, sesiones: [...prev.sesiones, sesionVacia()] }));

  const handleSubmit = (ev) => {
    ev.preventDefault();
    const mensaje = validar(form);
    if (mensaje) {
      onToast(mensaje, true);
      return;
    }
    setConfirmando(true);
  };

  const guardar = async () => {
    setConfirmando(false);
    setEnviando(true);
    try {
      if (esEdicion) await actualizarPlantilla(plantillaId, form);
      else await crearPlantilla(form);
      onToast(esEdicion ? 'Plantilla modificada correctamente.' : 'Plantilla creada correctamente.');
      onGuardada();
    } catch (err) {
      onToast(err.message, true);
      setEnviando(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl animate-fade-in">
      {botonVolver}

      <PageHeader
        title={esEdicion ? 'Modificación de plantilla' : 'Alta de nueva plantilla'}
        subtitle="Definí el plan y organizá los ejercicios de cada sesión."
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <h2 className="mb-4 text-base font-semibold text-ink">Datos de la plantilla</h2>
        <Field label="Nombre" htmlFor="plantilla-nombre">
          <input id="plantilla-nombre" type="text" value={form.nombre} maxLength={LIMITES.nombreMax}
            onChange={(ev) => setForm(prev => ({ ...prev, nombre: ev.target.value }))}
            placeholder="Ej. Hipertrofia · tren superior" className={inputClass} />
          <p className="mt-1.5 text-xs text-ink-muted">El nombre debe ser único para identificar esta plantilla.</p>
        </Field>

        <div className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">
          <Field label="Nivel" htmlFor="plantilla-nivel">
            <select id="plantilla-nivel" value={form.nivel} onChange={(ev) => setForm(prev => ({ ...prev, nivel: ev.target.value }))} className={inputClass}>
              <option value="">Seleccionar...</option>
              {NIVELES.map(n => <option key={n.valor} value={n.valor}>{n.texto}</option>)}
            </select>
          </Field>
          <Field label="Objetivo" htmlFor="plantilla-objetivo">
            <select id="plantilla-objetivo" value={form.objetivo} onChange={(ev) => setForm(prev => ({ ...prev, objetivo: ev.target.value }))} className={inputClass}>
              <option value="">Seleccionar...</option>
              {OBJETIVOS.map(o => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
            </select>
          </Field>
        </div>

        <div className="mt-8 border-t border-line pt-6">
          <div className="mb-1 flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="text-base font-semibold text-ink">Sesiones</h2>
            <p className="text-xs tabular-nums text-ink-soft">
              {plural(form.sesiones.length, 'sesión', 'sesiones')} · {plural(totalEjercicios, 'ejercicio', 'ejercicios')}
            </p>
          </div>
          <p className="mb-4 text-xs text-ink-soft">Agregá al menos una sesión con ejercicios. El peso y el RIR son opcionales.</p>

          <div className="space-y-4">
            {form.sesiones.map((sesion, i) => (
              <TarjetaSesion
                key={sesion.clave}
                sesion={sesion}
                numero={i + 1}
                puedeQuitar={form.sesiones.length > 1}
                catalogo={catalogo}
                idsPermitidosInactivos={idsPermitidosInactivos}
                onCambiar={(nueva) => cambiarSesion(sesion.clave, nueva)}
                onQuitar={() => quitarSesion(sesion.clave)}
              />
            ))}
          </div>

          <Button variant="ghost" onClick={agregarSesion}
            className="mt-4 w-full border border-dashed border-line text-ink hover:border-primary-light hover:text-primary">
            <Plus className="h-4 w-4" /> Agregar sesión
          </Button>
          {catalogo.every(e => e.activo === false) && (
            <p className="mt-3 text-xs text-error">No hay ejercicios activos en el catálogo. Cargalos primero en la sección Ejercicios.</p>
          )}
        </div>

        <FormActions onCancel={onVolver} enviando={enviando} />
      </Panel>

      <ConfirmModal
        isOpen={confirmando}
        title={esEdicion ? 'Confirmar cambios' : 'Confirmar creación'}
        message={esEdicion ? '¿Desea confirmar los cambios en la plantilla?' : '¿Desea confirmar la creación de la nueva plantilla?'}
        onConfirm={guardar}
        onCancel={() => setConfirmando(false)}
      />
    </div>
  );
}
