import { Fire } from '@phosphor-icons/react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useQuests } from '../api/quests'
import { useRecentActivity, useReviewStats, type ReviewStats } from '../api/stats'
import { DailyGoal } from '../components/DailyGoal'
import { freezeSummary } from '../components/streak'
import { QuestsPanel } from './QuestsPanel'
import { SubjectProgressList } from './SubjectProgressList'
import { WeekActivity } from './WeekActivity'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Tableau de bord personnel (CLAUDE.md, phase 1) : quoi réviser aujourd'hui, série, quêtes (ADR 0040), progression par
 * sujet et par thème.
 */
export function DashboardPage() {
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const activity = useRecentActivity()
  const quests = useQuests()

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">
        {profile.data ? `Bonjour ${profile.data.displayName}` : 'Tableau de bord'}
      </h1>

      {stats.isError && <p role="alert">Impossible de charger vos statistiques.</p>}
      {stats.data && <TodayCard stats={stats.data} />}
      {progress.data && (
        <div className="rounded-xl bg-surface p-5 shadow-sm">
          <DailyGoal progress={progress.data} />
        </div>
      )}

      {progress.data && stats.data && (
        <dl className="grid grid-cols-3 gap-3 text-center">
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
          <Tile
            label="Maîtrisées"
            value={`${stats.data.overall.mastered} / ${stats.data.overall.total}`}
          />
        </dl>
      )}

      {quests.data && <QuestsPanel board={quests.data} />}
      {activity.data && <WeekActivity days={activity.data} />}
      {stats.data && (
        <SubjectProgressList
          subjects={stats.data.subjects}
          levels={progress.data?.subjects ?? []}
        />
      )}
    </section>
  )
}

function TodayCard({ stats }: { stats: ReviewStats }) {
  const { dueNow, dueToday } = stats.overall
  const toReview = dueNow + stats.newToday
  return (
    <div className="flex flex-col items-start gap-3 rounded-xl bg-brand p-5 text-on-brand shadow-sm">
      {toReview > 0 ? (
        <>
          <p className="text-lg font-semibold">
            {plural(dueNow, 'carte')} à revoir, {plural(stats.newToday, 'nouvelle')}
          </p>
          <Link
            to="/review"
            className="rounded-lg bg-surface px-4 py-2 font-medium text-brand-strong hover:bg-brand-soft"
          >
            Réviser maintenant
          </Link>
        </>
      ) : (
        <p className="text-lg font-semibold">
          Tout est à jour
          {dueToday > 0 && ` : encore ${plural(dueToday, 'carte')} à revoir plus tard aujourd'hui`}.
        </p>
      )}
    </div>
  )
}

function Tile({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl bg-surface p-4 shadow-sm">
      <dt className="text-xs font-medium tracking-wide text-ink-muted uppercase">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
      {hint && <dd className="text-xs text-ink-muted">{hint}</dd>}
    </div>
  )
}
