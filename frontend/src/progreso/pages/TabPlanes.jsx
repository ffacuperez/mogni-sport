import { useEffect, useState } from 'react';

import { Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import TarjetaPlan from '../components/TarjetaPlan';
import TituloSeccion from '../components/TituloSeccion';
import { consultarPlanes } from '../services/progresoService';

/** Tab "Planes": el plan activo destacado y debajo los anteriores. */
export default function TabPlanes() {
  const [planes, setPlanes] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarPlanes()
      .then((respuesta) => { if (vigente) setPlanes(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, []);

  if (error) return <ErrorConsulta error={error} />;

  if (!planes) return <Cargando texto="Cargando tus planes" />;

  return (
    <div className="flex flex-col gap-3">
      {planes.activo && (
        <>
          <TituloSeccion>Plan activo</TituloSeccion>
          <TarjetaPlan plan={planes.activo} />
        </>
      )}
      {planes.anteriores.length > 0 && (
        <>
          <div className={planes.activo ? 'pt-2' : undefined}>
            <TituloSeccion>Anteriores</TituloSeccion>
          </div>
          {planes.anteriores.map((plan) => <TarjetaPlan key={plan.id} plan={plan} />)}
        </>
      )}
    </div>
  );
}
