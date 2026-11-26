import { ArrowClockwise, CaretRight } from '@phosphor-icons/react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useCatalogSubjects, type Subject } from '../api/content'
import { useReviewStats } from '../api/stats'
import { useSession } from '../auth/session'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { SUBJECT_CATEGORIES } from '../components/subjectCategories'
import { card } from '../components/ui/card'
import { DoorBanner } from '../components/ui/DoorBanner'
import { heading } from '../components/ui/typography'
import { SubjectLogo } from './SubjectLogo'
import { useIsAdmin } from './useIsAdmin'

const ACTION =
  'inline-flex min-h-11 items-center text-[0.9375rem] font-black text-brand-strong underline'

/** Ce qu'un sujet ouvre depuis la porte : son Tour s'il en a un, son cours s'il a des cartes, et ses cartes par thème. */
function actions(subject: Subject): { to: string; label: string; name: string }[] {
  const base = `/subjects/${encodeURIComponent(subject.slug)}`
  return [
    ...(subject.hasTour
      ? [{ to: `${base}/tour`, label: 'Le Tour', name: `Le Tour de ${subject.title}` }]
      : []),
    ...(subject.cardCount > 0
      ? [{ to: `${base}/course`, label: 'Le cours', name: `Le cours de ${subject.title}` }]
      : []),
    { to: base, label: 'Les cartes', name: `Les cartes de ${subject.title}` },
  ]
}

/**
 * Les révisions du jour, en tête de la porte : la session de révision n'a plus son onglet (ADR 0077), on y entre par
 * ici et par l'accueil. Connecté, la ligne dit combien de cartes attendent ; un visiteur est invité à se connecter par
 * la page elle-même.
 */
function ReviewsOfTheDay() {
  const signedIn = useSession().status === 'authenticated'
  const stats = useReviewStats(signedIn)
  const due = stats.data?.overall.dueNow
  const fresh = stats.data?.newToday ?? 0
  const detail =
    due === undefined
      ? 'Les cartes à revoir aujourd’hui, pour ne rien oublier'
      : due > 0
        ? `${due} carte${due > 1 ? 's' : ''} à revoir`
        : fresh > 0
          ? `Rien à revoir, ${fresh} nouvelle${fresh > 1 ? 's' : ''} carte${fresh > 1 ? 's' : ''} à découvrir`
          : 'Tout est à jour pour aujourd’hui'
  return (
    <Link
      to="/review"
      className="flex items-center gap-3 rounded-xl bg-surface px-3.5 py-2.5 shadow-float motion-safe:transition-transform motion-safe:duration-75 motion-safe:active:translate-y-1"
    >
      <span
        aria-hidden="true"
        className="flex size-10 shrink-0 items-center justify-center rounded-[0.8125rem] bg-success-soft text-success-strong"
      >
        <ArrowClockwise weight="bold" className="size-6" />
      </span>
      <span className="flex flex-1 flex-col">
        <span className="font-black">Révisions du jour</span>{' '}
        <span className="text-[0.8125rem] font-bold text-ink-muted">{detail}</span>
      </span>
      <CaretRight weight="bold" aria-hidden className="size-5 shrink-0 text-ink-muted" />
    </Link>
  )
}

/**
 * Apprendre (ADR 0077), le point d'entrée de la porte : les révisions du jour, puis les sujets par catégorie, chacun
 * menant à son Tour, à son cours (qui est aussi son parcours, sa carte des compétences) et à ses cartes. Un sujet
 * s'ouvre s'il est publié, ou pour un admin qui relit un brouillon ; les autres sont annoncés en préparation
 * (ADR 0052). Les pages « Le Tour » et « Sujets » restent, et s'atteignent d'ici.
 */
export function LearnPage() {
  const subjects = useCatalogSubjects()
  const admin = useIsAdmin()
  const [touched, setTouched] = useState<Subject>()

  return (
    <section className="flex flex-col gap-5">
      <DoorBanner door="learn" eyebrow="Apprendre" title="Tes sujets, pas à pas">
        <p className="max-w-xs text-[0.9375rem] leading-[1.4] font-bold text-brand-soft">
          Des leçons courtes : tu lis, tu exécutes, tu valides.
        </p>
      </DoorBanner>
      <ReviewsOfTheDay />
      {/*
        La silhouette réserve la hauteur de l'écran : les liens du bas et le pied de page restent sous le pli tant que
        les sujets chargent, au lieu de sauter quand la liste arrive (la stabilité est mesurée par `speed.spec.ts`).
      */}
      {subjects.isPending && (
        <div className="min-h-dvh">
          <LoadingState label="Chargement des sujets…" shape="tiles" />
        </div>
      )}
      {subjects.isError && (
        <ErrorState
          message="Impossible de charger les sujets."
          onRetry={() => void subjects.refetch()}
        />
      )}
      {/* Annonce, pour un lecteur d'écran, le sujet en préparation qu'on vient de toucher. */}
      <p role="status" className="sr-only">
        {touched && `${touched.title} : work in progress, ce sujet arrive bientôt.`}
      </p>
      {SUBJECT_CATEGORIES.map(({ category, label }) => {
        const inCategory = (subjects.data ?? []).filter((subject) => subject.category === category)
        if (inCategory.length === 0) return null
        return (
          <section
            key={category}
            aria-labelledby={`learn-${category}`}
            className="flex flex-col gap-3"
          >
            <h2 id={`learn-${category}`} className={heading({ size: 'md' })}>
              {label}
            </h2>
            <ul className="grid gap-3 sm:grid-cols-2">
              {inCategory.map((subject) => (
                <li key={subject.slug} className="min-w-0">
                  {subject.status === 'PUBLISHED' || admin ? (
                    <div className={`flex h-full items-center gap-3 ${card()} py-3`}>
                      <SubjectLogo subject={subject} />
                      <div className="flex min-w-0 flex-col">
                        <h3 className="font-black wrap-anywhere">{subject.title}</h3>
                        <p className="flex flex-wrap gap-x-4">
                          {actions(subject).map(({ to, label: text, name }) => (
                            <Link key={to} to={to} aria-label={name} className={ACTION}>
                              {text}
                            </Link>
                          ))}
                        </p>
                      </div>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setTouched(subject)}
                      className="flex h-full w-full items-center gap-3 rounded-2xl border-2 border-dashed border-line-strong bg-surface-muted px-4.5 py-3 text-left"
                    >
                      <span className="opacity-60 grayscale">
                        <SubjectLogo subject={subject} />
                      </span>
                      <span className="flex min-w-0 flex-col">
                        <span className="font-black wrap-anywhere text-ink-soft">
                          {subject.title}
                        </span>{' '}
                        <span className="text-sm font-bold text-ink-muted">
                          {touched?.slug === subject.slug ? 'Work in progress' : 'En préparation'}
                        </span>
                      </span>
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </section>
        )
      })}
      <nav aria-label="Aussi dans Apprendre" className="flex flex-col items-start">
        <Link to="/tours" className={ACTION}>
          Tous les Tours, langage par langage
        </Link>
        <Link to="/subjects" className={ACTION}>
          Parcourir toutes les cartes, thème par thème
        </Link>
      </nav>
    </section>
  )
}
