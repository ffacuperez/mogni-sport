import { cx } from '../../shared/components/clases';

/** Barra de progreso con un segmento por semana. El texto "Semana X de Y" la acompaña, por eso se oculta a los lectores de pantalla. */
export default function BarraSemanas({ total, completas }) {
  return (
    <div aria-hidden="true" className="flex gap-1">
      {Array.from({ length: total }, (_, i) => (
        <span key={i} className={cx('h-1.5 flex-1 rounded-[3px]', i < completas ? 'bg-primary' : 'bg-prog-line')} />
      ))}
    </div>
  );
}
