import { Link } from 'react-router'
import type { Progress } from '../api/progress'

/**
 * Une mesure de l'en-tête : un chiffre nu à côté de son dessin sur un téléphone, comme sur la maquette, une pastille en
 * relief sur un ordinateur. C'est un lien : la cible fait 44 px de haut.
 */
const MEASURE =
  'inline-flex min-h-11 items-center gap-1.5 text-base font-black whitespace-nowrap text-ink hover:text-brand-strong md:rounded-full md:bg-surface md:px-4 md:text-[1.0625rem] md:shadow-float'

/** La flamme de la série : son cœur est jaune, comme sur la maquette. */
function Flame() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="size-[1.375rem] shrink-0">
      <path
        d="M12 2c1 5 6 6 6 12a6 6 0 0 1-12 0c0-2 1-4 2-5 0 2 1 3 2 3 0-4-1-6 2-10z"
        className="fill-flame"
      />
      <path
        d="M12 13c1 2 3 2 3 4.5a3 3 0 0 1-6 0c0-1.5 1-2 1.5-3 .5 1 1 .5 1.5-1.5z"
        className="fill-sun"
      />
    </svg>
  )
}

/** La gemme de l'XP, avec sa facette plus claire. */
function Gem() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="size-5 shrink-0">
      <path d="M12 2l9 8-9 12L3 10z" className="fill-brand" />
      <path d="M12 2l4 8-4 12-4-12z" className="fill-brand-line" />
    </svg>
  )
}

/**
 * La série et l'XP, dans l'en-tête quand on est connecté (ADR 0077). Chacune mène à « Ta progression », qui en dit plus :
 * les gels de streak, le calendrier, la progression par sujet. Sur un téléphone, l'unité est sous-entendue par le
 * dessin ; le nom du lien la dit toujours. Un nouveau venu, sans série ni XP, ne voit pas deux zéros : rien n'est
 * affiché avant sa première carte.
 */
export function ProgressBadge({ progress }: { progress: Progress }) {
  if (progress.streak === 0 && progress.totalXp === 0) return null
  const unit = `jour${progress.streak > 1 ? 's' : ''}`
  const xp = progress.totalXp.toLocaleString('fr-FR')
  return (
    <span className="flex items-center gap-3.5 md:gap-3">
      <Link
        to="/progress"
        title="Jours consécutifs avec l’objectif atteint"
        aria-label={`Série : ${progress.streak} ${unit}`}
        className={MEASURE}
      >
        <Flame />
        <span>
          {progress.streak}
          <span className="max-md:hidden"> {unit}</span>
        </span>
      </Link>
      <Link to="/progress" title="XP gagnée" aria-label={`${xp} XP`} className={MEASURE}>
        <Gem />
        <span>
          {xp}
          <span className="max-md:hidden"> XP</span>
        </span>
      </Link>
    </span>
  )
}
