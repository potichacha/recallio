import type { Progress } from '../api/progress'

/** Streak, gels de streak (quand il y en a) et XP, affichés dans l'en-tête quand on est connecté. */
export function ProgressBadge({ progress }: { progress: Progress }) {
  const days = `${progress.streak} jour${progress.streak > 1 ? 's' : ''}`
  return (
    <span className="flex items-center gap-3 text-sm text-slate-600">
      <span title="Jours consécutifs avec l'objectif atteint" aria-label={`Série : ${days}`}>
        <span aria-hidden="true">🔥</span> {progress.streak}
      </span>
      {progress.streakFreezes > 0 && (
        <span
          title="Gels de streak : chacun couvre un jour manqué"
          aria-label={`Gels de streak : ${progress.streakFreezes}`}
        >
          <span aria-hidden="true">❄️</span> {progress.streakFreezes}
        </span>
      )}
      <span title="XP gagnée">{progress.totalXp} XP</span>
    </span>
  )
}
