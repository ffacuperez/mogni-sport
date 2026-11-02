import { useMemo } from 'react';
import { Plus, Trash2, X } from 'lucide-react';

import { Button, Field } from '../../shared/components/ui';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { LIMITES, NIVELES, OBJETIVOS } from '../services/plantillasService';
import { DESCANSO, ejercicioVacio, mayuscula, PESO, sesionVacia } from './formularioPlan';

// Armado de un plan de entrenamiento: datos (nombre, nivel, objetivo) y sesiones con ejercicios del catálogo.
// Lo usan el alta/modificación de plantillas (HU-265) y la asignación de un plan desde cero (HU-266).
// El estado y la validación están en formularioPlan.js; plantillasService.formularioAApi lo pasa al backend.

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

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

function TarjetaSesion({ sesion, numero, puedeQuitar, sujeto, catalogo, idsPermitidosInactivos, onCambiar, onQuitar }) {
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
          title={puedeQuitar ? undefined : `${mayuscula(sujeto)} tiene que tener al menos una sesión`}
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
              <div className="mt-3 grid grid-cols-1 gap-3 pr-0 sm:grid-cols-2 sm:pr-10 lg:grid-cols-3">
                <Field label="Series" htmlFor={`${idE}-series`}>
                  <input id={`${idE}-series`} type="text" inputMode="numeric" value={e.series} placeholder="Ej. 4"
                    onChange={(ev) => cambiarEjercicio(e.clave, 'series', ev.target.value)}
                    className={cx(inputClass, 'bg-card tabular-nums')} />
                </Field>
                <Field label="Repeticiones objetivo" htmlFor={`${idE}-reps`}>
                  <input id={`${idE}-reps`} type="text" value={e.repeticiones} placeholder="Ej. 8 o 8-10"
                    title="Un número fijo (8) o un rango mínimo-máximo (8-10)"
                    onChange={(ev) => cambiarEjercicio(e.clave, 'repeticiones', ev.target.value)}
                    className={cx(inputClass, 'bg-card tabular-nums')} />
                </Field>
                <Field label="Descanso sugerido (opcional)" htmlFor={`${idE}-descanso`}>
                  <div className="relative">
                    <input id={`${idE}-descanso`} type="text" inputMode="decimal" value={e.descanso} placeholder="Sin especificar"
                      title="Descanso entre series, en minutos (ej. 1,5 = un minuto y medio)"
                      onChange={(ev) => DESCANSO.test(ev.target.value) && cambiarEjercicio(e.clave, 'descanso', ev.target.value)}
                      className={cx(inputClass, 'bg-card pr-11 tabular-nums')} />
                    <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-ink-muted">min</span>
                  </div>
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

/** Nombre, nivel y objetivo. idPrefijo separa los ids si hay más de un formulario en la pantalla. */
export function DatosPlan({ form, setForm, idPrefijo = 'plantilla', ayudaNombre, placeholderNombre = 'Ej. Hipertrofia · tren superior' }) {
  return (
    <>
      <Field label="Nombre" htmlFor={`${idPrefijo}-nombre`}>
        <input id={`${idPrefijo}-nombre`} type="text" value={form.nombre} maxLength={LIMITES.nombreMax}
          onChange={(ev) => setForm(prev => ({ ...prev, nombre: ev.target.value }))}
          placeholder={placeholderNombre} className={inputClass} />
        {ayudaNombre && <p className="mt-1.5 text-xs text-ink-muted">{ayudaNombre}</p>}
      </Field>

      <div className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">
        <Field label="Nivel" htmlFor={`${idPrefijo}-nivel`}>
          <select id={`${idPrefijo}-nivel`} value={form.nivel} onChange={(ev) => setForm(prev => ({ ...prev, nivel: ev.target.value }))} className={inputClass}>
            <option value="">Seleccionar...</option>
            {NIVELES.map(n => <option key={n.valor} value={n.valor}>{n.texto}</option>)}
          </select>
        </Field>
        <Field label="Objetivo" htmlFor={`${idPrefijo}-objetivo`}>
          <select id={`${idPrefijo}-objetivo`} value={form.objetivo} onChange={(ev) => setForm(prev => ({ ...prev, objetivo: ev.target.value }))} className={inputClass}>
            <option value="">Seleccionar...</option>
            {OBJETIVOS.map(o => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
          </select>
        </Field>
      </div>
    </>
  );
}

/** Sesiones con sus ejercicios: agregar, quitar y completar cada una. */
export function EditorSesiones({ form, setForm, catalogo, idsPermitidosInactivos, sujeto = 'la plantilla' }) {
  const totalEjercicios = form.sesiones.reduce((total, s) => total + s.ejercicios.length, 0);
  const cambiarSesion = (clave, sesion) => setForm(prev => ({ ...prev, sesiones: prev.sesiones.map(s => (s.clave === clave ? sesion : s)) }));
  const quitarSesion = (clave) => setForm(prev => ({ ...prev, sesiones: prev.sesiones.filter(s => s.clave !== clave) }));
  const agregarSesion = () => setForm(prev => ({ ...prev, sesiones: [...prev.sesiones, sesionVacia()] }));

  return (
    <>
      <div className="mb-1 flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-base font-semibold text-ink">Sesiones</h2>
        <p className="text-xs tabular-nums text-ink-soft">
          {plural(form.sesiones.length, 'sesión', 'sesiones')} · {plural(totalEjercicios, 'ejercicio', 'ejercicios')}
        </p>
      </div>
      <p className="mb-4 text-xs text-ink-soft">Agregá al menos una sesión con ejercicios. Las repeticiones pueden ser un número fijo o un rango (ej. 8-10). El descanso, el peso y el RIR son opcionales.</p>

      <div className="space-y-4">
        {form.sesiones.map((sesion, i) => (
          <TarjetaSesion
            key={sesion.clave}
            sesion={sesion}
            numero={i + 1}
            puedeQuitar={form.sesiones.length > 1}
            sujeto={sujeto}
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
    </>
  );
}
