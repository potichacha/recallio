import { Fire } from '@phosphor-icons/react'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useQuests } from '../api/quests'
import { useRecentActivity, useReviewStats } from '../api/stats'
import { GoalRing } from '../components/GoalRing'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { freezeSummary } from '../components/streak'
import { button } from '../components/ui/button'
import { loadSessionSettings } from '../review/sessionSettings'
import { ActivityCalendar } from './ActivityCalendar'
import { ChapterPath } from './ChapterPath'
import { QuestsPanel } from './QuestsPanel'
import { SubjectProgressList } from './SubjectProgressList'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** Le calendrier d'activité remonte à cinq semaines. */
const CALENDAR_DAYS = 35

/**
 * « Ta progression » (ADR 0077) : ce que l'accueil montrait avant de se réduire à une carte dominante, tel quel.
 * L'objectif du jour et son anneau, la série, ses gels et l'XP, cinq semaines d'activité, le chemin du sujet en cours,
 * les quêtes du jour et de la semaine, puis chaque sujet suivi : son niveau, ses cartes, la maîtrise de ses notions, son
 * indice de préparation et le compte à rebours avant son entretien. On y arrive par la série et l'XP de l'en-tête. Un
 * nouveau venu n'y voit aucun bloc à zéro : ses quêtes, et de quoi commencer.
 */
export function ProgressPage() {
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const activity = useRecentActivity(CALENDAR_DAYS)
  const quests = useQuests()
  const [stored] = useState(loadSessionSettings)

  const followed = profile.data?.followedSubjects ?? []
  const current = stored.subject && followed.includes(stored.subject) ? stored.subject : followed[0]
  const currentStats = stats.data?.subjects.find((subject) => subject.slug === current)
  // Tant qu'aucune carte n'a été révisée, il n'y a rien à montrer d'une progression.
  const newcomer = stats.data?.overall.seen === 0

  return (
    <section className="flex flex-col gap-4">
      <header className="flex flex-col gap-1">
        <Link to="/" className="self-start text-sm font-bold text-brand-strong hover:underline">
          ← Accueil
        </Link>
        <h1 className="text-2xl font-black">Ta progression</h1>
      </header>

      {stats.isPending && <LoadingState label="Chargement de ta progression…" />}
      {stats.isError && (
        <ErrorState
          message="Impossible de charger tes statistiques."
          onRetry={() => void stats.refetch()}
        />
      )}
      {newcomer && (
        <EmptyState
          title="Ta progression commence à ta première carte."
          text="Ta série, ton XP, ton calendrier et tes sujets s’afficheront ici."
          pose="pointing"
        >
          <Link to="/review" className={button()}>
            Faire mes premières révisions
          </Link>
        </EmptyState>
      )}

      {!newcomer && progress.data && (
        <>
          <section
            aria-labelledby="goal-title"
            className="flex items-center justify-between gap-4 rounded-2xl bg-surface p-4"
          >
            <div className="flex flex-col gap-1">
              <h2 id="goal-title" className="text-lg font-black">
                Objectif du jour
              </h2>
              <p className="text-sm font-bold text-ink-muted">
                {progress.data.goalReachedToday
                  ? 'Atteint. À demain !'
                  : `${plural(Math.min(progress.data.cardsToday, progress.data.dailyGoal), 'carte')} sur ${progress.data.dailyGoal}`}
              </p>
            </div>
            <GoalRing done={progress.data.cardsToday} goal={progress.data.dailyGoal} />
          </section>
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
        </>
      )}
      {!newcomer && activity.data && <ActivityCalendar days={activity.data} />}
      {!newcomer && currentStats && current && (
        <ChapterPath slug={current} title={currentStats.title} themes={currentStats.themes} />
      )}

      {quests.data && <QuestsPanel board={quests.data} />}

      {!newcomer && stats.data && (
        <SubjectProgressList
          subjects={stats.data.subjects}
          levels={progress.data?.subjects ?? []}
        />
      )}
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
