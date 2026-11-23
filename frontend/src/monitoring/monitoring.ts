import { monitoringConfig, type MonitoringConfig } from './config'

/** Signale une erreur à Sentry. */
type Capture = (error: unknown) => void

/** Télécharge Sentry et le démarre : le morceau à part `sentry.ts`, avec `@sentry/browser`. */
export type LoadSentry = () => Promise<(config: MonitoringConfig) => Capture>

const loadSentry: LoadSentry = () => import('./sentry').then((module) => module.startSentry)

/**
 * Le suivi des erreurs de la PWA (ADR 0070). Sans DSN, il ne télécharge rien et n'envoie rien. Avec un DSN, il
 * télécharge Sentry dès le démarrage, hors du JavaScript de l'ouverture ; les erreurs signalées entre-temps attendent la
 * fin du téléchargement, puis partent. Les erreurs que Sentry attrape seul (celles qu'aucun code n'attrape) ne sont
 * vues qu'une fois Sentry démarré : celles d'avant sont perdues, c'est accepté.
 */
export function createMonitoring(load: LoadSentry = loadSentry) {
  let state: 'off' | 'loading' | 'on' = 'off'
  let capture: Capture = () => {}
  let waiting: unknown[] = []
  // Une même erreur n'est signalée qu'une fois, même si la page qui l'affiche est rendue deux fois.
  const reported = new WeakSet<object>()

  return {
    async start(config: MonitoringConfig | undefined): Promise<void> {
      if (!config || state !== 'off') return
      state = 'loading'
      try {
        capture = (await load())(config)
        state = 'on'
        waiting.forEach(capture)
      } catch {
        // Le morceau ne s'est pas téléchargé (hors ligne, nouvelle version) : la PWA continue, sans suivi.
        state = 'off'
      }
      waiting = []
    },

    report(error: unknown): void {
      if (typeof error === 'object' && error !== null) {
        if (reported.has(error)) return
        reported.add(error)
      }
      if (state === 'on') capture(error)
      else if (state === 'loading') waiting.push(error)
    },
  }
}

const monitoring = createMonitoring()

/** Démarre le suivi des erreurs avec la configuration que nginx a mise dans index.html. */
export function startMonitoring(): Promise<void> {
  return monitoring.start(monitoringConfig())
}

/**
 * Signale à Sentry une erreur attrapée par la PWA (celle d'une page, par exemple), s'il est allumé ; sans DSN, ne fait
 * rien. Un autre nom que `reportError`, une fonction du navigateur.
 */
export function captureError(error: unknown): void {
  monitoring.report(error)
}
