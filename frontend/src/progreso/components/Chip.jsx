import { cx } from '../../shared/components/clases';

const TONOS = {
  ok: 'bg-prog-ok-bg text-prog-ok',
  neutro: 'bg-prog-neutro-bg text-prog-soft',
};

/** Chip de estado de un plan o una sesión, con ícono opcional. */
export default function Chip({ tono, icono, children }) {
  return (
    <span className={cx('inline-flex shrink-0 items-center gap-1 whitespace-nowrap rounded-full py-1 pr-2.5 text-[12px] font-semibold', icono ? 'pl-2' : 'pl-2.5', TONOS[tono])}>
      {icono}
      {children}
    </span>
  );
}
