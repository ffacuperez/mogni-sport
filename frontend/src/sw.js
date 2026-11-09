/// <reference lib="webworker" />
// Service worker de la app (lo compila vite-plugin-pwa). Cachea los archivos del front para que la app
// instalada abra rápido, y recibe las notificaciones push que manda el backend.
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';
import { clientsClaim } from 'workbox-core';

self.skipWaiting();
clientsClaim();

// Solo archivos del front: las llamadas al backend (Railway) siempre van a la red
precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();
// Cualquier URL de la app (p. ej. /mi-cuenta/perfil) se responde con index.html y la resuelve React Router
registerRoute(new NavigationRoute(createHandlerBoundToURL('index.html')));

// El backend manda { titulo, mensaje, url, tag } (ver PushService)
self.addEventListener('push', (event) => {
  let datos = {};
  try {
    datos = event.data?.json() ?? {};
  } catch {
    datos = { mensaje: event.data?.text() };
  }

  event.waitUntil(self.registration.showNotification(datos.titulo || 'Mogni System', {
    body: datos.mensaje || '',
    icon: '/pwa-192x192.png',
    badge: '/pwa-64x64.png',
    lang: 'es',
    // Mismo tag = la notificación nueva reemplaza a la anterior de esa cuota en vez de apilarse
    tag: datos.tag,
    data: { url: datos.url || '/mi-cuenta/notificaciones' },
  }));
});

// Al tocar la notificación: si la app ya está abierta se la trae al frente, si no se abre
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = new URL(event.notification.data?.url || '/', self.location.origin).href;

  event.waitUntil((async () => {
    const ventanas = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const abierta = ventanas.find(v => v.url.startsWith(self.location.origin));
    if (abierta) {
      await abierta.focus();
      return abierta.navigate(url);
    }
    return self.clients.openWindow(url);
  })());
});
