import { useRef, useState } from 'react';

import { cx, inputClass, labelClass } from '../../shared/components/clases';
import { Button, Modal } from '../../shared/components/ui';
import {
  calcularRm, iniciarSesion, registrarEjercicio, textoDescansoSegundos, textoKg, textoSeriesRepeticiones,
} from '../services/progresoService';

const CAMPOS = [
  { campo: 'peso', titulo: 'Peso (kg)', etiqueta: 'Peso', inputMode: 'decimal' },
  { campo: 'repeticiones', titulo: 'Reps', etiqueta: 'Repeticiones', inputMode: 'numeric' },
  { campo: 'rir', titulo: 'RIR', etiqueta: 'RIR', inputMode: 'numeric' },
];

const entreEnteros = (valor, min, max) => /^\d+$/.test(valor) && Number(valor) >= min && Number(valor) <= max;

// Los mismos límites que valida el backend
const serieCompleta = ({ peso, repeticiones, rir }) => peso !== '' && Number(peso) >= 0 && Number(peso) <= 1000
  && entreEnteros(repeticiones, 1, 100) && entreEnteros(rir, 0, 10);

/** Una fila por serie del plan; si el ejercicio ya estaba hecho, arranca con lo cargado para corregirlo. */
const filasIniciales = (ejercicio) => Array.from({ length: ejercicio.series }, (_, i) => {
  const hecha = ejercicio.seriesRealizadas.find((serie) => serie.orden === i + 1);
  return hecha
    ? { peso: String(Number(hecha.peso)), repeticiones: String(hecha.repeticiones), rir: String(hecha.rir) }
    : { peso: '', repeticiones: '', rir: '' };
});

/**
 * HU-269: carga de un ejercicio. Pide peso, repeticiones y RIR de todas las series que indica el plan y una nota
 * opcional. Si la sesión todavía no empezó, la empieza al guardar el primer ejercicio.
 */
export default function RegistrarEjercicioModal({ ejercicio, sesion, planId, semana, onActualizada, onCerrar }) {
  const [filas, setFilas] = useState(() => filasIniciales(ejercicio));
  const [notas, setNotas] = useState(ejercicio.notas ?? '');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  // El estado tarda un render en deshabilitar el botón: el ref corta un doble clic en el mismo instante
  const enviando = useRef(false);

  const completas = filas.every(serieCompleta);
  const mejorRm = completas
    ? Math.max(...filas.map((f) => calcularRm(Number(f.peso), Number(f.repeticiones), Number(f.rir))))
    : null;
  const indicado = [
    textoSeriesRepeticiones(ejercicio),
    ejercicio.rirObjetivo != null && `RIR ${ejercicio.rirObjetivo}`,
    ejercicio.descansoMinutos != null && textoDescansoSegundos(ejercicio.descansoMinutos),
  ].filter(Boolean).join(' · ');

  const cambiar = (indice, campo, valor) => {
    setFilas((actuales) => actuales.map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)));
  };

  const guardar = async (e) => {
    e.preventDefault();
    if (!completas || enviando.current) return;
    enviando.current = true;
    setGuardando(true);
    setError('');
    try {
      let registroId = sesion.registroId;
      if (!registroId) {
        const iniciada = await iniciarSesion(planId, semana, sesion.numero);
        onActualizada(iniciada);
        registroId = iniciada.registroId;
      }
      const series = filas.map((f, i) => ({ orden: i + 1, peso: Number(f.peso), repeticiones: Number(f.repeticiones), rir: Number(f.rir) }));
      onActualizada(await registrarEjercicio(registroId, ejercicio.orden, { series, notas: notas.trim() || null }));
      onCerrar();
    } catch (err) {
      setError(err.message || 'No se pudo guardar el ejercicio');
      enviando.current = false;
      setGuardando(false);
    }
  };

  return (
    <Modal title={ejercicio.nombre} subtitle={indicado} onClose={onCerrar} cerrable={!guardando} size="lg">
      <form onSubmit={guardar} noValidate className="space-y-4">
        {ejercicio.indicaciones && <p className="text-sm text-ink-soft">{ejercicio.indicaciones}</p>}

        <div className="space-y-2">
          <div className="grid grid-cols-[2.5rem_repeat(3,minmax(0,1fr))] gap-2">
            <span className="text-xs font-medium text-ink-soft">Serie</span>
            {CAMPOS.map(({ campo, titulo }) => <span key={campo} className="text-xs font-medium text-ink-soft">{titulo}</span>)}
          </div>
          {filas.map((fila, i) => (
            <div key={i} className="grid grid-cols-[2.5rem_repeat(3,minmax(0,1fr))] items-center gap-2">
              <span className="text-sm font-bold text-ink-soft tabular-nums">{i + 1}</span>
              {CAMPOS.map(({ campo, etiqueta, inputMode }) => (
                <input
                  key={campo}
                  type="number"
                  inputMode={inputMode}
                  step={campo === 'peso' ? 'any' : 1}
                  min={0}
                  value={fila[campo]}
                  onChange={(e) => cambiar(i, campo, e.target.value)}
                  aria-label={`${etiqueta} de la serie ${i + 1}`}
                  disabled={guardando}
                  autoFocus={i === 0 && campo === 'peso'}
                  className={cx(inputClass, 'tabular-nums')}
                />
              ))}
            </div>
          ))}
        </div>

        <div>
          <label htmlFor="registrar-notas" className={labelClass}>Nota (opcional)</label>
          <textarea
            id="registrar-notas"
            rows={2}
            maxLength={500}
            value={notas}
            onChange={(e) => setNotas(e.target.value)}
            disabled={guardando}
            placeholder="Cómo te sentiste, molestias, cambios..."
            className={cx(inputClass, 'resize-none')}
          />
        </div>

        <p className="text-sm text-ink-soft" aria-live="polite">
          {mejorRm != null
            ? <>1RM estimado: <span className="font-semibold text-ink">{textoKg(mejorRm)}</span> (la mejor serie)</>
            : `Completá peso, repeticiones y RIR de ${ejercicio.series === 1 ? 'la serie' : `las ${ejercicio.series} series`} para guardar.`}
        </p>

        {error && <p className="text-sm text-error" role="alert">{error}</p>}

        <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
          <Button variant="ghost" onClick={onCerrar} disabled={guardando}>Cancelar</Button>
          <Button type="submit" variant="primary" disabled={!completas || guardando}>
            {guardando ? 'Guardando...' : 'Guardar ejercicio'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
