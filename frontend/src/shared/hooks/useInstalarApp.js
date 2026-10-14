import { useEffect, useState } from 'react';

// Chrome/Edge/Android disparan beforeinstallprompt una sola vez, apenas carga la página (a veces antes de
// que se monte el componente). Por eso se escucha acá, a nivel de módulo, y se guarda para usarlo después.
let eventoGuardado = null;
const suscriptores = new Set();
const avisar = () => suscriptores.forEach(fn => fn());

if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault(); // sin esto el navegador muestra su propio aviso y no se puede volver a usar
    eventoGuardado = e;
    avisar();
  });
  window.addEventListener('appinstalled', () => {
    eventoGuardado = null;
    avisar();
  });
}

/** La app está abierta como app instalada (no en una pestaña del navegador). */
export const estaInstalada = () =>
  window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;

// iPhone/iPad no tienen beforeinstallprompt: la única forma es Compartir → "Agregar a inicio"
export const esIOS = () =>
  /iphone|ipad|ipod/i.test(navigator.userAgent)
  || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1); // iPadOS se presenta como Mac

/**
 * Estado de la instalación de la app (PWA).
 * modo: 'nativo' (el navegador abre su diálogo), 'ios' (hay que mostrar instrucciones) o null (no se puede
 * o ya está instalada: el botón no se muestra).
 */
export function useInstalarApp() {
  const [, forzarRender] = useState(0);

  useEffect(() => {
    const fn = () => forzarRender(n => n + 1);
    suscriptores.add(fn);
    return () => suscriptores.delete(fn);
  }, []);

  let modo = null;
  if (!estaInstalada()) {
    if (eventoGuardado) modo = 'nativo';
    else if (esIOS()) modo = 'ios';
  }

  const instalar = async () => {
    if (!eventoGuardado) return;
    const evento = eventoGuardado;
    evento.prompt();
    const { outcome } = await evento.userChoice;
    // El evento sirve una sola vez; si la rechaza, Chrome lo vuelve a disparar más adelante
    if (outcome === 'accepted') eventoGuardado = null;
    avisar();
  };

  return { modo, instalar };
}
