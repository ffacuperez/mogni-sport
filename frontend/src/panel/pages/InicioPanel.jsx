import { useEffect, useState } from 'react';

import { Cargando, PageHeader } from '../../shared/components/ui';
import { GRILLA_TARJETAS } from '../components/estilos';
import PlanesPorVencer from '../components/PlanesPorVencer';
import SeccionSegura, { ErrorSeccion } from '../components/SeccionSegura';
import { consultarPanel, consultarPlanesPorVencer } from '../services/panelService';
import PanelPropietario from './PanelPropietario';

/** null si la sección no corresponde a este perfil (no se pide); si no, { datos, error }. */
const resultadoDe = (permitido, respuesta) => {
  if (!permitido) return null;
  return respuesta.status === 'fulfilled'
    ? { datos: respuesta.value, error: null }
    : { datos: null, error: respuesta.reason };
};

/**
 * Inicio del personal. Cada perfil pide solo lo que puede ver (el backend también lo exige): los indicadores del
 * negocio, solo el propietario; los planes por vencer, el propietario y el entrenador. Los dos pedidos salen en
 * paralelo y la pantalla espera a ambos con un solo "Cargando": recién ahí se dibuja todo junto. Si uno falla, el
 * resto se muestra igual y el error aparece solo en el lugar de esa sección.
 */
export default function InicioPanel({ esPropietario, puedeVerPlanes, onVerMora, onVerAlumno }) {
  const [inicio, setInicio] = useState(null);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([
      esPropietario ? consultarPanel() : Promise.resolve(null),
      puedeVerPlanes ? consultarPlanesPorVencer() : Promise.resolve(null),
    ]).then(([panel, planes]) => {
      if (vigente) setInicio({ panel: resultadoDe(esPropietario, panel), planes: resultadoDe(puedeVerPlanes, planes) });
    });
    return () => { vigente = false; };
  }, [esPropietario, puedeVerPlanes]);

  if (!inicio) {
    return (
      <div>
        <PageHeader title="Inicio" />
        <Cargando texto="Cargando Inicio" />
      </div>
    );
  }

  const { panel, planes } = inicio;
  const tarjetaPlanes = planes && (
    <SeccionSegura titulo="los planes por vencer">
      <PlanesPorVencer resultado={planes} onVerAlumno={onVerAlumno} />
    </SeccionSegura>
  );
  // Sin los indicadores (otro perfil, o fallaron), la tarjeta de planes va sola en la misma grilla
  const planesSolos = tarjetaPlanes && <div className={GRILLA_TARJETAS}>{tarjetaPlanes}</div>;

  return (
    <div className="space-y-6">
      <PageHeader title="Inicio" />
      {panel?.datos && (
        <SeccionSegura titulo="los indicadores del negocio" ademas={planesSolos}>
          <PanelPropietario panel={panel.datos} planesPorVencer={tarjetaPlanes} onVerMora={onVerMora} />
        </SeccionSegura>
      )}
      {panel?.error && (
        <ErrorSeccion titulo="No se pudieron cargar los indicadores del negocio" mensaje={panel.error.message} />
      )}
      {!panel?.datos && planesSolos}
    </div>
  );
}
