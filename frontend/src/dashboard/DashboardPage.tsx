import { Fire, Path } from '@phosphor-icons/react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { useSubjects } from '../api/content'
import { useProfile } from '../api/profile'
import { useProgress, type Progress } from '../api/progress'
import { useQuests } from '../api/quests'
import { useRecentActivity, useReviewStats, type ReviewStats } from '../api/stats'
import { GoalRing } from '../components/GoalRing'
import { freezeSummary } from '../components/streak'
import {
  DEFAULT_MINUTES,
  DEFAULT_MODE,
  loadSessionSettings,
  type SessionMinutes,
  type SessionMode,
} from '../review/sessionSettings'
import { QuestsPanel } from './QuestsPanel'
import { SubjectProgressList } from './SubjectProgressList'
import { WeekActivity } from './WeekActivity'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** Les Tours proposés sur l'accueil : ceux des sujets suivis, deux au plus. */
const TOUR_SHORTCUTS = 2

/**
 * L'accueil d'un utilisateur connecté (ADR 0050) : une seule action en tête, continuer à réviser là où il s'était
 * arrêté, avec l'anneau de l'objectif du jour ; puis le Tour, les quêtes, et sa progression plus bas. Qui apprend son
 * sujet reprend d'abord le Tour (ADR 0052).
 */
export function DashboardPage() {
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const activity = useRecentActivity()
  const quests = useQuests()
  const subjects = useSubjects()
  const [stored] = useState(loadSessionSettings)

  const followed = profile.data?.followedSubjects ?? []
  const current = stored.subject && followed.includes(stored.subject) ? stored.subject : followed[0]
  const titleOf = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug
  const tours = (subjects.data ?? [])
    .filter((subject) => subject.hasTour && followed.includes(subject.slug))
    .slice(0, TOUR_SHORTCUTS)
  const firstName = profile.data?.displayName.split(' ')[0]
  // Qui apprend (ADR 0032, 0052) reprend d'abord son Tour ; qui prépare un entretien, d'abord sa révision.
  const learning =
    current !== undefined &&
    profile.data?.tracks[current] === 'LEARN' &&
    tours.some((tour) => tour.slug === current)
  const tourShortcuts = tours.length > 0 && (
    <ul className="grid gap-3 sm:grid-cols-2">
      {tours.map((subject) => (
        <li key={subject.slug}>
          <Link
            to={`/subjects/${encodeURIComponent(subject.slug)}/tour`}
            className="flex items-center gap-3 rounded-xl bg-surface p-4 shadow-sm hover:bg-brand-soft"
          >
            <Path weight="bold" aria-hidden className="size-7 shrink-0 text-brand" />
            <span className="flex flex-col">
              <span className="font-semibold">Le Tour de {subject.title}</span>{' '}
              <span className="text-sm text-ink-muted">Reprendre là où tu t’es arrêté</span>
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )

  return (
    <section className="flex flex-col gap-6">
      <header className="flex items-center gap-4">
        {progress.data && (
          <GoalRing done={progress.data.cardsToday} goal={progress.data.dailyGoal} />
        )}
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold">{firstName ? `Bonjour ${firstName}` : 'Bonjour'}</h1>
          {progress.data && <p className="text-ink-muted">{goalMessage(progress.data)}</p>}
        </div>
      </header>

      {stats.isError && <p role="alert">Impossible de charger tes statistiques.</p>}
      {learning && tourShortcuts}
      {stats.data && current && (
        <ContinueCard
          subject={current}
          title={titleOf(current)}
          stats={stats.data}
          minutes={stored.minutes === undefined ? DEFAULT_MINUTES : stored.minutes}
          mode={stored.mode ?? DEFAULT_MODE}
        />
      )}

      {!learning && tourShortcuts}

      {quests.data && <QuestsPanel board={quests.data} />}

      <section aria-labelledby="progress-title" className="flex flex-col gap-4">
        <h2 id="progress-title" className="text-xl font-semibold">
          Ta progression
        </h2>
        {progress.data && (
          <dl className="grid grid-cols-2 gap-3 text-center">
            <Tile
              label="Série"
              value={
                <span className="inline-flex items-center gap-1">
                  <Fire weight="fill" aria-hidden className="text-flame" />
                  {plural(progress.data.streak, 'jour')}
                </span>
              }
              hint={freezeSummary(progress.data)}
            />
            <Tile label="XP" value={String(progress.data.totalXp)} />
          </dl>
        )}
        {activity.data && <WeekActivity days={activity.data} />}
        {stats.data && (
          <SubjectProgressList
            subjects={stats.data.subjects}
            levels={progress.data?.subjects ?? []}
          />
        )}
      </section>
    </section>
  )
}

function goalMessage({ cardsToday, dailyGoal, goalReachedToday, streak }: Progress): string {
  if (goalReachedToday) return `Objectif atteint : ${plural(streak, 'jour')} de suite.`
  const left = plural(Math.max(0, dailyGoal - cardsToday), 'carte')
  return streak > 0 ? `Encore ${left} pour tenir ta série.` : `${left} pour lancer ta série.`
}

/** La seule action en tête de l'accueil : reprendre la révision avec les derniers réglages, en un geste. */
function ContinueCard({
  subject,
  title,
  stats,
  minutes,
  mode,
}: {
  subject: string
  title: string
  stats: ReviewStats
  minutes: SessionMinutes
  mode: SessionMode
}) {
  const subjectStats = stats.subjects.find((candidate) => candidate.slug === subject)
  const dueNow = subjectStats?.counts.dueNow ?? 0
  const newToday = subjectStats?.newToday ?? 0
  const session = `/review?subject=${encodeURIComponent(subject)}&minutes=${minutes ?? 'none'}&mode=${mode}`

  return (
    <section
      aria-labelledby="continue-title"
      className="flex flex-col gap-4 rounded-2xl bg-brand p-5 text-on-brand shadow-press"
    >
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="continue-title" className="text-2xl font-bold">
          {title}
        </h2>
        <p className="text-sm">
          {dueNow} à revoir · {plural(newToday, 'nouvelle')}
        </p>
      </div>
      {dueNow + newToday > 0 ? (
        <Link
          to={session}
          className="flex min-h-16 flex-col items-center justify-center rounded-xl bg-surface px-4 py-2 text-brand-strong shadow-[0_4px_0_0_var(--color-brand-press)] transition-transform active:translate-y-0.5 active:shadow-none motion-reduce:transition-none"
        >
          <span className="text-lg font-bold">Continuer</span>{' '}
          <span className="text-sm">
            {minutes === null ? 'Sans limite' : `${minutes} min`} ·{' '}
            {mode === 'graded' ? 'questions corrigées' : 'toutes les questions'}
          </span>
        </Link>
      ) : (
        <p className="font-semibold">Tout est à jour en {title} pour aujourd’hui.</p>
      )}
      <Link to="/review" className="self-center text-sm underline">
        Changer de sujet ou de durée
      </Link>
    </section>
  )
}

function Tile({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl bg-surface p-4 shadow-sm">
      <dt className="text-sm text-ink-muted">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
      {hint && <dd className="text-xs text-ink-muted">{hint}</dd>}
    </div>
  )
}
