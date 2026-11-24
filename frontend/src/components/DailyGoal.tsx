import { useState, type ReactNode } from 'react'
import type { Progress } from '../api/progress'
import { Celebration } from './Celebration'

/**
 * Avancement de l'objectif du jour, la barre en tête d'une session (ADR 0058) : c'est lui qui fait compter la journée
 * dans le streak, et qui rapporte un gel de streak tous les sept jours (ADR 0036). Atteint pendant la session, il se
 * fête d'une gerbe de confettis, une fois : pas s'il l'était déjà à l'ouverture.
 *
 * @param leading ce qui précède la barre sur sa ligne : la croix qui termine la session
 */
export function DailyGoal({ progress, leading }: { progress: Progress; leading?: ReactNode }) {
  const done = Math.min(progress.cardsToday, progress.dailyGoal)
  const [reachedBefore] = useState(progress.goalReachedToday)
  return (
    <div className="relative flex flex-col gap-1">
      <div className="flex items-center gap-3">
        {leading}
        <progress
          value={done}
          max={progress.dailyGoal}
          aria-label="Objectif du jour"
          className="h-4 w-full flex-1 overflow-hidden rounded-full border-2 border-line-strong bg-brand-soft [&::-moz-progress-bar]:rounded-full [&::-moz-progress-bar]:bg-success [&::-webkit-progress-bar]:bg-brand-soft [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-success [&::-webkit-progress-value]:transition-[width] [&::-webkit-progress-value]:duration-500"
        />
        <span
          title="Objectif du jour"
          className="shrink-0 text-[0.9375rem] font-black text-ink-soft"
        >
          {done}/{progress.dailyGoal}
        </span>
      </div>
      {progress.goalReachedToday && (
        <p role="status" className="text-sm font-extrabold text-success">
          Objectif atteint ! Ta série passe à {progress.streak} jour
          {progress.streak > 1 ? 's' : ''}.
          {progress.freezeEarnedToday &&
            ' Elle te rapporte un gel de streak, qui couvrira un jour manqué.'}
        </p>
      )}
      {progress.goalReachedToday && !reachedBefore && <Celebration />}
    </div>
  )
}
