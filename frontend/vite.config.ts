import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { defineConfig } from 'vitest/config'

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Recallio',
        short_name: 'Recallio',
        description: 'Révisez vos entretiens techniques chaque jour, par répétition espacée.',
        lang: 'fr',
        theme_color: '#4f46e5',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [{ src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
      workbox: {
        // Les appels API ne sont jamais servis depuis le cache du service worker.
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  server: {
    // En dev, le front appelle /api sur son propre origin : pas de CORS à configurer.
    proxy: { '/api': BACKEND_URL },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    unstubGlobals: true,
  },
})
