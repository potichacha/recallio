import type { Progress } from '../api/progress'

/**
 * Avancement de l'objectif du jour : c'est lui qui fait compter la journée dans le streak, et qui rapporte un gel de
 * streak tous les sept jours (ADR 0036).
 */
export function DailyGoal({ progress }: { progress: Progress }) {
  const done = Math.min(progress.cardsToday, progress.dailyGoal)
  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-between text-sm">
        <span className="font-medium">Objectif du jour</span>
        <span className="text-ink-muted">
          {done} / {progress.dailyGoal}
        </span>
      </div>
      <progress
        value={done}
        max={progress.dailyGoal}
        aria-label="Objectif du jour"
        className="h-2 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-success [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:bg-success"
      />
      {progress.goalReachedToday && (
        <p role="status" className="text-sm font-medium text-success">
          Objectif atteint ! Ta série passe à {progress.streak} jour
          {progress.streak > 1 ? 's' : ''}.
          {progress.freezeEarnedToday &&
            ' Elle te rapporte un gel de streak, qui couvrira un jour manqué.'}
        </p>
      )}
    </div>
  )
}
