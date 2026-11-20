/** Un entretien se prévoit au plus un an à l'avance : le serveur refuse au-delà (ADR 0066). */
export const INTERVIEW_MAX_DAYS_AHEAD = 365

const twoDigits = (value: number) => String(value).padStart(2, '0')

/** Le jour qui suit aujourd'hui de `days` jours, dans le fuseau de l'appareil, au format d'un champ date. */
export function dayFromToday(days: number, today: Date = new Date()): string {
  const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() + days)
  return `${day.getFullYear()}-${twoDigits(day.getMonth() + 1)}-${twoDigits(day.getDate())}`
}

/** Les bornes d'un champ date d'entretien : de demain à dans un an. */
export function interviewDateBounds(today: Date = new Date()) {
  return { min: dayFromToday(1, today), max: dayFromToday(INTERVIEW_MAX_DAYS_AHEAD, today) }
}

// Des jours calendaires : formatés en UTC, ils ne glissent pas d'un jour selon le fuseau du navigateur.
const FULL_DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full', timeZone: 'UTC' })
const DAY = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', timeZone: 'UTC' })
const dateOf = (isoDay: string) => new Date(`${isoDay}T00:00:00Z`)

/** « mardi 15 décembre 2026 ». */
export const fullDate = (isoDay: string) => FULL_DATE.format(dateOf(isoDay))

/** « 15 décembre ». */
export const shortDate = (isoDay: string) => DAY.format(dateOf(isoDay))
