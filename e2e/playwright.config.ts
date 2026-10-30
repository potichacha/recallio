import { defineConfig, devices } from '@playwright/test'

/**
 * Parcours de bout en bout contre la pile « comme en production » : nginx (PWA), backend, PostgreSQL et Keycloak,
 * lancés par infra/compose.yaml avec infra/compose.e2e.yaml (voir e2e/README.md).
 */
export default defineConfig({
  testDir: './tests',
  // Les parcours partagent une base : ils s'exécutent l'un après l'autre, chacun avec son utilisateur.
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    // La PWA vise d'abord le mobile : le parcours de révision est aussi joué sur un écran de téléphone.
    { name: 'mobile', use: { ...devices['Pixel 7'] }, grep: /@mobile/ },
  ],
})
