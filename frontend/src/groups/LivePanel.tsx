import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useSubjects } from '../api/content'
import { ApiError } from '../api/errors'
import type { Group } from '../api/groups'
import { useGroupLiveGame, useGroupLiveResults, useOpenLiveGame } from '../api/live'
import { button } from '../components/ui/button'

/** La partie live du groupe (ADR 0021) : la rejoindre si elle est ouverte, sinon en lancer une. */
export function LivePanel({ group }: { group: Group }) {
  const live = useGroupLiveGame(group.id)
  const subjects = useSubjects()
  const titleOf = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug

  return (
    <section
      aria-labelledby="live-title"
      className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id="live-title" className="text-lg font-semibold">
        Partie live
      </h2>
      {live.isPending && <p>Chargement…</p>}
      {live.isError && <p role="alert">Impossible de savoir si une partie est ouverte.</p>}
      {live.data && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p>
            Une partie sur <strong>{titleOf(live.data.subject)}</strong> est ouverte (
            {live.data.players.length} joueur{live.data.players.length > 1 ? 's' : ''}).
          </p>
          <Link to={`/live/${live.data.id}`} className={button()}>
            Rejoindre la partie
          </Link>
        </div>
      )}
      {live.data === null && (
        <OpenLiveGameForm groupId={group.id} subjects={group.settings.subjects} titleOf={titleOf} />
      )}
      <LastResults groupId={group.id} titleOf={titleOf} />
    </section>
  )
}

const FINISHED_ON = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })
const PODIUM_SIZE = 3

/** Les dernières parties du groupe et leurs trois premiers : ce qui reste d'une partie live (ADR 0023). */
function LastResults({ groupId, titleOf }: { groupId: string; titleOf: (slug: string) => string }) {
  const results = useGroupLiveResults(groupId)
  if (!results.data || results.data.length === 0) return null
  return (
    <div className="flex flex-col gap-2 border-t border-line pt-3">
      <h3 className="text-sm font-semibold">Dernières parties</h3>
      <ul className="flex flex-col gap-2">
        {results.data.map((result) => (
          <li key={result.gameId} className="rounded-lg bg-surface-muted px-3 py-2 text-sm">
            <span className="font-medium">{titleOf(result.subject)}</span>
            <span className="text-ink-muted">
              {' '}
              · {FINISHED_ON.format(new Date(result.finishedAt))}
            </span>
            <span className="block text-ink-muted">
              {result.standings
                .filter((standing) => standing.rank <= PODIUM_SIZE)
                .map(
                  (standing) => `${standing.rank}. ${standing.displayName} (${standing.score} pts)`,
                )
                .join(' · ')}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function OpenLiveGameForm({
  groupId,
  subjects,
  titleOf,
}: {
  groupId: string
  subjects: string[]
  titleOf: (slug: string) => string
}) {
  const [subject, setSubject] = useState(subjects[0] ?? '')
  const open = useOpenLiveGame(groupId)
  const navigate = useNavigate()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    open.mutate(subject, { onSuccess: (game) => navigate(`/live/${game.id}`) })
  }

  return (
    <form onSubmit={submit} aria-label="Lancer une partie live" className="flex flex-col gap-3">
      <p className="text-sm text-ink-muted">
        Une partie live réunit les membres connectés autour des mêmes QCM, au même moment, avec un
        chrono court. Vous en serez l’hôte.
      </p>
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Sujet</span>
          <select
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
            className="rounded-lg border border-line-strong px-3 py-2"
          >
            {subjects.map((slug) => (
              <option key={slug} value={slug}>
                {titleOf(slug)}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" disabled={open.isPending} className={button()}>
          Lancer une partie live
        </button>
      </div>
      {open.isError && (
        <p role="alert">
          {open.error instanceof ApiError
            ? open.error.message
            : "La partie n'a pas pu être lancée."}
        </p>
      )}
    </form>
  )
}
