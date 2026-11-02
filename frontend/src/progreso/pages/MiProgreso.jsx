import { Outlet } from 'react-router';

import ControlSegmentado from '../components/ControlSegmentado';

/** HU-270: título y tabs de Mi progreso; cada tab es una ruta hija. */
export default function MiProgreso() {
  return (
    <div className="-mt-2 flex animate-fade-in flex-col gap-3 leading-[normal]">
      <h1 className="text-[26px] font-bold text-prog-ink">Mi progreso</h1>
      <ControlSegmentado />
      <Outlet />
    </div>
  );
}
