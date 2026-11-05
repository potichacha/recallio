import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useProgress } from '../api/progress'
import { useReviewQueue, type ReviewFilter, type ReviewResult } from '../api/review'
import { DailyGoal } from '../components/DailyGoal'
import { ReportButton } from '../components/ReportButton'
import { formatDelay } from './formatDelay'
import { ReviewCard } from './ReviewCard'
import type { SessionMinutes } from './sessionSettings'
import { button } from '../components/ui/button'

const MINUTE_MS = 60_000
const CLOCK_TICK_MS = 1_000

const PRIMARY = button()
const SECONDARY = button({ variant: 'secondary' })

/** Pourquoi une session s'arrête : le temps est écoulé, il ne reste rien à réviser, ou on l'a terminée. */
type SessionEnd = 'time' | 'done' | 'stopped'

type Tally = { reviewed: number; recalled: number }

const cardsReviewed = (count: number) =>
  `${count} carte${count > 1 ? 's' : ''} révisée${count > 1 ? 's' : ''}`

/**
 * Une session de révision : on parcourt la file carte par carte, et la série suivante est demandée dès que la
 * précédente est finie. Elle s'arrête quand sa durée est écoulée, toujours après la carte en cours (ADR 0048), quand il
 * ne reste rien à réviser, ou quand on la termine ; un bilan s'affiche alors.
 *
 * @param subjectTitle le sujet révisé, pour dire qu'il est à jour ; aucun pour un groupe ou un thème
 * @param onChangeSettings revenir au lancement d'une session ; aucun pour une session ouverte par un lien direct
 */
export function ReviewSession({
  title,
  filter,
  minutes,
  subjectTitle,
  onRestart,
  onChangeSettings,
}: {
  title: ReactNode
  filter: ReviewFilter
  minutes: SessionMinutes
  subjectTitle?: string
  onRestart: () => void
  onChangeSettings?: () => void
}) {
  const queue = useReviewQueue(filter)
  const progress = useProgress()
  const [endsAt] = useState(() => (minutes === null ? null : Date.now() + minutes * MINUTE_MS))
  const [position, setPosition] = useState(0)
  // Le numéro de la série : une carte revenue dans la série suivante (notée « Je ne savais pas ») repart de son recto.
  const [batch, setBatch] = useState(0)
  const [tally, setTally] = useState<Tally>({ reviewed: 0, recalled: 0 })
  const [stopped, setStopped] = useState<SessionEnd>()
  const [fetchingNext, setFetchingNext] = useState(false)

  if (queue.isPending) {
    return <p>Préparation de la session…</p>
  }
  if (queue.isError) {
    const notFound = queue.error instanceof ApiError && queue.error.status === 404
    const missing = filter.group ? 'groupe' : filter.theme ? 'thème' : 'sujet'
    return (
      <p role="alert">
        {notFound
          ? `Ce ${missing} est introuvable.`
          : 'Impossible de charger les cartes à réviser.'}
      </p>
    )
  }

  const cards = queue.data.cards
  const card = cards.at(position)
  const end = stopped ?? (card || fetchingNext ? undefined : 'done')

  const reviewed = (result: ReviewResult) => {
    setTally((current) => ({
      reviewed: current.reviewed + 1,
      recalled: current.recalled + (result.rating === 'AGAIN' ? 0 : 1),
    }))
    if (endsAt !== null && Date.now() >= endsAt) {
      setStopped('time')
      return
    }
    setPosition(position + 1)
    if (position + 1 < cards.length) return
    // La série est finie : la suivante est demandée tout de suite, sans attendre un clic.
    setFetchingNext(true)
    void queue.refetch().finally(() => {
      setBatch((current) => current + 1)
      setPosition(0)
      setFetchingNext(false)
    })
  }

  return (
    <section className="flex flex-col gap-4">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-2xl font-bold">{title}</h1>
        <p className="flex items-baseline gap-3 text-sm text-ink-muted">
          {endsAt !== null && !end && <SessionClock endsAt={endsAt} />}
          <span>{cardsReviewed(tally.reviewed)}</span>
        </p>
      </header>
      {progress.data && <DailyGoal progress={progress.data} />}
      {end ? (
        <SessionSummary
          end={end}
          tally={tally}
          subjectTitle={subjectTitle}
          nextDueAt={queue.data.nextDueAt}
          onRestart={onRestart}
          onChangeSettings={onChangeSettings}
        />
      ) : card ? (
        <>
          <ReviewCard key={`${batch}:${card.cardId}`} card={card} onReviewed={reviewed} />
          <div className="flex items-center justify-between gap-3">
            <ReportButton key={`report-${card.cardId}`} target={{ cardId: card.cardId }} />
            <button
              type="button"
              onClick={() => setStopped('stopped')}
              className="text-sm text-ink-muted underline hover:text-ink-soft"
            >
              Terminer la session
            </button>
          </div>
        </>
      ) : (
        <p className="text-ink-muted">Chargement des cartes suivantes…</p>
      )}
    </section>
  )
}

/** Le temps qui reste, en minutes : il borne la session sans presser la réponse (ADR 0048). */
function SessionClock({ endsAt }: { endsAt: number }) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), CLOCK_TICK_MS)
    return () => clearInterval(timer)
  }, [])
  const remaining = endsAt - now

  if (remaining <= 0) {
    return (
      <span role="timer" aria-label="Temps restant" className="font-medium text-warning">
        Temps écoulé : finissez cette carte
      </span>
    )
  }
  const minutesLeft = Math.ceil(remaining / MINUTE_MS)
  return (
    <span role="timer" aria-label="Temps restant">
      {minutesLeft > 1 ? `Encore ${minutesLeft} min` : 'Moins d’une minute'}
    </span>
  )
}

function SessionSummary({
  end,
  tally,
  subjectTitle,
  nextDueAt,
  onRestart,
  onChangeSettings,
}: {
  end: SessionEnd
  tally: Tally
  subjectTitle?: string
  nextDueAt?: string | null
  onRestart: () => void
  onChangeSettings?: () => void
}) {
  // Instant figé à l'affichage : le rendu reste pur (le délai n'a pas besoin d'être rafraîchi en continu).
  const [now] = useState(() => Date.now())
  const heading = {
    time: 'Temps écoulé : session terminée',
    stopped: 'Session terminée',
    done: subjectTitle
      ? `Tout est à jour en ${subjectTitle} pour le moment.`
      : 'Tout est à jour pour le moment.',
  }[end]

  return (
    <section
      aria-label="Bilan de la session"
      className="flex flex-col items-start gap-3 rounded-xl bg-surface p-5 shadow-sm"
    >
      <h2 className="font-medium">{heading}</h2>
      {tally.reviewed > 0 && (
        <p className="text-ink-muted">
          {cardsReviewed(tally.reviewed)}, dont {tally.recalled} réussie
          {tally.recalled > 1 ? 's' : ''}.
        </p>
      )}
      {end === 'done' && nextDueAt && (
        <p className="text-ink-muted">
          Prochaine carte à revoir dans {formatDelay(Date.parse(nextDueAt) - now)}.
        </p>
      )}
      <div className="flex flex-wrap gap-3">
        {end !== 'done' && (
          <button type="button" onClick={onRestart} className={PRIMARY}>
            Relancer une session
          </button>
        )}
        {onChangeSettings ? (
          <button type="button" onClick={onChangeSettings} className={SECONDARY}>
            {end === 'done' ? 'Choisir un autre sujet' : 'Changer de sujet ou de durée'}
          </button>
        ) : (
          <Link to="/review" className={SECONDARY}>
            Lancer une autre session
          </Link>
        )}
      </div>
    </section>
  )
}
