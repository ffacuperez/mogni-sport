import { Fragment, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { History } from 'lucide-react';

import { Cargando } from '../../shared/components/ui';
import ErrorConsulta from '../components/ErrorConsulta';
import EstadoVacio from '../components/EstadoVacio';
import FiltroPlan from '../components/FiltroPlan';
import ItemSesion from '../components/ItemSesion';
import TituloSeccion from '../components/TituloSeccion';
import { useProgreso } from '../services/ProgresoContext';
import { agruparPorSemana, nombreCortoPlan } from '../services/progresoService';

function SinSesiones() {
  return <EstadoVacio icono={History} {...useProgreso().textos.sinSesiones} />;
}

/**
 * Tab "Sesiones": las sesiones registradas del plan elegido, agrupadas por semana. El plan queda en la URL (?plan=),
 * así se conserva al volver del detalle de una sesión. Por defecto, el activo o, si no hay, el anterior más reciente.
 */
export default function TabSesiones() {
  const { consultas, textos } = useProgreso();
  const [params, setParams] = useSearchParams();
  const [planes, setPlanes] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlanes()
      .then((respuesta) => { if (vigente) setPlanes([respuesta.activo, ...respuesta.anteriores].filter(Boolean)); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas]);

  const plan = planes?.find((p) => String(p.id) === params.get('plan')) ?? planes?.[0];
  const planId = plan?.id;

  useEffect(() => {
    if (!planId) return undefined;
    let vigente = true;
    consultas.consultarSesiones(planId)
      .then((sesiones) => { if (vigente) setResultado({ planId, sesiones }); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas, planId]);

  if (error) return <ErrorConsulta error={error} />;

  if (!planes) return <Cargando texto={textos.cargandoSesiones} />;

  // Sin ningún plan no hay nada que filtrar: solo el estado vacío
  if (!plan) return <SinSesiones />;

  // Al cambiar de plan, lo anterior deja de valer hasta que llega la respuesta nueva
  const sesiones = resultado?.planId === planId ? resultado.sesiones : null;

  return (
    <div className="flex flex-col gap-3">
      <FiltroPlan planes={planes} valor={planId} onCambiar={(id) => setParams({ plan: id }, { replace: true })} />
      {!sesiones && <Cargando texto={textos.cargandoSesiones} />}
      {sesiones?.length === 0 && <SinSesiones />}
      {sesiones && agruparPorSemana(sesiones).map(({ semana, sesiones: deLaSemana }) => (
        <Fragment key={semana}>
          <div className="pt-1">
            <TituloSeccion>{nombreCortoPlan(plan)} · Semana {semana}</TituloSeccion>
          </div>
          {deLaSemana.map((sesion) => <ItemSesion key={sesion.id} sesion={sesion} />)}
        </Fragment>
      ))}
    </div>
  );
}
