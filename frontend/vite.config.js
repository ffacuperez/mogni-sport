import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  // `npm run preview` (para probar la app instalada y las push) usa el mismo puerto que dev,
  // que es uno de los orígenes que el CORS del backend acepta
  preview: { port: 5173, strictPort: true },
  plugins: [
    react(),
    tailwindcss(),
    // App instalable (PWA). Los íconos se generan con `npm run generar-iconos` (ver pwa-assets.config.js).
    // injectManifest: el service worker es nuestro (src/sw.js) porque además de cachear recibe las push.
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.js',
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      includeAssets: ['favicon.svg', 'favicon.ico', 'apple-touch-icon-180x180.png'],
      manifest: {
        name: 'Mogni Sport',
        short_name: 'Mogni Sport',
        description: 'Tu cuenta del gimnasio Mogni Sport',
        lang: 'es',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'portrait',
        theme_color: '#2c2c2c',
        background_color: '#2c2c2c',
        icons: [
          { src: 'pwa-64x64.png', sizes: '64x64', type: 'image/png' },
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          { src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
      },
    }),
  ],
})
