import type { Progress } from '../api/progress'

/** Streak et points, affichés dans l'en-tête quand on est connecté. */
export function ProgressBadge({ progress }: { progress: Progress }) {
  const days = `${progress.streak} jour${progress.streak > 1 ? 's' : ''}`
  return (
    <span className="flex items-center gap-3 text-sm text-slate-600">
      <span title="Jours consécutifs avec l'objectif atteint" aria-label={`Série : ${days}`}>
        <span aria-hidden="true">🔥</span> {progress.streak}
      </span>
      <span title="Points gagnés">{progress.totalPoints} pts</span>
    </span>
  )
}
