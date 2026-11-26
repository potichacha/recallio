import { ErrorState } from '../components/states/ErrorState'
import { Rico } from '../components/Rico'
import { Fire, Path } from '@phosphor-icons/react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { useSubjects } from '../api/content'
import { useProfile } from '../api/profile'
import { useProgress, type Progress } from '../api/progress'
import { useQuests } from '../api/quests'
import { useRecentActivity, useReviewStats, type ReviewStats } from '../api/stats'
import { useTourProgress } from '../api/tour'
import { GoalRing } from '../components/GoalRing'
import { freezeSummary } from '../components/streak'
import { DailyChallengeShortcut } from '../daily/DailyChallengeLinks'
import {
  DEFAULT_MINUTES,
  loadSessionSettings,
  type SessionMinutes,
} from '../review/sessionSettings'
import { button } from '../components/ui/button'
import { QuestOfTheDay, QuestsPanel } from './QuestsPanel'
import { SubjectProgressList } from './SubjectProgressList'
import { ActivityCalendar } from './ActivityCalendar'
import { ChapterPath } from './ChapterPath'
import { ricoWord } from './ricoWord'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** Les Tours proposés sur l'accueil : ceux des sujets suivis, deux au plus. */
const TOUR_SHORTCUTS = 2

/** Le calendrier d'activité remonte à cinq semaines. */
const CALENDAR_DAYS = 35

/**
 * L'accueil d'un utilisateur connecté (ADR 0050, 0058). Le premier écran d'un téléphone est celui de la maquette : le
 * mot de Rico, puis une seule action, continuer à réviser là où il s'était arrêté, avec l'anneau de l'objectif du jour,
 * son chemin dans le sujet et la quête du jour. Plus bas : son défi du jour (ADR 0067), le Tour, les autres quêtes et
 * sa progression. Qui apprend son sujet reprend d'abord le Tour (ADR 0052). Un nouveau venu ne voit aucun bloc à zéro :
 * ni série, ni XP, ni calendrier avant sa première carte.
 */
export function DashboardPage() {
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const activity = useRecentActivity(CALENDAR_DAYS)
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
          <TourShortcut slug={subject.slug} title={subject.title} />
        </li>
      ))}
    </ul>
  )

  const currentStats = stats.data?.subjects.find((subject) => subject.slug === current)
  // Tant qu'aucune carte n'a été révisée, il n'y a rien à montrer d'une progression.
  const newcomer = stats.data?.overall.seen === 0
  const word =
    progress.data && stats.data
      ? ricoWord({
          firstName,
          progress: progress.data,
          newcomer,
          nothingToReview:
            currentStats !== undefined && currentStats.counts.dueNow + currentStats.newToday === 0,
        })
      : undefined
  // La quête mise en avant sur le premier écran : la première du jour qui reste à faire.
  const featuredQuest = quests.data?.daily.find((quest) => !quest.completed)

  return (
    <section className="flex flex-col gap-4">
      <header className="flex items-center gap-3.5">
        <Rico pose={word?.pose} decorative />
        <div className="flex flex-1 flex-col rounded-2xl bg-surface px-3.5 py-3">
          <h1 className="text-lg font-black">
            {word?.greeting ?? (firstName ? `Salut ${firstName} !` : 'Salut !')}
          </h1>
          {word && (
            <p className="text-[0.9375rem] leading-snug font-bold text-ink-soft">{word.message}</p>
          )}
        </div>
      </header>

      {stats.isError && (
        <ErrorState
          message="Impossible de charger tes statistiques."
          onRetry={() => void stats.refetch()}
        />
      )}
      {learning && tourShortcuts}
      {stats.data && current && (
        <ContinueCard
          subject={current}
          title={titleOf(current)}
          stats={stats.data}
          progress={progress.data}
          minutes={stored.minutes === undefined ? DEFAULT_MINUTES : stored.minutes}
        />
      )}

      {currentStats && current && (
        <ChapterPath slug={current} title={titleOf(current)} themes={currentStats.themes} />
      )}

      {featuredQuest && <QuestOfTheDay quest={featuredQuest} />}

      {current && <DailyChallengeShortcut subject={current} />}

      {!learning && tourShortcuts}

      {quests.data && <QuestsPanel board={quests.data} featured={featuredQuest} />}

      {!newcomer && (
        <section aria-labelledby="progress-title" className="flex flex-col gap-4">
          <h2 id="progress-title" className="text-lg font-black">
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
          {activity.data && <ActivityCalendar days={activity.data} />}
          {stats.data && (
            <SubjectProgressList
              subjects={stats.data.subjects}
              levels={progress.data?.subjects ?? []}
            />
          )}
        </section>
      )}
    </section>
  )
}

/**
 * Le raccourci vers le Tour d'un sujet suivi. Il ne promet de « reprendre » qu'à qui a déjà ouvert une page ; tant que
 * la progression charge, il ne dit rien plutôt que de changer de texte sous les yeux.
 */
function TourShortcut({ slug, title }: { slug: string; title: string }) {
  const progress = useTourProgress(slug, true)
  return (
    <Link
      to={`/subjects/${encodeURIComponent(slug)}/tour`}
      className="flex items-center gap-3 rounded-2xl bg-surface px-4 py-3.5 hover:bg-brand-soft"
    >
      <Path weight="bold" aria-hidden className="size-7 shrink-0 text-brand" />
      <span className="flex flex-col">
        <span className="font-extrabold">Le Tour de {title}</span>{' '}
        {progress.data && (
          <span className="text-sm font-bold text-ink-muted">
            {progress.data.lastPage ? 'Reprendre là où tu t’es arrêté' : 'Commencer le Tour'}
          </span>
        )}
      </span>
    </Link>
  )
}

/**
 * La carte « Aujourd'hui », la seule action en tête de l'accueil : l'anneau de l'objectif du jour, ce qui attend dans le
 * sujet, et la révision reprise avec les derniers réglages, en un geste.
 */
function ContinueCard({
  subject,
  title,
  stats,
  progress,
  minutes,
}: {
  subject: string
  title: string
  stats: ReviewStats
  progress?: Progress
  minutes: SessionMinutes
}) {
  const subjectStats = stats.subjects.find((candidate) => candidate.slug === subject)
  const dueNow = subjectStats?.counts.dueNow ?? 0
  const newToday = subjectStats?.newToday ?? 0
  const session = `/review?subject=${encodeURIComponent(subject)}&minutes=${minutes ?? 'none'}`

  return (
    <section
      aria-labelledby="continue-title"
      className="flex flex-col gap-3.5 rounded-3xl bg-hero p-5 text-on-hero"
    >
      <div className="flex items-center justify-between gap-4">
        <div className="flex flex-col">
          <p className="text-sm font-extrabold text-on-hero-muted">Aujourd’hui</p>
          <h2 id="continue-title" className="text-[1.625rem] leading-tight font-black">
            {title}
          </h2>
        </div>
        {progress && <GoalRing done={progress.cardsToday} goal={progress.dailyGoal} onHero />}
      </div>
      <p className="text-[0.9375rem] font-bold text-on-hero-muted">
        {plural(dueNow, 'carte')} à revoir · {plural(newToday, 'nouvelle')}
      </p>
      {dueNow + newToday > 0 ? (
        <Link
          to={session}
          className={`${button({ variant: 'sun', size: 'lg' })} focus-visible:outline-on-hero md:min-w-80 md:self-start`}
        >
          Continuer · {minutes === null ? 'sans limite' : `${minutes} min`}
        </Link>
      ) : (
        <p className="font-extrabold">Tout est à jour en {title} pour aujourd’hui.</p>
      )}
      <Link
        to="/review"
        className="self-center py-0.5 text-sm font-bold underline focus-visible:outline-on-hero"
      >
        Changer de sujet ou de durée
      </Link>
    </section>
  )
}

function Tile({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-2xl bg-surface p-4">
      <dt className="text-sm font-bold text-ink-muted">{label}</dt>
      <dd className="text-lg font-black">{value}</dd>
      {hint && <dd className="text-xs text-ink-muted">{hint}</dd>}
    </div>
  )
}
