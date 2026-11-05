import { ShareButton } from '../components/ShareButton'
import type { LiveStanding } from '../api/live'

const RANK_LABELS = ['1er', '2e', '3e']

/**
 * Le classement d'une partie live (ADR 0023). Avec `limit`, les premiers seulement, et soi-même si l'on est plus loin :
 * chacun voit où il en est sans faire défiler.
 */
export function LiveStandings({
  standings,
  me,
  title,
  limit,
}: {
  standings: LiveStanding[]
  me: string | undefined
  title: string
  limit?: number
}) {
  const shown =
    limit === undefined
      ? standings
      : standings.filter((standing, index) => index < limit || standing.userId === me)

  return (
    <section aria-label={title} className="flex flex-col gap-2 rounded-xl bg-white p-4 shadow-sm">
      <h2 className="text-lg font-semibold">{title}</h2>
      <ol className="flex flex-col gap-1">
        {shown.map((standing) => (
          <li
            key={standing.userId}
            className={`flex items-center justify-between gap-3 rounded-lg px-3 py-2 ${
              standing.userId === me ? 'bg-indigo-50 font-medium' : 'bg-slate-50'
            }`}
          >
            <span className="flex items-center gap-3">
              <span className="w-8 text-right text-slate-500">{standing.rank}</span>
              <span>
                {standing.displayName}
                {standing.userId === me && <span className="text-slate-500"> (vous)</span>}
              </span>
            </span>
            <span className="text-sm text-slate-600">{standing.score} pts</span>
          </li>
        ))}
      </ol>
    </section>
  )
}

/** Partager sa place sur le podium d'une partie finie (ADR 0045) ; rien hors du podium. */
export function SharePodium({
  standings,
  me,
}: {
  standings: LiveStanding[]
  me: string | undefined
}) {
  const mine = standings.find(
    (standing) => standing.userId === me && standing.rank <= RANK_LABELS.length,
  )
  if (!mine) return null
  return (
    <ShareButton
      label="Partager mon podium"
      card={{
        kicker: 'Mode live',
        title: `${RANK_LABELS[mine.rank - 1]} place`,
        detail: `${mine.score} pts · ${mine.correctCount} bonne${mine.correctCount > 1 ? 's' : ''} réponse${mine.correctCount > 1 ? 's' : ''}`,
        url: window.location.origin,
      }}
    />
  )
}

/** Les trois premiers d'une partie finie. */
export function LivePodium({
  standings,
  me,
}: {
  standings: LiveStanding[]
  me: string | undefined
}) {
  const podium = standings.filter((standing) => standing.rank <= RANK_LABELS.length)
  return (
    <section aria-label="Podium" className="flex flex-col gap-2">
      <h2 className="text-lg font-semibold">Podium</h2>
      <ol className="grid gap-2 sm:grid-cols-3">
        {podium.map((standing) => (
          <li
            key={standing.userId}
            className="flex flex-col items-center gap-1 rounded-xl border border-amber-200 bg-amber-50 p-3 text-center"
          >
            <span className="text-sm font-semibold text-amber-800">
              {RANK_LABELS[standing.rank - 1]}
            </span>
            <span className="font-medium">
              {standing.displayName}
              {standing.userId === me && ' (vous)'}
            </span>
            <span className="text-sm text-slate-600">
              {standing.score} pts · {standing.correctCount} bonne
              {standing.correctCount > 1 ? 's' : ''} réponse{standing.correctCount > 1 ? 's' : ''}
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
}
