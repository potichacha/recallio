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
        theme_color: '#0e6b63',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [{ src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
      workbox: {
        // Les polices sont hébergées avec l'application (ADR 0049) : elles servent aussi hors ligne.
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        // Les appels API ne sont jamais servis depuis le cache du service worker.
        navigateFallbackDenylist: [/^\/api\//],
        // Les rappels (ADR 0047) : la réception d'une notification et le clic dessus.
        importScripts: ['push-sw.js'],
      },
    }),
  ],
  server: {
    // En dev, le front appelle /api sur son propre origin : pas de CORS à configurer.
    // `ws` : le mode live passe par un WebSocket sous /api (ADR 0021).
    proxy: { '/api': { target: BACKEND_URL, ws: true } },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    unstubGlobals: true,
  },
})
