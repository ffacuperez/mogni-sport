import { Activity, Check, Clock } from 'lucide-react';

/** Cómo se muestra cada estado de una sesión del plan: tono del Badge, texto e ícono. */
export const ESTADOS = {
  REGISTRADA: { tono: 'success', texto: 'Registrada', icono: Check },
  EN_CURSO: { tono: 'warning', texto: 'En curso', icono: Activity },
  PENDIENTE: { tono: 'neutral', texto: 'Pendiente', icono: Clock },
  NO_REGISTRADA: { tono: 'neutral', texto: 'No registrada' },
};
