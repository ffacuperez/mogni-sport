import { useState } from 'react';
import { CircleAlert, Info } from 'lucide-react';

import { Button, Field, Modal } from '../../shared/components/ui';
import { cx, inputClass } from '../../shared/components/clases';

const MENSAJE_INVALIDO = 'El cupo máximo tiene que ser un número entero mayor a 0.';

/** Cambiar el cupo máximo de alumnos por turno (HU-252). onGuardar(cupo) devuelve una promesa. */
export default function CupoModal({ cupoActual, onGuardar, onCancelar }) {
  const [valor, setValor] = useState(String(cupoActual ?? ''));
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const numero = Number(valor);
  const valido = valor.trim() !== '' && Number.isInteger(numero) && numero > 0;
  const error = valor.trim() !== '' && !valido ? MENSAJE_INVALIDO : errorServidor;

  const guardar = async (e) => {
    e.preventDefault();
    if (!valido || enviando) return;
    setEnviando(true);
    try {
      await onGuardar(numero);
    } catch (err) {
      setErrorServidor(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal title="Editar cupo máximo" subtitle="Cantidad máxima de alumnos en cada turno" onClose={onCancelar} cerrable={!enviando}>
      <form onSubmit={guardar} noValidate>
        <Field label="Alumnos por turno" htmlFor="cupo-maximo">
          <input
            id="cupo-maximo"
            type="number"
            inputMode="numeric"
            min="1"
            step="1"
            value={valor}
            onChange={(e) => { setValor(e.target.value); setErrorServidor(null); }}
            aria-invalid={Boolean(error)}
            autoFocus
            className={cx(inputClass, 'tabular-nums', error && 'border-primary ring-1 ring-primary')}
          />
        </Field>

        {error && (
          <p role="alert" className="mt-3 flex items-start gap-2 text-sm font-medium text-error">
            <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" /> {error}
          </p>
        )}

        <p className="mt-4 flex items-start gap-2 text-xs leading-relaxed text-ink-soft">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          El nuevo cupo se aplica a los turnos que todavía no fueron generados. Los turnos ya publicados mantienen su cupo.
        </p>

        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" onClick={onCancelar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={!valido || enviando}>
            {enviando ? 'Guardando...' : 'Guardar cupo'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
