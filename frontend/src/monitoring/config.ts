import { metaContent, servedValue } from '../servedConfig'

/** Les balises d'index.html que nginx remplit avec SENTRY_DSN, SENTRY_ENVIRONMENT et SENTRY_RELEASE (ADR 0070). */
export const SENTRY_TAGS = {
  dsn: { name: 'recallio-sentry-dsn', marker: '__SENTRY_DSN__' },
  environment: { name: 'recallio-sentry-environment', marker: '__SENTRY_ENVIRONMENT__' },
  release: { name: 'recallio-sentry-release', marker: '__SENTRY_RELEASE__' },
} as const

/** De quoi envoyer les erreurs à Sentry. */
export interface MonitoringConfig {
  dsn: string
  environment?: string
  release?: string
}

/**
 * La configuration de Sentry que nginx a mise dans index.html ; `undefined` sans DSN : en local (`npm run dev`), en CI,
 * dans le compose, et partout où SENTRY_DSN est vide. Rien n'est alors chargé ni envoyé.
 */
export function monitoringConfig(
  read: (name: string) => string | undefined = metaContent,
): MonitoringConfig | undefined {
  const value = ({ name, marker }: { name: string; marker: string }) =>
    servedValue(read(name), marker)
  const dsn = value(SENTRY_TAGS.dsn)
  if (!dsn) return undefined
  return {
    dsn,
    environment: value(SENTRY_TAGS.environment),
    release: value(SENTRY_TAGS.release),
  }
}
