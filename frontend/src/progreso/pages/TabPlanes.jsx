import { useEffect, useState } from 'react';
import { History } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import EstadoVacio from '../components/EstadoVacio';
import TarjetaPlan from '../components/TarjetaPlan';
import TituloSeccion from '../components/TituloSeccion';
import { useProgreso } from '../services/ProgresoContext';

function SinAnteriores() {
  return <EstadoVacio icono={History} {...useProgreso().textos.sinAnteriores} />;
}

/** Tab "Planes": el plan activo destacado y debajo los anteriores. Sin ningún plan, solo el estado vacío. */
export default function TabPlanes() {
  const { consultas, textos } = useProgreso();
  const [planes, setPlanes] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlanes()
      .then((respuesta) => { if (vigente) setPlanes(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas]);

  if (error) return <ErrorConsulta error={error} />;

  if (!planes) return <Cargando texto={textos.cargandoPlanes} />;

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
