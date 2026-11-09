import { useEffect, useState } from 'react';

// Mismo límite que el xl de Tailwind (1280 px): el menú lateral pasa arriba junto con las tarjetas de la lista
const CONSULTA = '(max-width: 1279px)';

/** true en pantallas chicas o ventanas angostas. Al cruzar el límite cambia sola, sin recargar. */
export function usePantallaChica() {
  const [pantallaChica, setPantallaChica] = useState(() => window.matchMedia(CONSULTA).matches);

  useEffect(() => {
    const media = window.matchMedia(CONSULTA);
    const alCambiar = (evento) => setPantallaChica(evento.matches);
    media.addEventListener('change', alCambiar);
    return () => media.removeEventListener('change', alCambiar);
  }, []);

  return pantallaChica;
}
