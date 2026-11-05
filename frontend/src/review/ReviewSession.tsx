import { Fire } from '@phosphor-icons/react'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { PROGRESS_KEY, useProgress, type Progress } from '../api/progress'
import { useReviewQueue, type ReviewFilter, type ReviewResult } from '../api/review'
import { DailyGoal } from '../components/DailyGoal'
import { InstallApp } from '../components/InstallApp'
import { useLayoutMode } from '../components/layoutMode'
import { ReminderPrompt } from '../components/ReminderPrompt'
import { ReportButton } from '../components/ReportButton'
import { button } from '../components/ui/button'
import { isReturningVisitor } from '../pwa/installPrompt'
import { answerFeedback } from './feedback'
import { formatDelay } from './formatDelay'
import { ReviewCard } from './ReviewCard'
import type { SessionMinutes } from './sessionSettings'

const MINUTE_MS = 60_000
const CLOCK_TICK_MS = 1_000

/** Pourquoi une session s'arrête : le temps est écoulé, il ne reste rien à réviser, ou on l'a terminée. */
type SessionEnd = 'time' | 'done' | 'stopped'

type Tally = { reviewed: number; recalled: number }

const cardsReviewed = (count: number) =>
  `${count} carte${count > 1 ? 's' : ''} révisée${count > 1 ? 's' : ''}`

/**
 * Une session de révision : on parcourt la file carte par carte, et la série suivante est demandée dès que la
 * précédente est finie. Elle s'arrête quand sa durée est écoulée, toujours après la carte en cours (ADR 0048), quand il
 * ne reste rien à réviser, ou quand on la termine ; un bilan s'affiche alors. Pendant la session, la navigation se fait
 * discrète (ADR 0050).
 *
 * @param subjectTitle le sujet révisé, pour dire qu'il est à jour ; aucun pour un groupe ou un thème
 * @param onMore cinq minutes de plus sur le même sujet ; aucun pour une session ouverte par un lien direct
 * @param onChangeSettings revenir au lancement d'une session ; aucun pour une session ouverte par un lien direct
 */
export function ReviewSession({
  title,
  filter,
  minutes,
  subjectTitle,
  onRestart,
  onMore,
  onChangeSettings,
}: {
  title: ReactNode
  filter: ReviewFilter
  minutes: SessionMinutes
  subjectTitle?: string
  onRestart: () => void
  onMore?: () => void
  onChangeSettings?: () => void
}) {
  const queue = useReviewQueue(filter)
  const progress = useProgress()
  const queryClient = useQueryClient()
  // L'XP d'avant la session, pour en dire le gain au bilan : la progression est déjà chargée par l'en-tête.
  const [startXp] = useState(() => queryClient.getQueryData<Progress>(PROGRESS_KEY)?.totalXp)
  const [endsAt] = useState(() => (minutes === null ? null : Date.now() + minutes * MINUTE_MS))
  const [position, setPosition] = useState(0)
  // Le numéro de la série : une carte revenue dans la série suivante (notée « Je ne savais pas ») repart de son recto.
  const [batch, setBatch] = useState(0)
  const [tally, setTally] = useState<Tally>({ reviewed: 0, recalled: 0 })
  const [stopped, setStopped] = useState<SessionEnd>()
  const [fetchingNext, setFetchingNext] = useState(false)

  const cards = queue.data?.cards ?? []
  const card = cards.at(position)
  const end = stopped ?? (queue.data && !card && !fetchingNext ? 'done' : undefined)
  useLayoutMode({ focus: queue.isSuccess && end === undefined })

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

  const reviewed = (result: ReviewResult) => {
    const recalled = result.rating !== 'AGAIN'
    answerFeedback(recalled)
    setTally((current) => ({
      reviewed: current.reviewed + 1,
      recalled: current.recalled + (recalled ? 1 : 0),
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

  const xpGained =
    startXp !== undefined && progress.data ? progress.data.totalXp - startXp : undefined

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
          xpGained={xpGained}
          progress={progress.data}
          onRestart={onRestart}
          onMore={onMore}
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
              className="text-sm text-ink-muted underline hover:text-ink"
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
        Temps écoulé : finis cette carte
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

/**
 * Le bilan, moment fort de la session (ADR 0050) : ce qu'elle a rapporté, la série quand l'objectif est atteint, puis
 * ce qu'on peut faire ensuite. C'est aussi là que se proposent les rappels et l'installation, quand l'intérêt est clair.
 */
function SessionSummary({
  end,
  tally,
  subjectTitle,
  nextDueAt,
  xpGained,
  progress,
  onRestart,
  onMore,
  onChangeSettings,
}: {
  end: SessionEnd
  tally: Tally
  subjectTitle?: string
  nextDueAt?: string | null
  xpGained?: number
  progress?: Progress
  onRestart: () => void
  onMore?: () => void
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
  const worked = tally.reviewed > 0

  return (
    <section
      aria-label="Bilan de la session"
      className="flex flex-col gap-4 rounded-2xl bg-surface p-5 shadow-sm"
    >
      {worked && progress?.goalReachedToday && (
        <div className="flex flex-col items-center gap-2 text-center motion-safe:animate-pop">
          <span className="flex size-16 items-center justify-center rounded-full bg-sun">
            <Fire weight="fill" aria-hidden className="size-9 text-on-sun" />
          </span>
          <p className="text-xl font-bold">
            {progress.streak} jour{progress.streak > 1 ? 's' : ''} de suite
          </p>
        </div>
      )}
      <h2 className="text-lg font-semibold">{heading}</h2>
      {worked && (
        <>
          <p className="text-ink-muted">
            {cardsReviewed(tally.reviewed)}, dont {tally.recalled} réussie
            {tally.recalled > 1 ? 's' : ''}.
          </p>
          <dl className="grid grid-cols-3 gap-2 text-center">
            {xpGained !== undefined && <Stat label="XP gagnée" value={`+${xpGained}`} />}
            <Stat label="réussies" value={`${tally.recalled}/${tally.reviewed}`} />
            {progress && (
              <Stat
                label="objectif du jour"
                value={`${Math.min(progress.cardsToday, progress.dailyGoal)}/${progress.dailyGoal}`}
              />
            )}
          </dl>
        </>
      )}
      {end === 'done' && nextDueAt && (
        <p className="text-ink-muted">
          Prochaine carte à revoir dans {formatDelay(Date.parse(nextDueAt) - now)}.
        </p>
      )}
      <div className="flex flex-wrap gap-3">
        {end !== 'done' && onMore && (
          <button type="button" onClick={onMore} className={button()}>
            Encore 5 minutes
          </button>
        )}
        {end !== 'done' && (
          <button
            type="button"
            onClick={onRestart}
            className={button({ variant: onMore ? 'secondary' : 'primary' })}
          >
            Relancer une session
          </button>
        )}
        {onChangeSettings ? (
          <button
            type="button"
            onClick={onChangeSettings}
            className={button({ variant: 'secondary' })}
          >
            {end === 'done' ? 'Choisir un autre sujet' : 'Changer de sujet ou de durée'}
          </button>
        ) : (
          <Link to="/review" className={button({ variant: 'secondary' })}>
            Lancer une autre session
          </Link>
        )}
      </div>
      {worked && <ReminderPrompt />}
      {worked && isReturningVisitor() && <InstallApp />}
    </section>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5 rounded-xl bg-surface-muted px-2 py-3">
      <dt className="order-2 text-xs text-ink-muted">{label}</dt>
      <dd className="order-1 text-lg font-bold">{value}</dd>
    </div>
  )
}
