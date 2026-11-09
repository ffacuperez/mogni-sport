import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router';
import { ArrowRight, Phone, TriangleAlert, X } from 'lucide-react';

import BotonWhatsApp from '../../shared/components/BotonWhatsApp';
import { Badge, Cargando } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { edadAl, estadoCuotaAgenda, textoMesYAnio } from '../services/agendaService';

const iniciales = (alumno) => `${alumno.nombre?.[0] ?? ''}${alumno.apellido?.[0] ?? ''}`.toUpperCase();

/** "2026-10-18" -> "18/10" */
const diaYMes = (fechaISO) => `${fechaISO.slice(8, 10)}/${fechaISO.slice(5, 7)}`;

const Vacio = () => <span className="font-normal text-ink-muted">—</span>;

function Fila({ label, children }) {
  return (
    <div className="flex items-center justify-between gap-4 py-2.5">
      <dt className="shrink-0 text-sm text-ink-soft">{label}</dt>
      <dd className="min-w-0 text-right text-sm font-semibold text-ink">{children}</dd>
    </div>
  );
}

/**
 * MOGNI-260: ficha rápida del alumno desde la agenda, en un panel lateral. Cierra con Escape o con un clic afuera.
 * alumnoAgenda: ítem del turno (nombre, membresía y cuota, lo mismo que muestra la fila).
 * alumnos: listado de MainApp (DNI, fecha de nacimiento, fecha de ingreso, objetivo, antecedentes y celular).
 * hoy: fecha de hoy según el backend (para calcular la edad).
 */
export default function FichaAlumnoPanel({ alumnoAgenda, alumnos, alumnosCargados, hoy, onClose }) {
  const botonCerrar = useRef(null);
  const datos = alumnos.find(a => a.id === alumnoAgenda.alumnoId);
  const cuota = estadoCuotaAgenda(alumnoAgenda.cuota);
  // La edad guardada en el backend solo se actualiza al editar el alumno: se calcula con la fecha de nacimiento
  const edad = datos?.fechaNacimiento && hoy ? edadAl(datos.fechaNacimiento, hoy) : null;
  const nombre = `${alumnoAgenda.nombre} ${alumnoAgenda.apellido}`;

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  // El foco entra al panel y, al cerrarlo, vuelve a la fila del alumno
  useEffect(() => {
    const anterior = document.activeElement;
    botonCerrar.current?.focus();
    return () => anterior?.focus?.();
  }, []);

  // Portal a body: el animate-fade-in de la página (transform) haría que el fixed quede encerrado en el contenido
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/50 animate-fade-in"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={`Ficha de ${nombre}`}
        className="flex h-full w-full max-w-sm flex-col bg-card shadow-modal animate-slide-in-right"
      >
        <div className="flex-1 overflow-y-auto p-5 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-ink-soft">Ficha del alumno</p>
            <button
              ref={botonCerrar}
              type="button"
              onClick={onClose}
              aria-label="Cerrar"
              className={cx('-mr-1.5 cursor-pointer rounded-full p-1.5 text-ink-soft transition-colors hover:bg-row-hover hover:text-ink', FOCUS)}
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="mt-5 flex items-center gap-4 border-b border-line pb-5">
            <span aria-hidden="true" className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-error-bg text-lg font-semibold text-primary">
              {iniciales(alumnoAgenda)}
            </span>
            <div className="min-w-0">
              <h2 className="truncate text-xl font-bold text-ink">{nombre}</h2>
              <p className="text-sm text-ink-soft tabular-nums">{datos?.dni ? `DNI ${datos.dni}` : 'DNI —'}</p>
            </div>
          </div>

          {!alumnosCargados ? <Cargando texto="Cargando datos del alumno" className="mt-10" /> : (
            <>
              <dl className="mt-3">
                <Fila label="Membresía">
                  {alumnoAgenda.membresiaNombre ?? 'Sin membresía'}
                  {/* Inicio de la inscripción activa */}
                  {datos?.fechaIngreso && (
                    <span className="block text-xs font-normal text-ink-muted">desde {textoMesYAnio(datos.fechaIngreso)}</span>
                  )}
                </Fila>
                <Fila label="Edad">{edad != null ? `${edad} años` : <Vacio />}</Fila>
                <Fila label="Objetivo">{datos?.objetivo || <Vacio />}</Fila>
                <Fila label="Cuota">
                  {cuota ? (
                    <span className="inline-flex items-center gap-2">
                      {alumnoAgenda.cuota.fechaVencimiento && (
                        <span className="text-xs font-normal text-ink-soft">Vence el {diaYMes(alumnoAgenda.cuota.fechaVencimiento)}</span>
                      )}
                      <Badge tone={cuota.tono}>{cuota.texto}</Badge>
                    </span>
                  ) : <Vacio />}
                </Fila>
                {!datos?.antecedentes && <Fila label="Antecedentes de salud"><Vacio /></Fila>}
              </dl>

              {datos?.antecedentes && (
                <div className="mt-3 flex gap-3 rounded-lg border border-warning/40 bg-warning-bg p-3">
                  <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0 text-warning-text" aria-hidden="true" />
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-warning-text">Antecedentes de salud</p>
                    <p className="mt-0.5 text-sm font-semibold text-ink">{datos.antecedentes}</p>
                  </div>
                </div>
              )}

              <div className="mt-3 flex items-center gap-3 rounded-lg bg-row-hover p-3">
                <Phone className="h-4 w-4 shrink-0 text-ink-soft" aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <p className="text-xs text-ink-soft">Celular</p>
                  <p className="text-sm font-semibold text-ink tabular-nums">{datos?.telefono || <Vacio />}</p>
                </div>
                {datos?.telefono && <BotonWhatsApp telefono={datos.telefono} />}
              </div>

              {!datos && (
                <p className="mt-4 text-xs text-ink-muted">No se encontraron más datos de este alumno en el listado.</p>
              )}
            </>
          )}
        </div>

        <div className="border-t border-line px-5 py-4 sm:px-6">
          <Link
            to={`/alumnos/${alumnoAgenda.alumnoId}`}
            className={cx('inline-flex items-center gap-1.5 rounded text-sm font-semibold text-primary hover:text-primary-hover hover:underline', FOCUS)}
          >
            Ver ficha completa <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </aside>
    </div>,
    document.body,
  );
}
