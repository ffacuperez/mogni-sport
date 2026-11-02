import { textoDescanso, textoRepeticiones } from '../services/plantillasService';

const plural = (n, singular, varios) => `${n} ${n === 1 ? singular : varios}`;
const formatoPeso = (peso) => `${Number(peso).toLocaleString('es-AR', { maximumFractionDigits: 2 })} kg`;

/** Tarjetas de las sesiones con sus ejercicios. Las usan el listado de plantillas y el plan de un alumno. */
export default function TarjetasSesiones({ sesiones }) {
  return (
    <ul className="grid grid-cols-1 items-start gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {sesiones.map(sesion => (
        <li key={sesion.id} className="rounded-lg bg-row-hover p-3">
          <div className="flex items-center gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-card text-sm font-bold tabular-nums text-ink shadow-card">
              {sesion.numero}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">
                Sesión {sesion.numero}{sesion.nombre && ` · ${sesion.nombre}`}
              </p>
              <p className="text-xs text-ink-soft">{plural(sesion.ejercicios.length, 'ejercicio', 'ejercicios')}</p>
            </div>
          </div>
          <ol className="mt-3 divide-y divide-line-soft rounded-md bg-card px-3 shadow-card">
            {sesion.ejercicios.map(ej => (
              <li key={ej.id} className="py-2.5">
                {/* Nombre a todo el ancho y los datos abajo: en columnas angostas, compartir fila lo aplastaba */}
                <p className="text-sm font-medium leading-snug text-ink">
                  {ej.nombre}
                  {!ej.ejercicioActivo && <span className="ml-1 text-xs font-normal text-ink-muted">(dado de baja)</span>}
                </p>
                <p className="mt-0.5 text-xs tabular-nums text-ink-soft">
                  <span className="font-semibold text-ink">{ej.series} × {textoRepeticiones(ej.repeticiones, ej.repeticionesMax)}</span>
                  {[
                    ej.pesoSugerido != null && formatoPeso(ej.pesoSugerido),
                    ej.rirObjetivo != null && `RIR ${ej.rirObjetivo}`,
                    ej.descansoMinutos != null && `${textoDescanso(ej.descansoMinutos)} desc.`,
                  ].filter(Boolean).map(dato => ` · ${dato}`).join('')}
                </p>
              </li>
            ))}
          </ol>
        </li>
      ))}
    </ul>
  );
}
