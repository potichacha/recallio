import type { Progress } from '../api/progress'

/** Avancement de l'objectif du jour : c'est lui qui fait compter la journée dans le streak. */
export function DailyGoal({ progress }: { progress: Progress }) {
  const done = Math.min(progress.cardsToday, progress.dailyGoal)
  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-between text-sm">
        <span className="font-medium">Objectif du jour</span>
        <span className="text-slate-500">
          {done} / {progress.dailyGoal}
        </span>
      </div>
      <progress
        value={done}
        max={progress.dailyGoal}
        aria-label="Objectif du jour"
        className="h-2 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-emerald-500 [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:bg-emerald-500"
      />
      {progress.goalReachedToday && (
        <p role="status" className="text-sm font-medium text-emerald-700">
          Objectif atteint ! Votre série passe à {progress.streak} jour
          {progress.streak > 1 ? 's' : ''}.
        </p>
      )}
    </div>
  )
}
