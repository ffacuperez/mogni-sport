import { useState } from 'react';

import AcordeonSesion from './AcordeonSesion';
import SelectorSemana from './SelectorSemana';
import TarjetaResumenPlan from './TarjetaResumenPlan';

/**
 * Resumen del plan, selector de semanas y sesiones de la semana elegida. Arranca en la semana que indica el backend.
 * HU-269: cuando se registra algo, la sesión se reemplaza por la que devuelve el backend, sin volver a pedir el plan.
 */
export default function DetallePlan({ plan: planInicial }) {
  const [plan, setPlan] = useState(planInicial);
  const [numeroSemana, setNumeroSemana] = useState(plan.semanaActual);
  const semana = plan.semanas[numeroSemana - 1];

  const actualizarSesion = (numero, nueva) => {
    setPlan((actual) => {
      const anterior = actual.semanas[numero - 1].sesiones.find((s) => s.numero === nueva.numero);
      const recienFinalizada = anterior.estado !== 'REGISTRADA' && nueva.estado === 'REGISTRADA';
      return {
        ...actual,
        sesionesRegistradas: actual.sesionesRegistradas + (recienFinalizada ? 1 : 0),
        semanas: actual.semanas.map((s) => (s.numero !== numero ? s : {
          ...s,
          sesiones: s.sesiones.map((sesion) => (sesion.numero === nueva.numero ? nueva : sesion)),
        })),
      };
    });
  };

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
          semana={semana.numero}
          onActualizada={(nueva) => actualizarSesion(semana.numero, nueva)}
        />
      ))}
    </div>
  );
}
