/** Les durées proposées pour une session, en minutes (ADR 0048). */
export const SESSION_MINUTES = [5, 10, 15, 20] as const

/** Une durée limitée, en minutes. */
export type TimedMinutes = (typeof SESSION_MINUTES)[number]

/** La durée d'une session, en minutes ; `null` : sans limite. */
export type SessionMinutes = TimedMinutes | null

export type SessionSettings = { subject: string; minutes: SessionMinutes }

export const DEFAULT_MINUTES: TimedMinutes = 10

const STORAGE_KEY = 'recallio.review-session'

const isMinutes = (value: unknown): value is SessionMinutes =>
  value === null || SESSION_MINUTES.some((minutes) => minutes === value)

/**
 * Les derniers réglages choisis sur cet appareil, pour lancer la session suivante d'un geste. Rien si le stockage est
 * vide, illisible ou indisponible (navigation privée, par exemple) : les valeurs par défaut prennent le relais. Le mode
 * « questions corrigées » qu'une version précédente y rangeait est ignoré : le serveur corrige toutes les cartes qu'on
 * révise (ADR 0063).
 */
export function loadSessionSettings(): Partial<SessionSettings> {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
    if (typeof stored !== 'object' || stored === null) return {}
    const settings: Partial<SessionSettings> = {}
    if ('subject' in stored && typeof stored.subject === 'string') settings.subject = stored.subject
    if ('minutes' in stored && isMinutes(stored.minutes)) settings.minutes = stored.minutes
    return settings
  } catch {
    return {}
  }
}

export function saveSessionSettings(settings: SessionSettings) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
  } catch {
    // Stockage indisponible : la prochaine session repartira des valeurs par défaut.
  }
}
