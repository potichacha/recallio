import { Fire, Snowflake } from '@phosphor-icons/react'
import type { Progress } from '../api/progress'

/** Une pastille de l'en-tête : blanche sur le fond de la page d'un téléphone, creusée dans la barre d'un ordinateur. */
const PILL =
  'items-center gap-1.5 rounded-full bg-surface px-2.5 py-1.5 text-sm font-black whitespace-nowrap min-[360px]:px-3 min-[360px]:py-2 md:bg-surface-muted'

/**
 * La série et l'XP, en pastilles dans l'en-tête quand on est connecté, et les gels de streak quand il y en a (sur un
 * écran assez large : l'accueil les redit). Sur un très petit écran, la série se réduit à son nombre.
 */
export function ProgressBadge({ progress }: { progress: Progress }) {
  const unit = `jour${progress.streak > 1 ? 's' : ''}`
  return (
    <span className="flex items-center gap-1.5 min-[360px]:gap-2">
      <span
        title="Jours consécutifs avec l'objectif atteint"
        aria-label={`Série : ${progress.streak} ${unit}`}
        className={`${PILL} inline-flex text-ink`}
      >
        <Fire weight="fill" aria-hidden className="size-4 text-flame" />
        <span>
          {progress.streak}
          <span className="max-[359px]:hidden"> {unit}</span>
        </span>
      </span>
      {progress.streakFreezes > 0 && (
        <span
          title="Gels de streak : chacun couvre un jour manqué"
          aria-label={`Gels de streak : ${progress.streakFreezes}`}
          className={`${PILL} hidden text-ink sm:inline-flex`}
        >
          <Snowflake weight="bold" aria-hidden className="size-4 text-info" />
          {progress.streakFreezes}
        </span>
      )}
      <span title="XP gagnée" className={`${PILL} inline-flex text-brand-strong`}>
        {progress.totalXp.toLocaleString('fr-FR')} XP
      </span>
    </span>
  )
}
