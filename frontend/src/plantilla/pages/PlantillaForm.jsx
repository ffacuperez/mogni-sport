import { useEffect, useState } from 'react';
import { ArrowLeft, CircleAlert } from 'lucide-react';

import ConfirmModal from '../../shared/components/ConfirmModal';
import { Cargando, FormActions, PageHeader, Panel } from '../../shared/components/ui';
import { cx, FOCUS } from '../../shared/components/clases';
import { listarEjercicios } from '../../ejercicio/services/ejerciciosService';
import { actualizarPlantilla, crearPlantilla, idsDeEjercicios, obtenerPlantilla } from '../services/plantillasService';
import { DatosPlan, EditorSemanas } from '../components/EditorPlan';
import { aFormulario, formularioVacio, ubicacionDelError, validarFormulario } from '../components/formularioPlan';

/** Alta (sin plantillaId) o modificación de una plantilla. */
export default function PlantillaForm({ plantillaId, onVolver, onGuardada, onToast }) {
  const esEdicion = plantillaId != null;
  const [form, setForm] = useState(esEdicion ? null : formularioVacio);
  const [catalogo, setCatalogo] = useState(null);
  const [idsPermitidosInactivos, setIdsPermitidosInactivos] = useState(() => new Set());
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  // La semana y la sesión del error de validación, para mostrarlas y llevar la pantalla hasta ahí
  const [irA, setIrA] = useState(null);

  useEffect(() => {
    Promise.all([listarEjercicios(), esEdicion ? obtenerPlantilla(plantillaId) : null])
      .then(([ejercicios, plantilla]) => {
        setCatalogo(ejercicios);
        if (plantilla) {
          setForm(aFormulario(plantilla));
          // Ejercicios dados de baja que ya estaban en la plantilla: se pueden conservar
          setIdsPermitidosInactivos(idsDeEjercicios(plantilla));
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

  const handleSubmit = (ev) => {
    ev.preventDefault();
    const mensaje = validarFormulario(form);
    if (mensaje) {
      onToast(mensaje, true);
      const ubicacion = ubicacionDelError(form);
      if (ubicacion) setIrA({ ...ubicacion, vez: Date.now() });
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
    <div className="mx-auto max-w-5xl animate-fade-in">
      {botonVolver}

      <PageHeader
        title={esEdicion ? 'Modificación de plantilla' : 'Alta de nueva plantilla'}
        subtitle="Armá cada semana con sus sesiones. Al asignarla, el alumno recibe una copia con todas las semanas."
      />

      <Panel as="form" onSubmit={handleSubmit} className="p-6 md:p-8" noValidate>
        <h2 className="mb-4 text-base font-semibold text-ink">Datos de la plantilla</h2>
        <DatosPlan form={form} setForm={setForm} ayudaNombre="El nombre debe ser único para identificar esta plantilla." />

        <div className="mt-8 border-t border-line pt-6">
          <EditorSemanas form={form} setForm={setForm} catalogo={catalogo} idsPermitidosInactivos={idsPermitidosInactivos}
            irA={irA} />
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
