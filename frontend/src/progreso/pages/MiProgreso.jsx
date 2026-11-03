import { Outlet } from 'react-router';

import ControlSegmentado from '../components/ControlSegmentado';

/** HU-270: título y tabs de Mi progreso; cada tab es una ruta hija. */
export default function MiProgreso() {
  return (
    <div className="animate-fade-in">
      <h1 className="mb-5 text-2xl font-bold text-ink">Mi rutina</h1>
      <ControlSegmentado />
      <Outlet />
    </div>
  );
}
