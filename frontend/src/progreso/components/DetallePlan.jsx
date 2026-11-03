import { useState } from 'react';

import AcordeonSesion from './AcordeonSesion';
import SelectorSemana from './SelectorSemana';
import TarjetaResumenPlan from './TarjetaResumenPlan';

/** Resumen del plan, selector de semanas y sesiones de la semana elegida. Arranca en la semana que indica el backend. */
export default function DetallePlan({ plan }) {
  const [numeroSemana, setNumeroSemana] = useState(plan.semanaActual);
  const semana = plan.semanas[numeroSemana - 1];

  return (
    <div className="flex flex-col gap-3">
      <TarjetaResumenPlan plan={plan} />
      <SelectorSemana semana={semana} cantidadSemanas={plan.cantidadSemanas} onCambiar={setNumeroSemana} />
      {/* La key incluye la semana: al cambiar de semana las sesiones vuelven a aparecer contraídas */}
      {semana.sesiones.map((sesion) => (
        <AcordeonSesion
          key={`${semana.numero}-${sesion.numero}`}
          sesion={sesion}
          planId={plan.id}
          semanaNumero={semana.numero}
        />
      ))}
    </div>
  );
}
