import { formatearFecha, formatearMonto, labelMedioDePago } from '../services/cuotasService';

const COLUMNAS = ['Fecha de pago', 'Desde', 'Hasta', 'Membresía', 'Monto', 'Medio de pago'];

/**
 * HU-48: cada pago con el período que cubrió (desde y hasta), su membresía y el desglose si tuvo recargo. Un cambio de
 * membresía aparece como otro pago (la diferencia, o $0 si la nueva no era más cara). La usa la pestaña Cuotas de la
 * ficha.
 */
export default function TablaPagos({ pagos }) {
  return (
    <div className="overflow-auto rounded-lg border border-line">
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
  );
}
