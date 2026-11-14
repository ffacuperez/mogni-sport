import { useEffect, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router';
import { ChevronLeft } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Cargando } from '../../shared/components/ui';
import BadgeEstadoPlan from '../components/BadgeEstadoPlan';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';

import DrawerSesionRuta from './DrawerSesion';
import ResumenPlanPanel from './ResumenPlanPanel';
import SesionesDeLaSemana from './SesionesDeLaSemana';
import { useSesionAbierta } from './useSesionAbierta';

/**
 * Detalle de un plan anterior, de solo lectura y a pantalla completa (sin las pestañas de la ficha). Arranca en la
 * última semana. La flecha vuelve siempre al historial de planes.
 */
export default function DetallePlanPanel({ alumno }) {
  const { planId } = useParams();
  const navigate = useNavigate();
  const { consultas, rutaBase } = useProgreso();
  const { abrir } = useSesionAbierta();
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlan(planId)
      .then((plan) => { if (vigente) setResultado({ planId, plan }); })
      .catch((error) => { if (vigente) setResultado({ planId, error }); });
    return () => { vigente = false; };
  }, [consultas, planId]);

  const actual = resultado?.planId === planId ? resultado : null;

  if (actual?.error) return <ErrorConsulta error={actual.error} />;

  if (!actual) return <Cargando texto="Cargando el plan" />;

  const { plan } = actual;

  // Solo se llega con la URL escrita a mano: el plan activo se ve en Plan actual
  if (plan.estado === 'ACTIVO') return <Navigate to={rutaBase} replace />;

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <header className="flex items-center gap-4">
        <button
          type="button"
          onClick={() => navigate(`${rutaBase}/planes`)}
          aria-label="Volver al historial de planes"
          className={cx('flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center rounded-full bg-card text-ink shadow-card transition-colors hover:bg-row-hover', FOCUS)}
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <div className="min-w-0">
          <p className="text-sm text-ink-soft">Historial de planes · {alumno.nombre} {alumno.apellido}</p>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <h1 className="text-2xl font-bold text-ink md:text-[28px]">{plan.nombre}</h1>
            <BadgeEstadoPlan estado={plan.estado} />
            <Badge>Solo lectura</Badge>
          </div>
        </div>
      </header>

      <ResumenPlanPanel plan={plan} />
      <SesionesDeLaSemana key={plan.id} plan={plan} semanaInicial={plan.cantidadSemanas} onVerRegistro={abrir} />
      <DrawerSesionRuta alumno={alumno} />
    </div>
  );
}
