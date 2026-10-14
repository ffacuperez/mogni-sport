import { request } from '../../shared/services/httpClient';
import { esIOS, estaInstalada } from '../../shared/hooks/useInstalarApp';

// Notificaciones push al celular (HU-250). El navegador genera una "suscripción" para este dispositivo y la
// mandamos al backend, que la usa para enviarle avisos aunque la app esté cerrada (ver src/sw.js).
const BASE_PATH = '/notificaciones/push';

export function soportaPush() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
}

/** En iPhone las push solo funcionan con la app instalada ("Agregar a inicio"), no desde Safari. */
export function necesitaInstalarParaPush() {
  return esIOS() && !estaInstalada();
}

export function permisoPush() {
  return 'Notification' in window ? Notification.permission : 'denied';
}

// En `npm run dev` no hay service worker (solo con build + preview o en Vercel)
async function registroServiceWorker() {
  const registro = await navigator.serviceWorker.getRegistration();
  if (!registro) {
    throw new Error('Las notificaciones solo funcionan con la app compilada (npm run build y npm run preview).');
  }
  return navigator.serviceWorker.ready;
}

export async function suscripcionActual() {
  if (!soportaPush()) return null;
  const registro = await navigator.serviceWorker.getRegistration();
  return registro ? registro.pushManager.getSubscription() : null;
}

// La clave VAPID viene en base64url y el navegador la pide en bytes
function aBytes(base64url) {
  const base64 = (base64url + '='.repeat((4 - (base64url.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/');
  return Uint8Array.from(atob(base64), c => c.charCodeAt(0));
}

const mismaClave = (a, b) => a && b && a.byteLength === b.byteLength && new Uint8Array(a).every((v, i) => v === b[i]);

async function guardarEnBackend(suscripcion) {
  const { endpoint, keys } = suscripcion.toJSON();
  await request(`${BASE_PATH}/suscripcion`, { method: 'POST', body: { endpoint, p256dh: keys.p256dh, auth: keys.auth } });
}

/** Pide el permiso (tiene que llamarse desde un click) y registra este dispositivo en el backend. */
export async function activarPush() {
  const registro = await registroServiceWorker();

  const permiso = await Notification.requestPermission();
  if (permiso !== 'granted') {
    throw new Error('No diste permiso para mostrar notificaciones.');
  }

  const { clavePublica } = await request(`${BASE_PATH}/clave-publica`);
  if (!clavePublica) {
    throw new Error('El servidor todavía no tiene configuradas las notificaciones.');
  }
  const clave = aBytes(clavePublica);

  // Si había una suscripción hecha con otra clave del servidor, ya no sirve
  let suscripcion = await registro.pushManager.getSubscription();
  if (suscripcion && !mismaClave(suscripcion.options.applicationServerKey, clave)) {
    await suscripcion.unsubscribe();
    suscripcion = null;
  }
  suscripcion ??= await registro.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: clave });

  await guardarEnBackend(suscripcion);
  return suscripcion;
}

/** Vuelve a mandar la suscripción existente (por si en este celular antes había otra cuenta). */
export async function sincronizarPush() {
  const suscripcion = await suscripcionActual();
  if (suscripcion && permisoPush() === 'granted') await guardarEnBackend(suscripcion);
  return suscripcion;
}

export async function desactivarPush() {
  const suscripcion = await suscripcionActual();
  if (!suscripcion) return;
  try {
    await request(`${BASE_PATH}/suscripcion`, { method: 'DELETE', body: { endpoint: suscripcion.endpoint } });
  } finally {
    await suscripcion.unsubscribe();
  }
}

/** Le pide al backend que mande ya mismo una push a este alumno. Devuelve cuántas salieron. */
export async function enviarPushDePrueba() {
  const { enviadas } = await request(`${BASE_PATH}/prueba`, { method: 'POST' });
  return enviadas;
}
