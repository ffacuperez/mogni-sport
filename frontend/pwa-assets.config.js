import { defineConfig } from '@vite-pwa/assets-generator/config'

// Genera los íconos de la app instalable (public/pwa-*.png, maskable, apple-touch) desde public/pwa-icon.svg.
// Se corre a mano con `npm run generar-iconos` cuando cambia el logo.
export default defineConfig({
  headLinkOptions: { preset: '2023' },
  preset: {
    transparent: { sizes: [64, 192, 512], favicons: [[48, 'favicon.ico']], padding: 0 },
    maskable: { sizes: [512], padding: 0.3, resizeOptions: { background: '#d32f2f' } },
    apple: { sizes: [180], padding: 0.3, resizeOptions: { background: '#d32f2f' } },
  },
  images: ['public/pwa-icon.svg'],
})
