import { Check, LockSimple } from '@phosphor-icons/react'
import { Link } from 'react-router'
import type { ReviewStats } from '../api/stats'

type Theme = ReviewStats['subjects'][number]['themes'][number]

/** Les étapes affichées : le domaine où l'on en est, celui d'avant et celui d'après. Le cours les montre tous. */
const SHOWN = 3

type State = 'done' | 'current' | 'upcoming'
type Step = { theme: Theme; state: State; percent: number }

/** L'état de chaque domaine : fini quand toutes ses cartes ont été vues, en cours pour le premier qui ne l'est pas. */
function steps(themes: Theme[]): Step[] {
  const withCards = themes.filter((theme) => theme.counts.total > 0)
  const current = withCards.findIndex((theme) => theme.counts.seen < theme.counts.total)
  return withCards.map((theme, index) => ({
    theme,
    state: current === -1 || index < current ? 'done' : index === current ? 'current' : 'upcoming',
    percent: Math.floor((100 * theme.counts.seen) / theme.counts.total),
  }))
}

/** Ce qu'un lecteur d'écran entend après le nom du domaine : l'état ne repose pas sur la couleur de la pastille. */
function spoken({ state, percent }: Step): string {
  if (state === 'done') return 'terminé'
  return state === 'current' ? `en cours, ${percent} % des cartes vues` : 'à venir'
}

const DOT: Record<State, string> = {
  done: 'bg-success text-on-success',
  current: 'border-4 border-sun bg-brand text-on-brand',
  upcoming: 'bg-surface-sunken text-ink-muted',
}

/**
 * Le chemin d'un sujet (ADR 0058) : trois pastilles reliées par un trait, pour voir d'où l'on vient et ce qui attend. Un
 * domaine fini porte une coche, celui où l'on en est son pourcentage de cartes vues, le suivant un cadenas. Tout le
 * chemin est un lien vers le cours du sujet, sa carte des compétences ; son texte dit l'état de chaque domaine.
 */
export function ChapterPath({
  slug,
  title,
  themes,
}: {
  slug: string
  title: string
  themes: Theme[]
}) {
  const all = steps(themes)
  if (all.length === 0) return null
  const current = all.findIndex((step) => step.state === 'current')
  const start = Math.max(
    0,
    Math.min((current === -1 ? all.length : current) - 1, all.length - SHOWN),
  )
  const shown = all.slice(start, start + SHOWN)

  return (
    <section aria-labelledby="path-title" className="flex flex-col gap-2.5">
      <h2 id="path-title" className="text-lg font-black">
        Ton chemin en {title}
      </h2>
      <Link
        to={`/subjects/${encodeURIComponent(slug)}/course`}
        className="flex items-start rounded-2xl bg-surface p-4 hover:bg-brand-soft"
      >
        {shown.map((step, index) => (
          <span
            key={step.theme.slug}
            className="relative flex flex-1 flex-col items-center gap-1.5"
          >
            {/* Le trait qui relie une étape à la précédente : vert une fois le chemin parcouru. */}
            {index > 0 && (
              <span
                aria-hidden="true"
                className={`absolute top-[21px] right-1/2 -left-1/2 mx-8 h-1.5 rounded-full ${shown[index - 1].state === 'done' ? 'bg-success' : 'bg-surface-sunken'}`}
              />
            )}
            <span
              aria-hidden="true"
              className={`flex size-12 items-center justify-center rounded-full text-sm font-black ${DOT[step.state]}`}
            >
              {step.state === 'done' && <Check weight="bold" className="size-6" />}
              {step.state === 'current' && `${step.percent}%`}
              {step.state === 'upcoming' && <LockSimple weight="fill" className="size-5" />}
            </span>
            <span
              className={`line-clamp-2 px-1 text-center text-xs font-extrabold ${step.state === 'upcoming' ? 'text-ink-muted' : ''}`}
            >
              {step.theme.title}
              <span className="sr-only"> : {spoken(step)}. </span>
            </span>
          </span>
        ))}
        <span className="sr-only">Voir tout le cours de {title}</span>
      </Link>
    </section>
  )
}
