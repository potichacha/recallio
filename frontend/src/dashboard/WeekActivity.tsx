import type { DayActivity } from '../api/stats'

const WEEKDAY = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', timeZone: 'UTC' })
const FULL_DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full', timeZone: 'UTC' })

/**
 * Les derniers jours : une pastille pleine quand l'objectif a été atteint, un flocon quand un gel de streak a couvert
 * le jour manqué (ADR 0036).
 */
export function WeekActivity({ days }: { days: DayActivity[] }) {
  return (
    <section aria-labelledby="week-title" className="flex flex-col gap-2">
      <h2 id="week-title" className="text-lg font-semibold">
        Derniers jours
      </h2>
      <ol className="grid grid-cols-7 gap-2">
        {days.map(({ day, cardsReviewed, goalReached, frozen }) => {
          // Une date sans heure : lue en UTC pour ne pas glisser d'un jour selon le fuseau du navigateur.
          const date = new Date(`${day}T00:00:00Z`)
          const status = frozen
            ? 'jour manqué couvert par un gel'
            : goalReached
              ? 'objectif atteint'
              : `${cardsReviewed} carte(s)`
          return (
            <li
              key={day}
              aria-label={`${FULL_DATE.format(date)} : ${status}`}
              className="flex flex-col items-center gap-1 text-xs text-slate-500"
            >
              <span
                aria-hidden="true"
                className={`flex size-9 items-center justify-center rounded-full font-medium ${
                  frozen
                    ? 'bg-sky-100'
                    : goalReached
                      ? 'bg-emerald-500 text-white'
                      : cardsReviewed > 0
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-slate-100 text-slate-400'
                }`}
              >
                {frozen ? '❄️' : cardsReviewed}
              </span>
              <span aria-hidden="true">{WEEKDAY.format(date)}</span>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
