import { useEffect, useRef, useState } from 'react';

/**
 * Estado del toast. Cada aviso nuevo reinicia el temporizador, así un aviso anterior
 * no cierra antes de tiempo al siguiente.
 */
export function useToast(duracion = 4000) {
  const [toast, setToast] = useState(null);
  const timer = useRef(null);

  useEffect(() => () => clearTimeout(timer.current), []);

  const mostrar = (message, type = 'success') => {
    clearTimeout(timer.current);
    setToast({ message, type, key: Date.now() });
    timer.current = setTimeout(() => setToast(null), duracion);
  };

  return [toast, mostrar];
}
