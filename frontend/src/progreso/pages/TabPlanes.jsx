import { useEffect, useState } from 'react';
import { History } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import EstadoVacio from '../components/EstadoVacio';
import TarjetaPlan from '../components/TarjetaPlan';
import TituloSeccion from '../components/TituloSeccion';
import { consultarPlanes } from '../services/progresoService';

function SinAnteriores() {
  return <EstadoVacio icono={History} titulo="Aún no tenés planes anteriores" texto="Cuando termines tu plan actual, lo vas a encontrar acá." />;
}

/** Tab "Planes": el plan activo destacado y debajo los anteriores. Sin ningún plan, solo el estado vacío. */
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

  const hayAnteriores = planes.anteriores.length > 0;

  if (!planes.activo && !hayAnteriores) return <SinAnteriores />;

  return (
    <div className="flex flex-col gap-3">
      {planes.activo && (
        <>
          <TituloSeccion>Plan activo</TituloSeccion>
          <TarjetaPlan plan={planes.activo} />
        </>
      )}
      <div className={planes.activo ? 'pt-2' : undefined}>
        <TituloSeccion>Anteriores</TituloSeccion>
      </div>
      {hayAnteriores ? planes.anteriores.map((plan) => <TarjetaPlan key={plan.id} plan={plan} />) : <SinAnteriores />}
    </div>
  );
}
