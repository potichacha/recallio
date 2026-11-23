import { captureException, init } from '@sentry/browser'
import type { MonitoringConfig } from './config'
import { scrubBreadcrumb, scrubEvent } from './scrub'

/**
 * Ce que Sentry ferait par défaut et que la PWA ne fait pas (ADR 0070) : une session à chaque visite, qui joindrait
 * Sentry même sans erreur, et des fils d'Ariane tirés de la console.
 */
const LEFT_OUT_INTEGRATIONS = new Set(['BrowserSession', 'Console'])

/**
 * Démarre Sentry et rend de quoi lui signaler une erreur. Ce module, et `@sentry/browser` avec lui, n'est téléchargé
 * que si un DSN est donné (`monitoring.ts`) : il ne pèse rien à l'ouverture de la PWA. Son nom compte : le service
 * worker ne met pas en cache le morceau qui en sort, `assets/sentry-*.js` (vite.config.ts).
 *
 * Sans donnée personnelle : ni utilisateur, ni adresse IP déduite, ni cookies, ni chaîne de requête, ni corps de
 * requête ; ni Session Replay ni traces de performance ; chaque erreur et chaque fil d'Ariane passent par
 * `scrub.ts`.
 */
export function startSentry(config: MonitoringConfig): (error: unknown) => void {
  init({
    dsn: config.dsn,
    environment: config.environment,
    release: config.release,
    integrations: (defaults) =>
      defaults.filter((integration) => !LEFT_OUT_INTEGRATIONS.has(integration.name)),
    // L'équivalent de sendDefaultPii: false depuis la version 11 : userInfo: false dit aussi à Sentry de ne pas déduire
    // l'adresse IP (infer_ip: never).
    dataCollection: {
      userInfo: false,
      cookies: false,
      urlQueryParams: false,
      httpBodies: [],
      stackFrameVariables: false,
    },
    // Rien ne part sans erreur, pas même le compte des événements écartés.
    sendClientReports: false,
    // Le message des erreurs de fetch reste celui du navigateur ; seul celui envoyé à Sentry dit l'hôte.
    enhanceFetchErrorMessages: 'report-only',
    beforeSend: scrubEvent,
    beforeBreadcrumb: scrubBreadcrumb,
  })
  return (error) => {
    captureException(error)
  }
}
