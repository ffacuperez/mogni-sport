import { Button, Cargando, Modal } from '../../shared/components/ui';
import TablaPagos from '../components/TablaPagos';

/** HU-48: el historial de pagos de un alumno, desde el listado de cuotas. */
export default function HistorialPagosModal({ alumno, pagos, cargando, onClose }) {
  if (!alumno) return null;

  return (
    <Modal
      title="Historial de pagos"
      subtitle={`${alumno.nombre} ${alumno.apellido} · DNI ${alumno.dni}`}
      onClose={onClose}
      size="xl"
    >
      {cargando ? (
        <Cargando texto="Cargando historial" className="py-8" />
      ) : pagos.length === 0 ? (
        <p className="rounded-lg border border-dashed border-line bg-row-hover px-4 py-8 text-center text-sm text-ink-soft">
          Este alumno no tiene pagos registrados.
        </p>
      ) : (
        <TablaPagos pagos={pagos} className="max-h-96" />
      )}

      <div className="mt-6 flex justify-end">
        <Button variant="ghost" onClick={onClose}>Cerrar</Button>
      </div>
    </Modal>
  );
}
