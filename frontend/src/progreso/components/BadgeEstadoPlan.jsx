import { Badge } from '../../shared/components/ui';

export default function BadgeEstadoPlan({ estado }) {
  const activo = estado === 'ACTIVO';
  return <Badge tone={activo ? 'success' : 'neutral'} className="shrink-0">{activo ? 'Activo' : 'Finalizado'}</Badge>;
}
