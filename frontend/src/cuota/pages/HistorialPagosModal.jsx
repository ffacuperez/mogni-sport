import { formatearFecha, formatearMonto, labelMedioDePago } from '../services/cuotasService';
import { Button, Cargando, Modal } from '../../shared/components/ui';

const COLUMNAS = ['Fecha de pago', 'Desde', 'Hasta', 'Membresía', 'Monto', 'Medio de pago'];

/**
 * HU-48: cada pago con el período que cubrió (desde y hasta), su membresía y el desglose si tuvo recargo. Un cambio de
 * membresía aparece como otro pago (la diferencia, o $0 si la nueva no era más cara).
 */
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
        <div className="max-h-96 overflow-auto rounded-lg border border-line">
          <table className="w-full text-center text-sm">
            <thead className="sticky top-0 bg-thead">
              <tr>
                {COLUMNAS.map((th) => (
                  <th key={th} scope="col" className="h-12 whitespace-nowrap px-3 text-center text-[13px] font-semibold text-ink-soft">{th}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-line-soft bg-card">
              {pagos.map((pago) => (
                <tr key={pago.id}>
                  <td className="whitespace-nowrap px-3 py-3 tabular-nums text-ink">{formatearFecha(pago.fecha)}</td>
                  <td className="whitespace-nowrap px-3 py-3 tabular-nums text-ink">{formatearFecha(pago.inicio)}</td>
                  <td className="whitespace-nowrap px-3 py-3 tabular-nums text-ink">{formatearFecha(pago.vencimiento)}</td>
                  <td className="px-3 py-3 text-ink">{pago.membresia}</td>
                  <td className="whitespace-nowrap px-3 py-3 tabular-nums">
                    <span className="font-semibold text-success">{formatearMonto(pago.monto)}</span>
                    {pago.recargo > 0 && (
                      <span className="block text-xs text-ink-soft">
                        (cuota {formatearMonto(pago.precio)} + recargo {formatearMonto(pago.recargo)})
                      </span>
                    )}
                    {pago.cambioDeMembresia && (
                      <span className="block text-xs text-ink-soft">
                        {pago.monto > 0 ? 'Diferencia por cambio de membresía' : 'Cambio de membresía, sin cobro'}
                      </span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-3 py-3 text-ink-soft">{labelMedioDePago(pago.medioDePago)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="mt-6 flex justify-end">
        <Button variant="ghost" onClick={onClose}>Cerrar</Button>
      </div>
    </Modal>
  );
}
