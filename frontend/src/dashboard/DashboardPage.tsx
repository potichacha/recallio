import { Link } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useRecentActivity, useReviewStats, type ReviewStats } from '../api/stats'
import { DailyGoal } from '../components/DailyGoal'
import { ThemeProgressList } from './ThemeProgressList'
import { WeekActivity } from './WeekActivity'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** Tableau de bord personnel (CLAUDE.md, phase 1) : quoi réviser aujourd'hui, série, progression par thème. */
export function DashboardPage() {
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const activity = useRecentActivity()

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">
        {profile.data ? `Bonjour ${profile.data.displayName}` : 'Tableau de bord'}
      </h1>

      {stats.isError && <p role="alert">Impossible de charger vos statistiques.</p>}
      {stats.data && <TodayCard stats={stats.data} />}
      {progress.data && (
        <div className="rounded-xl bg-white p-5 shadow-sm">
          <DailyGoal progress={progress.data} />
        </div>
      )}

      {progress.data && stats.data && (
        <dl className="grid grid-cols-3 gap-3 text-center">
          <Tile label="Série" value={`🔥 ${plural(progress.data.streak, 'jour')}`} />
          <Tile label="Points" value={String(progress.data.totalPoints)} />
          <Tile
            label="Maîtrisées"
            value={`${stats.data.overall.mastered} / ${stats.data.overall.total}`}
          />
        </dl>
      )}

      {activity.data && <WeekActivity days={activity.data} />}
      {stats.data && <ThemeProgressList themes={stats.data.themes} />}
    </section>
  )
}

function TodayCard({ stats }: { stats: ReviewStats }) {
  const { dueNow, dueToday } = stats.overall
  const toReview = dueNow + stats.newToday
  return (
    <div className="flex flex-col items-start gap-3 rounded-xl bg-indigo-600 p-5 text-white shadow-sm">
      {toReview > 0 ? (
        <>
          <p className="text-lg font-semibold">
            {plural(dueNow, 'carte')} à revoir, {plural(stats.newToday, 'nouvelle')}
          </p>
          <Link
            to="/review"
            className="rounded-lg bg-white px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50"
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

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl bg-white p-4 shadow-sm">
      <dt className="text-xs font-medium tracking-wide text-slate-500 uppercase">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  )
}
