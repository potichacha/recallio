import type { CardType } from '../api/content'
import { SERVER_GRADED_TYPES } from '../components/cardTypeLabels'

/** Les durées proposées pour une session, en minutes (ADR 0048). */
export const SESSION_MINUTES = [5, 10, 15, 20] as const

/** La durée d'une session, en minutes ; `null` : sans limite. */
export type SessionMinutes = (typeof SESSION_MINUTES)[number] | null

/** Toutes les questions, ou seulement celles que le serveur corrige (ADR 0048). */
export type SessionMode = 'all' | 'graded'

export type SessionSettings = { subject: string; minutes: SessionMinutes; mode: SessionMode }

export const DEFAULT_MINUTES: SessionMinutes = 10
export const DEFAULT_MODE: SessionMode = 'all'

const STORAGE_KEY = 'recallio.review-session'

const isMinutes = (value: unknown): value is SessionMinutes =>
  value === null || SESSION_MINUTES.some((minutes) => minutes === value)

const isMode = (value: unknown): value is SessionMode => value === 'all' || value === 'graded'

/**
 * Les derniers réglages choisis sur cet appareil, pour lancer la session suivante d'un geste. Rien si le stockage est
 * vide, illisible ou indisponible (navigation privée, par exemple) : les valeurs par défaut prennent le relais.
 */
export function loadSessionSettings(): Partial<SessionSettings> {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
    if (typeof stored !== 'object' || stored === null) return {}
    const settings: Partial<SessionSettings> = {}
    if ('subject' in stored && typeof stored.subject === 'string') settings.subject = stored.subject
    if ('minutes' in stored && isMinutes(stored.minutes)) settings.minutes = stored.minutes
    if ('mode' in stored && isMode(stored.mode)) settings.mode = stored.mode
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

/** Les formats révisés dans ce mode ; aucun : tous les formats. */
export function sessionTypes(mode: SessionMode): CardType[] | undefined {
  return mode === 'graded' ? [...SERVER_GRADED_TYPES] : undefined
}
