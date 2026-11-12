import { Check, Snowflake } from '@phosphor-icons/react'
import type { DayActivity } from '../api/stats'

const FULL_DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full', timeZone: 'UTC' })
const WEEKDAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D']

const CELL = 'flex aspect-square items-center justify-center rounded-lg text-xs font-bold'
const STYLES = {
  reached: 'bg-success text-on-success',
  frozen: 'bg-info-soft text-info',
  started: 'bg-success-soft text-success',
  empty: 'bg-surface-muted text-ink-faint',
} as const

type State = keyof typeof STYLES

function stateOf({ cardsReviewed, goalReached, frozen }: DayActivity): State {
  if (frozen) return 'frozen'
  if (goalReached) return 'reached'
  return cardsReviewed > 0 ? 'started' : 'empty'
}

/** Une date sans heure : lue en UTC pour ne pas glisser d'un jour selon le fuseau du navigateur. */
const dateOf = (day: string) => new Date(`${day}T00:00:00Z`)

/**
 * Le calendrier d'activité (ADR 0058) : les dernières semaines, une case par jour, rangées du lundi au dimanche. Une
 * coche quand l'objectif a été atteint, un flocon quand un gel de streak a couvert le jour manqué (ADR 0036), le
 * nombre de cartes sinon. Chaque case dit sa date et son état aux lecteurs d'écran.
 */
export function ActivityCalendar({ days }: { days: DayActivity[] }) {
  if (days.length === 0) return null
  // Les cases vides avant le premier jour, pour qu'il tombe sous son jour de la semaine (lundi en tête).
  const offset = (dateOf(days[0].day).getUTCDay() + 6) % 7

  return (
    <section aria-labelledby="calendar-title" className="flex flex-col gap-2">
      <h2 id="calendar-title" className="text-lg font-semibold">
        Tes dernières semaines
      </h2>
      <div
        aria-hidden="true"
        className="grid grid-cols-7 gap-1.5 text-center text-xs text-ink-muted"
      >
        {WEEKDAYS.map((weekday, index) => (
          <span key={index}>{weekday}</span>
        ))}
      </div>
      <ol className="grid grid-cols-7 gap-1.5">
        {Array.from({ length: offset }, (_, index) => (
          <li key={`pad-${index}`} aria-hidden="true" />
        ))}
        {days.map((activity) => {
          const state = stateOf(activity)
          const status = {
            frozen: 'jour manqué couvert par un gel',
            reached: 'objectif atteint',
            started: `${activity.cardsReviewed} carte(s)`,
            empty: `${activity.cardsReviewed} carte(s)`,
          }[state]
          return (
            <li
              key={activity.day}
              aria-label={`${FULL_DATE.format(dateOf(activity.day))} : ${status}`}
              className={`${CELL} ${STYLES[state]}`}
            >
              {state === 'reached' && <Check weight="bold" aria-hidden className="size-4" />}
              {state === 'frozen' && <Snowflake weight="bold" aria-hidden className="size-4" />}
              {state === 'started' && <span aria-hidden="true">{activity.cardsReviewed}</span>}
            </li>
          )
        })}
      </ol>
      <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink-muted">
        <li className="flex items-center gap-1">
          <Check weight="bold" aria-hidden className="size-4 text-success" /> objectif atteint
        </li>
        <li className="flex items-center gap-1">
          <Snowflake weight="bold" aria-hidden className="size-4 text-info" /> jour couvert par un
          gel
        </li>
        <li>un nombre : les cartes d’un jour commencé</li>
      </ul>
    </section>
  )
}
