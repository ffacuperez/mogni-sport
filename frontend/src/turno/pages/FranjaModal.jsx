import { useState } from 'react';
import { CircleAlert, Repeat } from 'lucide-react';

import { Button, Field, Modal } from '../../shared/components/ui';
import { cx, FOCUS, inputClass } from '../../shared/components/clases';
import { calcularTurnos, DIAS_CON_FRANJAS, etiquetaTurno, textoDias, validarFranjas } from '../services/franjasService';

/**
 * Alta y modificación de una franja (HU-252). Al crear se pueden elegir varios días: se crea una franja por día con
 * el mismo horario. Al modificar se edita una sola franja, así que se elige un único día. Valida mientras se
 * completa con los mismos mensajes que el backend; si igual el backend rechaza el guardado, su mensaje se muestra
 * acá mismo.
 * onGuardar({ diasSemana, horaInicio, horaFin }) devuelve una promesa: si falla, el modal queda abierto con el error.
 */
export default function FranjaModal({ franja, franjas, onGuardar, onCancelar }) {
  const esEdicion = Boolean(franja);
  const [datos, setDatos] = useState({
    diasSemana: franja ? [franja.diaSemana] : [],
    horaInicio: franja?.horaInicio ?? '',
    horaFin: franja?.horaFin ?? '',
  });
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo, valor) => {
    setDatos(prev => ({ ...prev, [campo]: valor }));
    setErrorServidor(null);
  };
  // Al crear, cada click marca o desmarca el día; al modificar, elige el único día de la franja
  const alternarDia = (valor) => {
    setDatos(prev => ({
      ...prev,
      diasSemana: esEdicion ? [valor] : prev.diasSemana.includes(valor)
        ? prev.diasSemana.filter(d => d !== valor)
        : [...prev.diasSemana, valor],
    }));
    setErrorServidor(null);
  };

  const completo = Boolean(datos.diasSemana.length && datos.horaInicio && datos.horaFin);
  const horasCargadas = Boolean(datos.horaInicio && datos.horaFin);
  const erroresValidacion = horasCargadas ? validarFranjas(datos, franjas, franja?.id) : [];
  const errores = erroresValidacion.length ? erroresValidacion : (errorServidor ? [errorServidor] : []);
  const valido = completo && erroresValidacion.length === 0;
  const turnos = valido ? calcularTurnos(datos.horaInicio, datos.horaFin) : [];
  const cantidadDias = datos.diasSemana.length;
  // La superposición marca las dos horas; los demás errores, la de fin
  const marcarInicio = erroresValidacion.some(e => e.startsWith('Esta franja se superpone'));
  const marcarFin = erroresValidacion.length > 0;

  const guardar = async (e) => {
    e.preventDefault();
    if (!valido || enviando) return;
    setEnviando(true);
    try {
      await onGuardar(datos);
    } catch (err) {
      setErrorServidor(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const claseHora = (conError) => cx(inputClass, 'tabular-nums', conError && 'border-primary ring-1 ring-primary');

  return (
    <Modal title={esEdicion ? 'Editar franja horaria' : 'Nueva franja horaria'} onClose={onCancelar} size="lg" cerrable={!enviando}>
      <form onSubmit={guardar} noValidate>
        <fieldset className="mb-5">
          <legend className="mb-2 text-sm font-medium text-ink">
            {esEdicion ? 'Día' : 'Días'}
            {!esEdicion && <span className="ml-2 text-xs font-normal text-ink-soft">Podés elegir varios</span>}
          </legend>
          <div className="flex flex-wrap gap-2">
            {DIAS_CON_FRANJAS.map(dia => {
              const elegido = datos.diasSemana.includes(dia.valor);
              return (
                <button
                  key={dia.valor}
                  type="button"
                  onClick={() => alternarDia(dia.valor)}
                  aria-pressed={elegido}
                  className={cx(
                    'min-w-14 flex-1 cursor-pointer rounded-full border px-3 py-2 text-sm font-medium transition-colors',
                    elegido ? 'border-primary bg-primary text-on-dark' : 'border-line bg-card text-ink hover:bg-row-hover',
                    FOCUS,
                  )}
                >
                  {dia.corto}
                </button>
              );
            })}
          </div>
        </fieldset>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Desde" htmlFor="franja-desde">
            <input id="franja-desde" type="time" value={datos.horaInicio} onChange={(e) => cambiar('horaInicio', e.target.value)}
              aria-invalid={marcarInicio} className={claseHora(marcarInicio)} />
          </Field>
          <Field label="Hasta" htmlFor="franja-hasta">
            <input id="franja-hasta" type="time" value={datos.horaFin} onChange={(e) => cambiar('horaFin', e.target.value)}
              aria-invalid={marcarFin} className={claseHora(marcarFin)} />
          </Field>
        </div>

        {errores.length > 0 && (
          <div role="alert" className="mt-4 space-y-1">
            {errores.map(mensaje => (
              <p key={mensaje} className="flex items-start gap-2 text-sm font-medium text-error">
                <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {mensaje}
              </p>
            ))}
          </div>
        )}

        {valido && (
          <div className="mt-5 rounded-lg bg-row-hover p-4">
            <p className="text-sm font-semibold text-ink">
              {cantidadDias > 1 ? 'Cada franja tiene' : 'Esta franja tiene'} {turnos.length} {turnos.length === 1 ? 'turno' : 'turnos'} de 1 hora
            </p>
            <ul className="mt-3 flex flex-wrap gap-2">
              {turnos.map(turno => (
                <li key={turno.horaInicio} className="rounded-full bg-error-bg px-3 py-1 text-xs font-semibold tabular-nums text-primary">
                  {etiquetaTurno(turno)}
                </li>
              ))}
            </ul>
            <p className="mt-3 text-xs leading-relaxed text-ink-soft">
              Los cambios se aplican a todos los turnos, incluyendo los de hoy y mañana. Si hay reservas afectadas,
              se cancelan con clase extra.
            </p>
          </div>
        )}

        {cantidadDias > 0 && (
          <p className="mt-4 flex items-center gap-2 text-sm text-ink-soft">
            <Repeat className="h-4 w-4 shrink-0" /> Esta franja se repite todos los {textoDias(datos.diasSemana)}.
          </p>
        )}

        <div className="mt-6 flex justify-end gap-3 border-t border-line pt-5">
          <Button variant="ghost" onClick={onCancelar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={!valido || enviando}>
            {enviando ? 'Guardando...' : cantidadDias > 1 ? `Guardar ${cantidadDias} franjas` : 'Guardar franja'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
