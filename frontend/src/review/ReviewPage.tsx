import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useProgress } from '../api/progress'
import { useReviewQueue, type ReviewQueue } from '../api/review'
import { DailyGoal } from './DailyGoal'
import { formatDelay } from './formatDelay'
import { ReviewCard } from './ReviewCard'

/**
 * Session de révision : on parcourt la file reçue carte par carte, puis on en demande une nouvelle. Les cartes notées
 * « À revoir » reviennent quelques minutes plus tard, dans une file suivante.
 */
export function ReviewPage() {
  const [searchParams] = useSearchParams()
  const theme = searchParams.get('theme') ?? undefined
  const queue = useReviewQueue({ theme })
  const progress = useProgress()
  const [position, setPosition] = useState(0)
  const [reviewed, setReviewed] = useState(0)

  if (queue.isPending) {
    return <p>Préparation de la session…</p>
  }
  if (queue.isError) {
    const unknownTheme = queue.error instanceof ApiError && queue.error.status === 404
    return (
      <p role="alert">
        {unknownTheme ? 'Ce thème n’existe pas.' : 'Impossible de charger les cartes à réviser.'}
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
      <Link to="/themes" className="text-indigo-600 hover:underline">
        Parcourir les thèmes
      </Link>
    </div>
  )
}
