import { useState, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useSubjects } from '../api/content'
import { ApiError } from '../api/errors'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useReviewQueue, type ReviewFilter, type ReviewQueue } from '../api/review'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { DailyGoal } from '../components/DailyGoal'
import { formatDelay } from './formatDelay'
import { ReviewCard } from './ReviewCard'

/**
 * Page de révision. Sans filtre, elle puise dans les sujets suivis (il faut donc en suivre au moins un) ; un sujet
 * (`?subject=go`) ou un thème (`?theme=...`) se révise même s'il n'est pas suivi.
 */
export function ReviewPage() {
  const [searchParams] = useSearchParams()
  const filter: ReviewFilter = {
    subject: searchParams.get('subject') ?? undefined,
    theme: searchParams.get('theme') ?? undefined,
  }
  // Changer de filtre démarre une nouvelle session : la position et le compteur repartent de zéro.
  const session = <ReviewSession key={`${filter.subject}|${filter.theme}`} filter={filter} />

  if (filter.theme) return session
  // Les onglets restent en place d'un sujet à l'autre : seule la session change.
  return (
    <SubjectTabs current={filter.subject}>
      {filter.subject ? session : <RequireFollowedSubjects>{session}</RequireFollowedSubjects>}
    </SubjectTabs>
  )
}

/** Le choix du sujet révisé (tous les sujets suivis, ou l'un d'eux), dès qu'il y a plus d'un sujet. */
function SubjectTabs({ current, children }: { current?: string; children: ReactNode }) {
  const profile = useProfile()
  const followed = profile.data?.followedSubjects ?? []
  // Un sujet non suivi, ouvert depuis le catalogue, apparaît aussi : on voit ce qu'on révise.
  const tabs = current && !followed.includes(current) ? [...followed, current] : followed

  return (
    <div className="flex flex-col gap-4">
      {tabs.length > 1 && <SubjectTabsNav tabs={tabs} current={current} />}
      {children}
    </div>
  )
}

function SubjectTabsNav({ tabs, current }: { tabs: string[]; current?: string }) {
  const subjects = useSubjects()
  const titleOf = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug
  const tabClass = (active: boolean) =>
    active
      ? 'rounded-full bg-indigo-600 px-3 py-1 text-sm font-medium text-white'
      : 'rounded-full border border-slate-200 bg-white px-3 py-1 text-sm text-slate-700 hover:border-indigo-300'

  return (
    <nav aria-label="Sujet révisé" className="flex flex-wrap gap-2">
      <Link
        to="/review"
        aria-current={!current ? 'page' : undefined}
        className={tabClass(!current)}
      >
        Tous mes sujets
      </Link>
      {tabs.map((slug) => (
        <Link
          key={slug}
          to={`/review?subject=${encodeURIComponent(slug)}`}
          aria-current={current === slug ? 'page' : undefined}
          className={tabClass(current === slug)}
        >
          {titleOf(slug)}
        </Link>
      ))}
    </nav>
  )
}

/**
 * Session de révision : on parcourt la file reçue carte par carte, puis on en demande une nouvelle. Les cartes notées
 * « À revoir » reviennent quelques minutes plus tard, dans une file suivante.
 */
function ReviewSession({ filter }: { filter: ReviewFilter }) {
  const queue = useReviewQueue(filter)
  const progress = useProgress()
  const [position, setPosition] = useState(0)
  const [reviewed, setReviewed] = useState(0)

  if (queue.isPending) {
    return <p>Préparation de la session…</p>
  }
  if (queue.isError) {
    const notFound = queue.error instanceof ApiError && queue.error.status === 404
    return (
      <p role="alert">
        {notFound
          ? `Ce ${filter.theme ? 'thème' : 'sujet'} n’existe pas.`
          : 'Impossible de charger les cartes à réviser.'}
      </p>
    )
  }

  const card = queue.data.cards.at(position)
  const continueSession = async () => {
    await queue.refetch()
    setPosition(0)
  }

  return (
    <section className="flex flex-col gap-4">
      <header className="flex items-baseline justify-between gap-2">
        <h1 className="text-2xl font-bold">Réviser</h1>
        <p className="text-sm text-slate-500">
          {reviewed} carte{reviewed > 1 ? 's' : ''} révisée{reviewed > 1 ? 's' : ''}
        </p>
      </header>
      {progress.data && <DailyGoal progress={progress.data} />}
      {card ? (
        <ReviewCard
          key={card.cardId}
          card={card}
          onReviewed={() => {
            setReviewed((count) => count + 1)
            setPosition((current) => current + 1)
          }}
        />
      ) : (
        <SessionPause
          queue={queue.data}
          batchFinished={position > 0}
          loading={queue.isFetching}
          onContinue={continueSession}
        />
      )}
    </section>
  )
}

function SessionPause({
  queue,
  batchFinished,
  loading,
  onContinue,
}: {
  queue: ReviewQueue
  batchFinished: boolean
  loading: boolean
  onContinue: () => void
}) {
  // Instant figé à l'affichage : le rendu reste pur (le délai n'a pas besoin d'être rafraîchi en continu).
  const [now] = useState(() => Date.now())
  if (batchFinished) {
    return (
      <div className="flex flex-col items-start gap-3 rounded-xl bg-white p-5 shadow-sm">
        <p className="font-medium">Série terminée, bravo !</p>
        <button
          type="button"
          onClick={onContinue}
          disabled={loading}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Continuer
        </button>
      </div>
    )
  }
  return (
    <div className="flex flex-col items-start gap-3 rounded-xl bg-white p-5 shadow-sm">
      <p className="font-medium">Tout est à jour pour le moment.</p>
      {queue.nextDueAt && (
        <p className="text-slate-600">
          Prochaine carte à revoir dans {formatDelay(Date.parse(queue.nextDueAt) - now)}.
        </p>
      )}
      <Link to="/subjects" className="text-indigo-600 hover:underline">
        Parcourir les sujets
      </Link>
    </div>
  )
}
