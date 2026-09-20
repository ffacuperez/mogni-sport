// --- SHARED COMPONENTS (Main App) ---
export default function ConfirmModal({ isOpen, title, message, onConfirm, onCancel }) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-slate-800 border border-slate-700 rounded-lg p-6 max-w-sm w-full shadow-2xl animate-in zoom-in-95 duration-200">
        <h3 className="text-xl font-bold text-white mb-2">{title}</h3>
        <p className="text-slate-300 mb-6">{message}</p>
        <div className="flex justify-end gap-3">
          <button onClick={onCancel} className="px-4 py-2 rounded bg-slate-700 text-white hover:bg-slate-600 transition-colors">
            Cancelar
          </button>
          <button onClick={onConfirm} className="px-4 py-2 rounded bg-red-800 text-white hover:bg-red-900 transition-colors">
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}
