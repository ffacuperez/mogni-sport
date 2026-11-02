import { useEffect, useState } from 'react';
import { ArrowLeft, CircleAlert } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Cargando, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { listarEjercicios } from '../../ejercicio/services/ejerciciosService';
import { DatosPlan, EditorSesiones } from '../../plantilla/components/EditorPlan';
import { aFormulario, validarFormulario } from '../../plantilla/components/formularioPlan';
import { actualizarPlan, obtenerPlan } from '../services/planesService';

/** HU-266: modificación del plan de un alumno. Solo cambia su copia, no la plantilla de la que salió. */
export default function PlanAsignadoForm({ planId, onVolver, onGuardado, onToast }) {
  const [plan, setPlan] = useState(null);
  const [form, setForm] = useState(null);
  const [catalogo, setCatalogo] = useState(null);
  const [idsPermitidosInactivos, setIdsPermitidosInactivos] = useState(() => new Set());
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([listarEjercicios(), obtenerPlan(planId)])
      .then(([ejercicios, cargado]) => {
        setCatalogo(ejercicios);
        setPlan(cargado);
        setForm(aFormulario(cargado));
        // Ejercicios dados de baja que ya estaban en el plan: se pueden conservar
        setIdsPermitidosInactivos(new Set(cargado.sesiones.flatMap(s => s.ejercicios.map(e => String(e.ejercicioId)))));
      })
      .catch((err) => setError(err.message));
  }, [planId]);

  const botonVolver = (
    <button type="button" onClick={onVolver}
      className={cx('mb-3 flex cursor-pointer items-center gap-1.5 rounded-md text-xs font-medium text-ink-soft transition-colors hover:text-ink', FOCUS)}>
      <ArrowLeft className="h-3.5 w-3.5" /> Volver al plan
    </button>
  );

  const mensajeError = error ?? (plan && plan.estado !== 'ACTIVO' ? 'El plan está finalizado y ya no se puede modificar.' : null);
  if (mensajeError) {
    return (
      <div className="mx-auto max-w-3xl">
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

  return (
    <div className="mx-auto max-w-3xl animate-fade-in">
      {botonVolver}

      <PageHeader
        eyebrow={`Plan de entrenamiento · ${plan.alumnoNombre}`}
        title="Modificación del plan"
        subtitle="Los cambios se aplican solo al plan de este alumno."
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <h2 className="mb-4 text-base font-semibold text-ink">Datos del plan</h2>
        <DatosPlan form={form} setForm={setForm} idPrefijo="plan" />

        <div className="mt-8 border-t border-line pt-6">
          <EditorSesiones form={form} setForm={setForm} catalogo={catalogo}
            idsPermitidosInactivos={idsPermitidosInactivos} sujeto="el plan" />
        </div>

        <FormActions onCancel={onVolver} enviando={enviando} />
      </Panel>

      <ConfirmModal
        isOpen={confirmando}
        title="Confirmar cambios"
        message={`¿Desea confirmar los cambios en el plan de ${plan.alumnoNombre}? La plantilla original no se modifica.`}
        onConfirm={guardar}
        onCancel={() => setConfirmando(false)}
      />
    </div>
  );
}
