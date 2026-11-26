import { CaretRight, NotePencil, Timer, type Icon } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { useSubjects } from '../api/content'
import { useProfile } from '../api/profile'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { button } from '../components/ui/button'
import { card } from '../components/ui/card'
import { DoorBanner } from '../components/ui/DoorBanner'
import { heading } from '../components/ui/typography'
import { mistakesPath } from './mistakesPath'

/** Ce qu'un sujet suivi propose pour s'entraîner, en attendant la révision intensive (étape 6 de la refonte). */
const WAYS: { icon: Icon; title: string; text: string; to: (subject: string) => string }[] = [
  {
    icon: Timer,
    title: 'Examen blanc',
    text: 'Vingt questions au plus, une minute chacune, puis ta note et le corrigé.',
    to: (subject) => `/subjects/${encodeURIComponent(subject)}/exam`,
  },
  {
    icon: NotePencil,
    title: 'Carnet d’erreurs',
    text: 'Tes cartes ratées cette semaine, à refaire sans rien enregistrer.',
    to: mistakesPath,
  },
]

/**
 * S'entraîner (ADR 0077) : pour chaque sujet suivi, l'examen blanc (ADR 0044) et le carnet d'erreurs (ADR 0057). Rien
 * de ce qu'on fait ici n'écrit dans la répétition espacée. Qui ne suit encore aucun sujet est conduit à les choisir.
 */
export function PracticeDoorPage() {
  const profile = useProfile()
  const subjects = useSubjects()
  const followed = profile.data?.followedSubjects ?? []
  const titleOf = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug

  return (
    <section className="flex flex-col gap-5">
      <DoorBanner door="practice" eyebrow="S’entraîner" title="Comme en entretien">
        <p className="max-w-xs text-[0.9375rem] leading-[1.4] font-bold text-info-soft">
          Un examen blanc chronométré, et tes erreurs de la semaine à refaire.
        </p>
      </DoorBanner>
      {profile.isPending && <LoadingState label="Chargement de tes sujets…" shape="tiles" />}
      {profile.isError && (
        <ErrorState
          message="Impossible de charger tes sujets."
          onRetry={() => void profile.refetch()}
        />
      )}
      {profile.isSuccess && followed.length === 0 && (
        <EmptyState
          title="Tu ne suis encore aucun sujet."
          text="Choisis un sujet : son examen blanc et ton carnet d’erreurs arrivent ici."
          pose="pointing"
        >
          <Link to="/profile/subjects" className={button()}>
            Choisir mes sujets
          </Link>
        </EmptyState>
      )}
      {followed.map((slug) => (
        <section key={slug} aria-labelledby={`practice-${slug}`} className="flex flex-col gap-3">
          <h2 id={`practice-${slug}`} className={heading({ size: 'md' })}>
            {titleOf(slug)}
          </h2>
          <ul className="grid gap-3 sm:grid-cols-2">
            {WAYS.map(({ icon: WayIcon, title, text, to }) => (
              <li key={title}>
                <Link
                  to={to(slug)}
                  className={`flex h-full items-center gap-3 ${card()} motion-safe:transition-transform motion-safe:duration-75 motion-safe:active:translate-y-1`}
                >
                  <span
                    aria-hidden="true"
                    className="flex size-11 shrink-0 items-center justify-center rounded-[0.875rem] bg-info-soft text-info"
                  >
                    <WayIcon weight="bold" className="size-6" />
                  </span>
                  <span className="flex flex-1 flex-col">
                    <span className="font-black">{title}</span>{' '}
                    <span className="text-[0.8125rem] leading-snug font-bold text-ink-muted">
                      {text}
                    </span>
                  </span>
                  <CaretRight
                    weight="bold"
                    aria-hidden
                    className="size-5 shrink-0 text-ink-muted"
                  />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </section>
  )
}
