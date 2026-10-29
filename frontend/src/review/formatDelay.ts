const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR
const MONTH = 30 * DAY
const YEAR = 365 * DAY

/** Délai court et lisible, affiché sur les boutons de note : « 10 min », « 3 j », « 2 mois »... */
export function formatDelay(milliseconds: number): string {
  const delay = Math.max(0, milliseconds)
  if (delay < HOUR) return `${Math.max(1, Math.round(delay / MINUTE))} min`
  if (delay < DAY) return `${Math.round(delay / HOUR)} h`
  if (delay < MONTH) return `${Math.round(delay / DAY)} j`
  if (delay < YEAR) return `${Math.round(delay / MONTH)} mois`
  const years = Math.round(delay / YEAR)
  return `${years} an${years > 1 ? 's' : ''}`
}
