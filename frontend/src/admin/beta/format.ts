// Un jour du rapport est un jour calendaire : formaté en UTC, il ne glisse pas d'un jour selon le fuseau de l'admin.
const DAY = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', timeZone: 'UTC' })

/** « 2026-11-25 » devient « 25 nov. ». */
export const formatDay = (isoDate: string) => DAY.format(new Date(`${isoDate}T00:00:00Z`))

export const formatNumber = (value: number) => value.toLocaleString('fr-FR')

/** « 1 compte », « 3 comptes » : le pluriel régulier d'un mot. */
export const plural = (count: number, word: string) =>
  `${formatNumber(count)} ${word}${count > 1 ? 's' : ''}`

/** Une part absente (rien à diviser) s'écrit d'un tiret, jamais « 0 % ». */
export const formatPercent = (percent: number | null | undefined) =>
  typeof percent === 'number' ? `${percent} %` : '–'
