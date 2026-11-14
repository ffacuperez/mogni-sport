import { useEffect, useMemo, useState } from 'react';
import { ArrowRightToLine, ChevronDown, Copy, Lock, Plus, Trash2, X } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Button, Field } from '../../shared/components/ui';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { LIMITES, NIVELES, OBJETIVOS } from '../services/plantillasService';
import { diasDeLaSemana, fechaFinPorSemanas } from '../../plan/services/planesService';
import { copiarSemana, copiarSesiones, DESCANSO, ejercicioVacio, mayuscula, PESO, sesionVacia, totalSesiones } from './formularioPlan';

// Armado de un plan de entrenamiento por semanas: cada semana tiene su nota y sus propias sesiones, y cada sesión sus
// ejercicios del catálogo. Lo usan las plantillas (HU-265), la asignación desde cero (HU-266) y la modificación de un
// plan asignado (HU-267). El estado y la validación están en formularioPlan.js; plantillasService.formularioAApi lo
// pasa al backend.

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;

// Etiquetas principales de cada bloque de la sesión (su nombre y cada ejercicio) y la línea que separa esos bloques
const ETIQUETA_DESTACADA = 'text-sm font-bold text-ink';
const SEPARADOR = 'border-b-2 border-ink-muted/50';

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MESES_LARGOS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const partes = (iso) => { const [, m, d] = iso.split('-').map(Number); return { dia: d, mes: m - 1 }; };
const corta = (iso) => { const { dia, mes } = partes(iso); return `${dia} ${MESES[mes]}`; };

/** "2–8 nov" o "30 oct – 5 nov". */
function rangoCorto({ desde, hasta }) {
  const a = partes(desde);
  const b = partes(hasta);
  return a.mes === b.mes ? `${a.dia}–${b.dia} ${MESES[b.mes]}` : `${corta(desde)} – ${corta(hasta)}`;
}

/** "Del 9 al 15 de noviembre" o "Del 30 de octubre al 5 de noviembre". */
function rangoLargo({ desde, hasta }) {
  const a = partes(desde);
  const b = partes(hasta);
  return a.mes === b.mes
    ? `Del ${a.dia} al ${b.dia} de ${MESES_LARGOS[b.mes]}`
    : `Del ${a.dia} de ${MESES_LARGOS[a.mes]} al ${b.dia} de ${MESES_LARGOS[b.mes]}`;
}

/** "las semanas 4, 5 y 6" o "la semana 4". */
function listaSemanas(numeros) {
  if (numeros.length === 1) return `la semana ${numeros[0]}`;
  return `las semanas ${numeros.slice(0, -1).join(', ')} y ${numeros[numeros.length - 1]}`;
}

/** Selector de ejercicio agrupado por grupo muscular. Los dados de baja solo aparecen si ya estaban elegidos. */
function SelectorEjercicio({ id, value, onChange, catalogo, idsPermitidosInactivos, disabled }) {
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
    <select id={id} value={value} disabled={disabled} onChange={(ev) => onChange(ev.target.value)} className={cx(inputClass, 'bg-card')}>
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

/**
 * "Cargar en todos los ejercicios" (pedido del cliente): series, RIR y descanso que se copian a todos los ejercicios
 * de la sesión. Solo existe en el formulario; no se guarda. Los campos vacíos no se tocan.
 */
function CargarEnTodos({ pref, cantidad, onAplicar }) {
  const [valores, setValores] = useState({ series: '', rir: '', descanso: '' });
  const [aplicado, setAplicado] = useState(0);
  const hayAlgo = Object.values(valores).some(v => v.trim());
  const cambiar = (campo, valor) => { setValores(prev => ({ ...prev, [campo]: valor })); setAplicado(0); };
  const aplicar = () => {
    if (!hayAlgo) return;
    onAplicar(valores);
    setAplicado(cantidad);
  };
  const conEnter = (ev) => { if (ev.key === 'Enter') { ev.preventDefault(); aplicar(); } };
  const campo = cx(inputClass, 'bg-card tabular-nums');

  return (
    <div className="mb-4 rounded-lg border border-dashed border-ink-muted/60 bg-card p-3 sm:p-4">
      <p className="text-xs leading-relaxed text-ink-soft">
        <span className="text-sm font-bold text-ink">Cargar en todos los ejercicios</span>
        {' · '}Completá lo que quieras y tocá Enter o Aplicar: se copia a los {plural(cantidad, 'ejercicio', 'ejercicios')} de esta sesión. Después podés ajustar cada uno.
      </p>
      <div className="mt-3 flex flex-wrap items-end gap-3">
        <Field label="Series" htmlFor={`${pref}-todos-series`} className="w-28">
          <input id={`${pref}-todos-series`} type="text" inputMode="numeric" value={valores.series} placeholder="Ej. 4"
            onChange={(ev) => cambiar('series', ev.target.value)} onKeyDown={conEnter} className={campo} />
        </Field>
        <Field label="RIR objetivo" htmlFor={`${pref}-todos-rir`} className="w-28">
          <input id={`${pref}-todos-rir`} type="text" inputMode="numeric" maxLength={2} value={valores.rir} placeholder="Ej. 2"
            onChange={(ev) => cambiar('rir', ev.target.value)} onKeyDown={conEnter} className={campo} />
        </Field>
        <Field label="Descanso (min)" htmlFor={`${pref}-todos-descanso`} className="w-28">
          <input id={`${pref}-todos-descanso`} type="text" inputMode="decimal" value={valores.descanso} placeholder="Ej. 2"
            onChange={(ev) => DESCANSO.test(ev.target.value) && cambiar('descanso', ev.target.value)} onKeyDown={conEnter} className={campo} />
        </Field>
        <Button size="sm" onClick={aplicar} disabled={!hayAlgo} className="mb-0.5 py-2.5">Aplicar</Button>
        {aplicado > 0 && (
          <span role="status" className="mb-2.5 text-xs font-semibold text-success">
            Listo: aplicado a {plural(aplicado, 'ejercicio', 'ejercicios')}
          </span>
        )}
      </div>
    </div>
  );
}

/**
 * Una sesión del editor. Se despliega o se contrae desde su encabezado: contraída muestra solo el número, el nombre y
 * la cantidad de ejercicios. registro: si el alumno ya la empezó o la registró ({ completada, fecha }); en ese caso es
 * de solo lectura. soloLectura: también lo es si la semana ya pasó.
 */
function TarjetaSesion({
  sesion, numero, numeroSemana, abierta, onAlternar, quitar, registro, soloLectura, catalogo, idsPermitidosInactivos,
  onCambiar, onQuitar,
}) {
  const cambiarEjercicio = (clave, campo, valor) =>
    onCambiar({ ...sesion, ejercicios: sesion.ejercicios.map(e => (e.clave === clave ? { ...e, [campo]: valor } : e)) });
  const quitarEjercicio = (clave) => onCambiar({ ...sesion, ejercicios: sesion.ejercicios.filter(e => e.clave !== clave) });
  const agregarEjercicio = () => onCambiar({ ...sesion, ejercicios: [...sesion.ejercicios, ejercicioVacio()] });
  const cargarEnTodos = ({ series, rir, descanso }) => onCambiar({
    ...sesion,
    ejercicios: sesion.ejercicios.map(e => ({
      ...e,
      ...(series.trim() && { series: series.trim() }),
      ...(rir.trim() && { rir: rir.trim() }),
      ...(descanso.trim() && { descanso: descanso.trim() }),
    })),
  });

  const bloqueada = soloLectura || Boolean(registro);
  const pref = `sesion-${sesion.clave}`;
  const nombre = sesion.nombre.trim();
  const fecha = registro?.fecha ? registro.fecha.split('-').reverse().slice(0, 2).join('/') : '';
  return (
    <section id={pref} className="scroll-mt-20 rounded-lg bg-row-hover p-4 sm:p-5" aria-label={`Sesión ${numero}`}>
      <div className={cx('flex flex-wrap items-center justify-between gap-x-3 gap-y-2', abierta && 'mb-3')}>
        <button
          type="button"
          onClick={onAlternar}
          aria-expanded={abierta}
          aria-controls={`${pref}-contenido`}
          className={cx('-m-1 flex min-w-0 flex-1 cursor-pointer flex-wrap items-center gap-x-2 gap-y-1 rounded-md p-1 text-left', FOCUS)}
        >
          <ChevronDown aria-hidden="true" className={cx('h-4 w-4 shrink-0 text-ink-soft transition-transform', !abierta && '-rotate-90')} />
          <span className="min-w-0 text-base font-bold text-ink">
            Sesión {numero}
            {nombre && <span className="font-semibold"> · {nombre}</span>}
          </span>
          <span className="whitespace-nowrap text-xs text-ink-soft">{plural(sesion.ejercicios.length, 'ejercicio', 'ejercicios')}</span>
          {registro && (
            <span className={cx('whitespace-nowrap rounded-full px-2 py-0.5 text-[11px] font-semibold',
              registro.completada ? 'bg-success-bg text-success' : 'bg-warning-bg text-warning-text')}>
              {registro.completada ? 'Registrada' : 'En curso'}{fecha && ` · ${fecha}`}
            </span>
          )}
        </button>
        {registro && !soloLectura ? (
          <span className="flex items-center gap-1 text-xs text-ink-soft">
            <Lock className="h-3.5 w-3.5" aria-hidden="true" />
            {registro.completada ? 'Ya la registró: solo lectura' : 'Ya la empezó: solo lectura'}
          </span>
        ) : !soloLectura && (
          <button
            type="button"
            onClick={onQuitar}
            disabled={!quitar.puede}
            title={quitar.motivo}
            aria-label={`Quitar la sesión ${numero} de la semana ${numeroSemana}`}
            className={cx('flex cursor-pointer items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-primary transition-colors hover:bg-error-bg disabled:cursor-not-allowed disabled:text-ink-muted disabled:hover:bg-transparent', FOCUS)}
          >
            <Trash2 className="h-3.5 w-3.5" /> Quitar sesión
          </button>
        )}
      </div>

      <div id={`${pref}-contenido`} hidden={!abierta}>
        {/* Dato de toda la sesión: la línea lo separa del primer ejercicio, igual que entre ejercicios */}
        <div className={cx('mb-4 pb-4', SEPARADOR)}>
          <Field label="Nombre de la sesión (opcional)" htmlFor={`${pref}-nombre`} labelClassName={ETIQUETA_DESTACADA}
            className={cx(!bloqueada && 'mb-4', 'max-w-lg')}>
            <input id={`${pref}-nombre`} type="text" value={sesion.nombre} maxLength={LIMITES.nombreSesionMax} disabled={bloqueada}
              onChange={(ev) => onCambiar({ ...sesion, nombre: ev.target.value })}
              placeholder="Ej. Full body A" className={cx(inputClass, 'bg-card')} />
          </Field>
          {!bloqueada && <CargarEnTodos pref={pref} cantidad={sesion.ejercicios.length} onAplicar={cargarEnTodos} />}
        </div>

        <ul className="divide-y-2 divide-ink-muted/50">
          {/* El número coincide con el de los mensajes de error ("Semana 1, sesión 2, ejercicio 3: ...") */}
          {sesion.ejercicios.map((e, j) => {
            const idE = `${pref}-ej-${e.clave}`;
            const campo = cx(inputClass, 'bg-card tabular-nums');
            return (
              <li key={e.clave} className="py-4 first:pt-0">
                <Field label={`${j + 1}. Ejercicio del catálogo`} htmlFor={`${idE}-ejercicio`} labelClassName={ETIQUETA_DESTACADA}>
                  <div className="flex items-center gap-2">
                    <SelectorEjercicio id={`${idE}-ejercicio`} value={e.ejercicioId} catalogo={catalogo} disabled={bloqueada}
                      idsPermitidosInactivos={idsPermitidosInactivos}
                      onChange={(valor) => cambiarEjercicio(e.clave, 'ejercicioId', valor)} />
                    {!bloqueada && (
                      <button type="button" onClick={() => quitarEjercicio(e.clave)} disabled={sesion.ejercicios.length === 1}
                        title={sesion.ejercicios.length === 1 ? 'La sesión tiene que tener al menos un ejercicio' : undefined}
                        aria-label={`Quitar ejercicio ${j + 1} de la sesión ${numero}`}
                        className={cx('shrink-0 cursor-pointer rounded-md p-2 text-ink-muted transition-colors hover:bg-card hover:text-primary disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-ink-muted', FOCUS)}>
                        <X className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                </Field>
                <div className={cx('mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3', !bloqueada && 'sm:pr-10')}>
                  <Field label="Series" htmlFor={`${idE}-series`}>
                    <input id={`${idE}-series`} type="text" inputMode="numeric" value={e.series} placeholder="Ej. 4" disabled={bloqueada}
                      onChange={(ev) => cambiarEjercicio(e.clave, 'series', ev.target.value)} className={campo} />
                  </Field>
                  <Field label="Repeticiones objetivo" htmlFor={`${idE}-reps`}>
                    <input id={`${idE}-reps`} type="text" value={e.repeticiones} placeholder="Ej. 8 o 8-10" disabled={bloqueada}
                      title="Un número fijo (8) o un rango mínimo-máximo (8-10)"
                      onChange={(ev) => cambiarEjercicio(e.clave, 'repeticiones', ev.target.value)} className={campo} />
                  </Field>
                  <Field label="Descanso sugerido (opcional)" htmlFor={`${idE}-descanso`}>
                    <div className="relative">
                      <input id={`${idE}-descanso`} type="text" inputMode="decimal" value={e.descanso} placeholder="Sin especificar"
                        disabled={bloqueada} title="Descanso entre series, en minutos (ej. 1,5 = un minuto y medio)"
                        onChange={(ev) => DESCANSO.test(ev.target.value) && cambiarEjercicio(e.clave, 'descanso', ev.target.value)}
                        className={cx(campo, 'pr-11')} />
                      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-ink-muted">min</span>
                    </div>
                  </Field>
                  <Field label="Peso sugerido (opcional)" htmlFor={`${idE}-peso`}>
                    <div className="relative">
                      <input id={`${idE}-peso`} type="text" inputMode="decimal" value={e.peso} placeholder="Sin especificar" disabled={bloqueada}
                        onChange={(ev) => PESO.test(ev.target.value) && cambiarEjercicio(e.clave, 'peso', ev.target.value)}
                        className={cx(campo, 'pr-9')} />
                      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-ink-muted">kg</span>
                    </div>
                  </Field>
                  <Field label="RIR objetivo (opcional)" htmlFor={`${idE}-rir`}>
                    <input id={`${idE}-rir`} type="text" inputMode="numeric" value={e.rir} maxLength={2} placeholder="Sin especificar"
                      disabled={bloqueada}
                      title="Repeticiones en reserva: cuántas repeticiones le deberían quedar al terminar la serie (0 a 10)"
                      onChange={(ev) => cambiarEjercicio(e.clave, 'rir', ev.target.value)} className={campo} />
                  </Field>
                  <Field label="Indicaciones (opcional)" htmlFor={`${idE}-indicaciones`} className="sm:col-span-2 lg:col-span-3">
                    <input id={`${idE}-indicaciones`} type="text" value={e.indicaciones} maxLength={LIMITES.indicacionesMax}
                      disabled={bloqueada} placeholder="Ej. Tempo 2-1-3, superserie con gemelos"
                      onChange={(ev) => cambiarEjercicio(e.clave, 'indicaciones', ev.target.value)} className={cx(inputClass, 'bg-card')} />
                  </Field>
                </div>
              </li>
            );
          })}
        </ul>

        {!bloqueada && (
          <button type="button" onClick={agregarEjercicio}
            className={cx('mt-1 flex cursor-pointer items-center gap-1 rounded-md px-1 py-1 text-xs font-semibold text-primary hover:underline', FOCUS)}>
            <Plus className="h-3.5 w-3.5" /> Agregar ejercicio
          </button>
        )}
      </div>
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

/** Chip de una semana en la barra de semanas. */
function ChipSemana({ numero, sub, estado, seleccionada, nota, onElegir }) {
  const pasada = estado === 'pasada';
  return (
    <button
      type="button"
      onClick={onElegir}
      aria-pressed={seleccionada}
      title={nota || undefined}
      className={cx(
        'flex min-h-[58px] w-[132px] cursor-pointer flex-col items-start gap-0.5 rounded-[10px] px-3 py-2 text-left transition-colors',
        seleccionada ? 'border-2 border-primary bg-error-bg' : cx('border border-line hover:border-ink-muted', pasada ? 'bg-row-hover' : 'bg-card'),
        FOCUS,
      )}
    >
      <span className={cx('flex w-full items-center gap-1.5 text-[13px] font-bold',
        seleccionada ? 'text-primary-hover' : pasada ? 'text-ink-soft' : 'text-ink')}>
        Semana {numero}
        {pasada && <Lock className="h-3 w-3 text-ink-soft" aria-label="Pasada" />}
        {nota && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-info" aria-label="Tiene nota" />}
      </span>
      <span className="text-xs text-ink-soft">{sub}</span>
      {estado === 'actual' && (
        <span className="mt-0.5 rounded-full bg-warning-bg px-2 py-0.5 text-[11px] font-semibold text-warning-text">En curso</span>
      )}
    </button>
  );
}

/**
 * Semanas del plan: una barra de chips para elegir la semana y, debajo, la semana elegida con su nota y sus sesiones.
 *
 * plan (opcional): { inicio, semanaEnCurso, registros, alumno } de un plan asignado. Con inicio, cada semana muestra sus
 * fechas. Con semanaEnCurso (al modificar), las semanas anteriores son de solo lectura, la semana en curso no se puede
 * eliminar y sus sesiones con registro (registros: Map "numero de sesión" → { completada, fecha }) tampoco se pueden
 * modificar. Sin plan es una plantilla: todo se puede editar.
 *
 * irA ({ semana, sesion, vez }): la semana y la sesión del error de validación; se muestra esa semana, se despliega la
 * sesión y la pantalla se mueve hasta ella. vez cambia en cada intento.
 */
export function EditorSemanas({ form, setForm, catalogo, idsPermitidosInactivos, sujeto = 'la plantilla', plan, irA }) {
  const enCurso = plan?.semanaEnCurso ?? null;
  const [elegidaPropia, setElegida] = useState(() => form.semanas[(enCurso ?? 1) - 1]?.clave ?? form.semanas[0]?.clave);
  const [abiertasPropias, setAbiertas] = useState(() => new Set());
  const [confirmacion, setConfirmacion] = useState(null);
  // El último error ya atendido (su "vez"): hasta que el usuario toca algo, se muestra la semana y la sesión del error
  const [atendido, setAtendido] = useState(null);
  const forzado = irA && atendido !== irA.vez ? irA : null;
  const elegida = forzado ? forzado.semana : elegidaPropia;
  const estaAbierta = (clave) => abiertasPropias.has(clave) || clave === forzado?.sesion;
  // Al tocar algo, lo que mostraba el error pasa a ser lo elegido por el usuario
  const consolidar = () => {
    if (!forzado) return;
    setElegida(forzado.semana);
    if (forzado.sesion) setAbiertas(prev => new Set(prev).add(forzado.sesion));
    setAtendido(forzado.vez);
  };
  const elegir = (clave) => { consolidar(); setElegida(clave); };

  // Con la semana del error ya en pantalla, la lleva hasta la sesión
  useEffect(() => {
    if (irA?.sesion) document.getElementById(`sesion-${irA.sesion}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [irA]);

  const semanas = form.semanas;
  const indice = Math.max(0, semanas.findIndex(s => s.clave === elegida));
  const semana = semanas[indice];
  const numero = indice + 1;
  const estadoDe = (i) => (enCurso == null ? 'libre' : i + 1 < enCurso ? 'pasada' : i + 1 === enCurso ? 'actual' : 'futura');
  const estado = estadoDe(indice);
  const editable = estado !== 'pasada';
  const borrable = (i) => semanas.length > 1 && (enCurso == null || i + 1 > enCurso);
  const siguientes = semanas.map((_, j) => j).filter(j => j > indice && (enCurso == null || j + 1 > enCurso));
  const enTope = semanas.length >= LIMITES.semanasMax;
  const fechas = (i) => (plan?.inicio ? diasDeLaSemana(plan.inicio, i + 1) : null);

  // Registros de la semana en curso: no se pueden quitar las sesiones registradas ni las que van antes de la última
  // registrada (correrían los números con los que el alumno las registró)
  const registros = estado === 'actual' ? (plan?.registros ?? new Map()) : new Map();
  const ultimaRegistrada = Math.max(0, ...registros.keys());

  const conSemanas = (cambiar, nuevaElegida) => {
    setForm(prev => ({ ...prev, semanas: cambiar([...prev.semanas]) }));
    if (nuevaElegida) elegir(nuevaElegida);
  };
  const cambiarSemana = (cambios) => conSemanas(ss => ss.map((s, i) => (i === indice ? { ...s, ...cambios } : s)));
  const cambiarSesion = (clave, sesion) => cambiarSemana({ sesiones: semana.sesiones.map(s => (s.clave === clave ? sesion : s)) });

  const agregarSemana = () => {
    const nueva = copiarSemana(semanas[semanas.length - 1], { conNota: false });
    conSemanas(ss => [...ss, nueva], nueva.clave);
  };
  // Una semana pasada no se puede insertar en el medio (correría las que ya pasaron): su copia va al final
  const posicionCopia = estado === 'pasada' ? semanas.length : indice + 1;
  const duplicar = () => {
    const copia = copiarSemana(semana);
    conSemanas(ss => { ss.splice(posicionCopia, 0, copia); return ss; }, copia.clave);
  };
  const pedirCopiarASiguientes = () => setConfirmacion({
    titulo: 'Copiar a las siguientes',
    texto: `Las sesiones de la semana ${numero} reemplazan a las de ${listaSemanas(siguientes.map(j => j + 1))}. Lo que tenían cargado se pierde.`,
    accion: () => conSemanas(ss => ss.map((s, j) => (siguientes.includes(j) ? { ...s, sesiones: copiarSesiones(semana.sesiones) } : s))),
  });
  const pedirEliminar = () => {
    const quedan = semanas.length - 1;
    const fin = plan?.inicio ? ` y termina el ${corta(fechaFinPorSemanas(plan.inicio, quedan))}` : '';
    setConfirmacion({
      titulo: `Eliminar la semana ${numero}`,
      texto: `Se eliminan la semana y sus ${plural(semana.sesiones.length, 'sesión', 'sesiones')}. `
        + (indice < semanas.length - 1 ? 'Las siguientes se corren un lugar. ' : '')
        + `${mayuscula(sujeto)} queda con ${plural(quedan, 'semana', 'semanas')}${fin}.`,
      peligro: true,
      // Queda elegida la semana anterior (o la siguiente, si era la primera)
      accion: () => conSemanas(ss => ss.filter((_, j) => j !== indice), (semanas[indice - 1] ?? semanas[indice + 1]).clave),
    });
  };

  const agregarSesion = () => {
    const nueva = sesionVacia();
    consolidar();
    cambiarSemana({ sesiones: [...semana.sesiones, nueva] });
    setAbiertas(prev => new Set(prev).add(nueva.clave));
  };
  const alternar = (clave) => {
    const abierta = estaAbierta(clave);
    consolidar();
    setAbiertas(prev => {
      const siguiente = new Set(prev);
      if (abierta) siguiente.delete(clave); else siguiente.add(clave);
      return siguiente;
    });
  };
  const todasAbiertas = semana.sesiones.every(s => estaAbierta(s.clave));
  const desplegarOContraerTodas = () => {
    consolidar();
    setAbiertas(prev => {
      const siguiente = new Set(prev);
      semana.sesiones.forEach(s => (todasAbiertas ? siguiente.delete(s.clave) : siguiente.add(s.clave)));
      return siguiente;
    });
  };

  const quitarDe = (k) => {
    if (semana.sesiones.length === 1) return { puede: false, motivo: 'La semana tiene que tener al menos una sesión' };
    if (k + 1 < ultimaRegistrada) {
      return { puede: false, motivo: `No se puede quitar: correría las sesiones que ${plan.alumno} ya registró` };
    }
    return { puede: true };
  };

  const finPlan = plan?.inicio ? fechaFinPorSemanas(plan.inicio, semanas.length) : null;
  const sesionesMax = LIMITES.sesionesPorSemanaMax;
  const botonTexto = cx('cursor-pointer rounded px-1 text-xs font-semibold text-primary hover:underline', FOCUS);

  return (
    <>
      {/* Barra de semanas */}
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-ink">Semanas</h2>
          <p className="mt-0.5 text-xs text-ink-soft">
            {plural(semanas.length, 'semana', 'semanas')} · {plural(totalSesiones(form), 'sesión', 'sesiones')}
            {finPlan && ` · del ${corta(plan.inicio)} al ${corta(finPlan)}`}
            {enCurso && ` · va por la semana ${enCurso}`}
          </p>
        </div>
        {enCurso && (
          <div className="flex flex-wrap gap-3 text-xs text-ink-soft">
            <span className="flex items-center gap-1"><Lock className="h-3 w-3" aria-hidden="true" /> Pasada: solo lectura</span>
            <span className="flex items-center gap-1">
              <span className="rounded-full bg-warning-bg px-2 py-0.5 text-[11px] font-semibold text-warning-text">En curso</span>
              se edita con cuidado
            </span>
          </div>
        )}
      </div>

      <div role="group" aria-label="Elegí la semana" className="mt-3 flex flex-wrap gap-2">
        {semanas.map((s, i) => (
          <ChipSemana key={s.clave} numero={i + 1} estado={estadoDe(i)} seleccionada={i === indice} nota={s.nota.trim()}
            sub={fechas(i) ? rangoCorto(fechas(i)) : plural(s.sesiones.length, 'sesión', 'sesiones')}
            onElegir={() => elegir(s.clave)} />
        ))}
        <button type="button" onClick={agregarSemana} disabled={enTope}
          title={enTope ? `Máximo ${LIMITES.semanasMax} semanas` : 'Agrega una copia de la última semana'}
          className={cx('flex min-h-[58px] w-[132px] cursor-pointer items-center justify-center gap-1.5 rounded-[10px] border-[1.5px] border-dashed border-ink-muted bg-card text-[13px] font-semibold text-primary transition-colors hover:border-primary disabled:cursor-not-allowed disabled:text-ink-muted disabled:hover:border-ink-muted', FOCUS)}>
          <Plus className="h-4 w-4" /> Agregar semana
        </button>
      </div>
      <p className="mt-2 text-xs text-ink-soft">
        {plan?.inicio
          ? `La semana 1 empieza el ${corta(plan.inicio)}, el día de la asignación. «Agregar semana» suma una copia de la última y corre la fecha fin. Máximo ${LIMITES.semanasMax} semanas.`
          : `«Agregar semana» suma una copia de la última. Máximo ${LIMITES.semanasMax} semanas.`}
      </p>

      {/* La semana elegida */}
      <section aria-labelledby="titulo-semana" className="mt-6 border-t border-line pt-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 id="titulo-semana" className="flex flex-wrap items-center gap-2 text-lg font-bold text-ink">
              Semana {numero}{semana.nota.trim() && <span className="font-semibold"> · {semana.nota.trim()}</span>}
              {estado === 'actual' && <span className="rounded-full bg-warning-bg px-2 py-0.5 text-[11px] font-semibold text-warning-text">En curso</span>}
              {estado === 'pasada' && <span className="rounded-full bg-row-hover px-2 py-0.5 text-[11px] font-semibold text-ink-soft">Pasada · solo lectura</span>}
            </h3>
            {fechas(indice) && (
              <p className="mt-0.5 text-xs text-ink-soft">{rangoLargo(fechas(indice))} · {plural(semana.sesiones.length, 'sesión', 'sesiones')}</p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" onClick={duplicar} disabled={enTope}
              title={enTope ? `${mayuscula(sujeto)} ya tiene el máximo de ${LIMITES.semanasMax} semanas` : `Agrega una copia como semana ${posicionCopia + 1}`}>
              <Copy className="h-3.5 w-3.5" /> {estado === 'pasada' ? 'Copiar al final' : 'Duplicar'}
            </Button>
            <Button size="sm" onClick={pedirCopiarASiguientes} disabled={siguientes.length === 0}
              title={siguientes.length === 0 ? 'No hay semanas siguientes que se puedan modificar' : `Reemplaza las sesiones de ${listaSemanas(siguientes.map(j => j + 1))}`}>
              <ArrowRightToLine className="h-3.5 w-3.5" /> Copiar a las siguientes
            </Button>
            <Button size="sm" variant="ghost" onClick={pedirEliminar} disabled={!borrable(indice)}
              className="text-primary hover:bg-error-bg"
              title={semanas.length === 1 ? `${mayuscula(sujeto)} tiene que tener al menos una semana`
                : !borrable(indice) ? 'Las semanas que ya empezaron no se pueden eliminar' : `Eliminar la semana ${numero}`}>
              <Trash2 className="h-3.5 w-3.5" /> Eliminar semana
            </Button>
          </div>
        </div>

        {estado === 'pasada' && (
          <p className="mt-4 rounded-lg bg-row-hover px-4 py-3 text-sm text-ink-soft">
            <strong className="text-ink">Esta semana ya pasó.</strong> Queda como referencia de lo indicado y no se puede modificar ni eliminar.
          </p>
        )}
        {estado === 'actual' && (
          <p className="mt-4 rounded-lg bg-warning-bg px-4 py-3 text-sm leading-relaxed text-ink">
            <strong>Es la semana en curso.</strong> Las sesiones que {plan.alumno} todavía no empezó se pueden modificar o quitar, y podés agregar sesiones al final. Las que ya registró o empezó quedan en solo lectura.
          </p>
        )}

        <Field label="Nota de la semana (opcional)" htmlFor={`nota-${semana.clave}`} className="mt-4 max-w-lg">
          <div className="relative">
            <input id={`nota-${semana.clave}`} type="text" value={semana.nota} maxLength={LIMITES.notaMax} disabled={!editable}
              onChange={(ev) => cambiarSemana({ nota: ev.target.value })} placeholder="Ej. Semana de descarga"
              className={cx(inputClass, 'pr-14')} />
            <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs tabular-nums text-ink-muted">
              {semana.nota.length}/{LIMITES.notaMax}
            </span>
          </div>
        </Field>

        <div className="mb-3 mt-6 flex flex-wrap items-baseline justify-between gap-2">
          <h4 className="text-base font-semibold text-ink">Sesiones</h4>
          <div className="flex flex-wrap items-baseline gap-x-3">
            <span className="text-xs tabular-nums text-ink-soft">{semana.sesiones.length} de {sesionesMax} como máximo</span>
            {semana.sesiones.length > 1 && (
              <button type="button" className={botonTexto} onClick={desplegarOContraerTodas}>
                {todasAbiertas ? 'Contraer todas' : 'Desplegar todas'}
              </button>
            )}
          </div>
        </div>

        <div className="space-y-3">
          {semana.sesiones.map((sesion, k) => (
            <TarjetaSesion
              key={sesion.clave}
              sesion={sesion}
              numero={k + 1}
              numeroSemana={numero}
              abierta={estaAbierta(sesion.clave)}
              onAlternar={() => alternar(sesion.clave)}
              quitar={quitarDe(k)}
              registro={registros.get(k + 1)}
              soloLectura={!editable}
              catalogo={catalogo}
              idsPermitidosInactivos={idsPermitidosInactivos}
              onCambiar={(nueva) => cambiarSesion(sesion.clave, nueva)}
              onQuitar={() => cambiarSemana({ sesiones: semana.sesiones.filter(s => s.clave !== sesion.clave) })}
            />
          ))}
        </div>

        {editable && (
          <Button variant="ghost" onClick={agregarSesion} disabled={semana.sesiones.length >= sesionesMax}
            title={semana.sesiones.length >= sesionesMax ? `Máximo ${sesionesMax} sesiones por semana` : undefined}
            className="mt-4 w-full border border-dashed border-line text-ink hover:border-primary-light hover:text-primary">
            <Plus className="h-4 w-4" /> Agregar sesión
          </Button>
        )}
        {catalogo.every(e => e.activo === false) && (
          <p className="mt-3 text-xs text-error">No hay ejercicios activos en el catálogo. Cargalos primero en la sección Ejercicios.</p>
        )}
      </section>

      <ConfirmModal
        isOpen={Boolean(confirmacion)}
        title={confirmacion?.titulo}
        message={confirmacion?.texto}
        danger={confirmacion?.peligro}
        onConfirm={() => { confirmacion.accion(); setConfirmacion(null); }}
        onCancel={() => setConfirmacion(null)}
      />
    </>
  );
}
