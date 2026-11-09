import { Button, Modal } from '../../shared/components/ui';

/**
 * Aviso de finalizar plan, el mismo en Progreso y en el detalle del plan. Sin foco inicial en el botón rojo, para no
 * confirmar sin querer con Enter una acción que no se puede deshacer.
 */
export default function FinalizarPlanModal({ nombrePlan, nombreAlumno, enviando = false, onConfirm, onCancel }) {
  return (
    <Modal title="Finalizar plan" onClose={onCancel} cerrable={!enviando}>
      <p className="mb-2.5 text-sm text-ink">¿Desea finalizar el plan “{nombrePlan}” de {nombreAlumno}?</p>
      <p className="mb-1.5 text-sm font-semibold text-ink">Al finalizarlo:</p>
      <ul className="mb-6 list-disc pl-5 text-sm leading-relaxed text-ink-soft">
        <li>Va a quedar sin plan activo y no va a poder registrar más sesiones de este plan.</li>
        <li>El plan y sus sesiones registradas pasan al historial.</li>
        <li>Le vas a poder asignar un plan nuevo.</li>
        <li>Esta acción no se puede deshacer.</li>
      </ul>
      <div className="flex justify-end gap-3">
        <Button variant="ghost" onClick={onCancel} disabled={enviando}>Cancelar</Button>
        <Button variant="danger" onClick={onConfirm} disabled={enviando}>{enviando ? 'Finalizando...' : 'Finalizar plan'}</Button>
      </div>
    </Modal>
  );
}
