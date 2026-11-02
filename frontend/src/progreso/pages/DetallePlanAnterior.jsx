import { useEffect, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router';

import { Cargando } from '../../shared/components/ui';
import DetallePlan from '../components/DetallePlan';
import EncabezadoDetalle from '../components/EncabezadoDetalle';
import ErrorConsulta from '../components/ErrorConsulta';
import { consultarPlan, tituloPlan } from '../services/progresoService';

/** Detalle de un plan finalizado, de solo lectura. La flecha vuelve siempre al tab "Planes". */
export default function DetallePlanAnterior() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [plan, setPlan] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarPlan(id)
      .then((respuesta) => { if (vigente) setPlan(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [id]);

  if (error) return <ErrorConsulta error={error} />;

  if (!plan) return <Cargando texto="Cargando el plan" />;

  // Solo se llega con la URL escrita a mano: el plan activo se ve en su tab
  if (plan.estado === 'ACTIVO') return <Navigate to="/mi-cuenta/progreso" replace />;

  return (
    <div className="animate-fade-in">
      <EncabezadoDetalle
        titulo={tituloPlan(plan)}
        subtitulo="Plan anterior · solo lectura"
        onVolver={() => navigate('/mi-cuenta/progreso/planes')}
      />
      <DetallePlan plan={plan} />
    </div>
  );
}
