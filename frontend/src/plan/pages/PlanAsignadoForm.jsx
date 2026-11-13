import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CircleAlert } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Cargando, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { listarEjercicios } from '../../ejercicio/services/ejerciciosService';
import { idsDeEjercicios } from '../../plantilla/services/plantillasService';
import { DatosPlan, EditorSemanas } from '../../plantilla/components/EditorPlan';
import { aFormulario, ubicacionDelError, validarFormulario } from '../../plantilla/components/formularioPlan';
import { actualizarPlan, fechaFinPorSemanas, obtenerPlan, textoFecha } from '../services/planesService';

/**
 * HU-267: modificación del plan de un alumno. Solo cambia su copia, no la plantilla de la que salió. Las semanas que
 * ya pasaron y las sesiones que el alumno ya empezó o registró quedan en solo lectura (el backend también las cuida).
 */
export default function PlanAsignadoForm({ planId, onVolver, onGuardado, onToast }) {
  const [plan, setPlan] = useState(null);
  const [form, setForm] = useState(null);
  const [catalogo, setCatalogo] = useState(null);
  const [idsPermitidosInactivos, setIdsPermitidosInactivos] = useState(() => new Set());
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  // La semana y la sesión del error de validación, para mostrarlas y llevar la pantalla hasta ahí
  const [irA, setIrA] = useState(null);

  useEffect(() => {
    Promise.all([listarEjercicios(), obtenerPlan(planId)])
      .then(([ejercicios, cargado]) => {
        setCatalogo(ejercicios);
        setPlan(cargado);
        setForm(aFormulario(cargado));
        // Ejercicios dados de baja que ya estaban en el plan: se pueden conservar
        setIdsPermitidosInactivos(idsDeEjercicios(cargado));
      })
      .catch((err) => setError(err.message));
  }, [planId]);

  // Lo que el editor necesita saber del plan: fechas, semana en curso y sesiones con registro de esa semana
  const contexto = useMemo(() => plan && {
    inicio: plan.fechaAsignacion,
    semanaEnCurso: plan.semanaEnCurso,
    registros: new Map(plan.sesionesConRegistro
      .filter(r => r.semana === plan.semanaEnCurso)
      .map(r => [r.numeroSesion, r])),
    alumno: plan.alumnoNombre.split(' ')[0],
  }, [plan]);

  const botonVolver = (
    <button type="button" onClick={onVolver}
      className={cx('mb-3 flex cursor-pointer items-center gap-1.5 rounded-md text-xs font-medium text-ink-soft transition-colors hover:text-ink', FOCUS)}>
      <ArrowLeft className="h-3.5 w-3.5" /> Volver al plan
    </button>
  );

  const mensajeError = error ?? (plan && plan.estado !== 'ACTIVO' ? 'El plan está finalizado y ya no se puede modificar.' : null);
  if (mensajeError) {
    return (
      <div className="mx-auto max-w-5xl">
        {botonVolver}
        <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
          <CircleAlert className="h-5 w-5 shrink-0" />
          <span className="text-sm">{mensajeError}</span>
        </div>
      </div>
    );
  }
  if (!form || !catalogo) return <Cargando texto="Cargando plan" />;

  const handleSubmit = (ev) => {
    ev.preventDefault();
    const mensaje = validarFormulario(form, 'el plan');
    if (mensaje) {
      onToast(mensaje, true);
      const ubicacion = ubicacionDelError(form, 'el plan');
      if (ubicacion) setIrA({ ...ubicacion, vez: Date.now() });
      return;
    }
    setConfirmando(true);
  };

  const guardar = async () => {
    setConfirmando(false);
    setEnviando(true);
    try {
      await actualizarPlan(planId, form);
      onToast('Plan modificado correctamente.');
      onGuardado();
    } catch (err) {
      onToast(err.message, true);
      setEnviando(false);
    }
  };

  const fin = fechaFinPorSemanas(plan.fechaAsignacion, form.semanas.length);

  return (
    <div className="mx-auto max-w-5xl animate-fade-in">
      {botonVolver}

      <PageHeader
        eyebrow={`Plan de entrenamiento · ${plan.alumnoNombre}`}
        title={`Editar plan de ${plan.alumnoNombre}`}
        subtitle={`Los cambios son solo para ${contexto.alumno}: la plantilla de origen no se toca.`}
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <h2 className="mb-4 text-base font-semibold text-ink">Datos del plan</h2>
        <DatosPlan form={form} setForm={setForm} idPrefijo="plan" />

        <div className="mt-8 border-t border-line pt-6">
          <EditorSemanas form={form} setForm={setForm} catalogo={catalogo} idsPermitidosInactivos={idsPermitidosInactivos}
            sujeto="el plan" plan={contexto} irA={irA} />
        </div>

        <FormActions onCancel={onVolver} enviando={enviando} />
      </Panel>

      <ConfirmModal
        isOpen={confirmando}
        title="Confirmar cambios"
        message={`¿Desea confirmar los cambios en el plan de ${plan.alumnoNombre}? Queda de ${form.semanas.length} ${form.semanas.length === 1 ? 'semana' : 'semanas'} y termina el ${textoFecha(fin)}. La plantilla original no se modifica.`}
        onConfirm={guardar}
        onCancel={() => setConfirmando(false)}
      />
    </div>
  );
}
