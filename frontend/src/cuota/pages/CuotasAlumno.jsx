import { useEffect, useState } from 'react';
import { Wallet } from 'lucide-react';

import { Cargando, Panel } from '../../shared/components/ui';
import TablaPagos from '../components/TablaPagos';
import { obtenerHistorialDePagos } from '../services/cuotasService';

/** Tab "Cuotas" de la ficha (solo el propietario): el historial de pagos del alumno (HU-48). */
export default function CuotasAlumno({ alumno }) {
  // null: cargando
  const [pagos, setPagos] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    obtenerHistorialDePagos(alumno.id)
      .then((respuesta) => { if (vigente) setPagos(respuesta); })
      .catch((err) => { if (vigente) setError(err.message); });
    return () => { vigente = false; };
  }, [alumno.id]);

  return (
    <Panel className="p-6">
      <h2 className="mb-5 border-b border-line pb-4 text-base font-semibold text-ink">Historial de pagos</h2>
      {error && <p role="alert" className="text-sm text-error">No se pudo consultar el historial: {error}</p>}
      {!error && !pagos && <Cargando texto="Cargando historial" className="py-8" />}
      {pagos?.length === 0 && (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <span className="flex h-14 w-14 items-center justify-center rounded-full bg-error-bg text-primary">
            <Wallet className="h-6 w-6" />
          </span>
          <p className="text-sm text-ink-soft">Este alumno no tiene pagos registrados.</p>
        </div>
      )}
      {pagos?.length > 0 && <TablaPagos pagos={pagos} />}
    </Panel>
  );
}
