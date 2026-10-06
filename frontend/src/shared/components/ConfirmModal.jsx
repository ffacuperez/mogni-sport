import { Button, Modal } from './ui';

/** Confirmación genérica. danger: la acción es destructiva (baja, eliminación) y usa el botón Danger. */
export default function ConfirmModal({ isOpen, title, message, danger = false, onConfirm, onCancel }) {
  if (!isOpen) return null;
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="mb-6 text-sm leading-relaxed text-ink">{message}</p>
      <div className="flex justify-end gap-3">
        <Button variant="ghost" onClick={onCancel}>Cancelar</Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} autoFocus>
          Confirmar
        </Button>
      </div>
    </Modal>
  );
}
