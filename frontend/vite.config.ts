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
        description: 'Révise tes entretiens techniques chaque jour, par répétition espacée.',
        lang: 'fr',
        theme_color: '#5b3df5',
        background_color: '#ffffff',
        display: 'standalone',
        // Des PNG pour l'écran d'accueil d'Android, dont un « maskable » que le système découpe à sa forme ; le SVG
        // pour qui sait l'utiliser. Refaits par e2e/scripts/generate-pwa-icons.mjs.
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          {
            src: 'icon-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
          { src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
        ],
      },
      workbox: {
        // Les polices sont hébergées avec l'application (ADR 0049) : elles servent aussi hors ligne.
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        // Sauf Sentry (src/monitoring/sentry.ts), que la PWA ne télécharge que si nginx lui donne un DSN (ADR 0070) :
        // sans DSN, rien de Sentry n'est téléchargé, pas même pour le hors-ligne. Les source maps (.map) n'y sont pas.
        globIgnores: ['**/node_modules/**/*', 'assets/sentry-*.js'],
        // Les appels API ne sont jamais servis depuis le cache du service worker, ni robots.txt et le plan du site
        // (ADR 0065) : le service worker servirait la PWA à leur place.
        navigateFallbackDenylist: [/^\/api\//, /^\/robots\.txt$/, /^\/sitemap\.xml$/],
        // Les rappels (ADR 0047) : la réception d'une notification et le clic dessus.
        importScripts: ['push-sw.js'],
      },
    }),
  ],
  build: {
    // Les source maps, pour que Sentry lise les fichiers source d'une erreur (ADR 0070) : il les télécharge du site,
    // à côté de chaque fichier. Le dépôt est public, les servir ne révèle rien ; le service worker ne les met pas en
    // cache, et le budget de l'ouverture ne les compte pas.
    sourcemap: true,
  },
  server: {
    // En dev, le front appelle /api sur son propre origin : pas de CORS à configurer.
    // `ws` : le mode live passe par un WebSocket sous /api (ADR 0021). Le plan du site vient du backend, comme
    // derrière nginx (ADR 0065).
    proxy: {
      '/api': { target: BACKEND_URL, ws: true },
      '/sitemap.xml': { target: BACKEND_URL, rewrite: () => '/api/v1/sitemap.xml' },
    },
  },
  test: {
    environment: 'jsdom',
    // Au-dessus de l'attente des pages chargées à la demande (src/test/setup.ts).
    testTimeout: 20_000,
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    unstubGlobals: true,
  },
})
