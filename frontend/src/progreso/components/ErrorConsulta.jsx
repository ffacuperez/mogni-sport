import { CircleAlert } from 'lucide-react';

import AccesoBloqueado from '../../turno/components/AccesoBloqueado';
import { useProgreso } from '../services/ProgresoContext';

/** Error al consultar el progreso. 403: alumno inactivo; el mensaje con el motivo lo manda el backend. */
export default function ErrorConsulta({ error }) {
  const { textos } = useProgreso();
  if (error.status === 403) return <AccesoBloqueado titulo={textos.accesoBloqueado} mensaje={error.message} />;

  return (
    <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
      <CircleAlert className="h-5 w-5 shrink-0" />
      <span className="text-sm">{error.message}</span>
    </div>
  );
}
