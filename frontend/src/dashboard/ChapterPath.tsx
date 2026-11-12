import { Check } from '@phosphor-icons/react'
import { Link } from 'react-router'
import type { ReviewStats } from '../api/stats'

type Theme = ReviewStats['subjects'][number]['themes'][number]

/** Les étapes affichées : celle où l'on en est, celle d'avant, et les suivantes. Le cours les montre toutes. */
const SHOWN = 5

type Step = { theme: Theme; state: 'done' | 'current' | 'upcoming'; percent: number }

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

const LABELS = { done: 'terminé', upcoming: 'à venir' } as const

/**
 * Le chemin d'un sujet (ADR 0058) : ses domaines, l'un après l'autre, pour voir d'où l'on vient et ce qui attend. Un
 * domaine fini porte une coche, celui où l'on en est son pourcentage de cartes vues ; l'état est aussi écrit, pour ne
 * pas reposer sur la seule couleur.
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
    <section aria-labelledby="path-title" className="flex flex-col gap-3">
      <h2 id="path-title" className="text-xl font-semibold">
        Ton chemin en {title}
      </h2>
      <ol className="flex flex-col">
        {shown.map(({ theme, state, percent }, index) => (
          <li key={theme.slug} className="flex items-stretch gap-3">
            <span className="flex flex-col items-center">
              <span
                aria-hidden="true"
                className={`flex size-11 shrink-0 items-center justify-center rounded-full text-sm font-extrabold ${
                  state === 'done'
                    ? 'bg-success text-on-success'
                    : state === 'current'
                      ? 'bg-brand text-on-brand shadow-press'
                      : 'border-2 border-line-strong bg-surface text-ink-muted'
                }`}
              >
                {state === 'done' ? <Check weight="bold" className="size-5" /> : start + index + 1}
              </span>
              {/* Le trait qui relie une étape à la suivante. */}
              {index < shown.length - 1 && (
                <span aria-hidden="true" className="min-h-4 w-0.5 grow bg-line-strong" />
              )}
            </span>
            <span className="flex flex-col pt-1 pb-4">
              <span className={state === 'current' ? 'font-extrabold' : 'font-semibold'}>
                {theme.title}
              </span>
              <span className="text-sm text-ink-muted">
                {state === 'current' ? `en cours · ${percent} % des cartes vues` : LABELS[state]}
              </span>
            </span>
          </li>
        ))}
      </ol>
      <Link
        to={`/subjects/${encodeURIComponent(slug)}/course`}
        className="self-start text-sm font-medium text-brand-strong hover:underline"
      >
        Voir tout le cours de {title}
      </Link>
    </section>
  )
}
