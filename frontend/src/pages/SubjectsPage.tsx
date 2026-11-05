import { Path } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { useSubjects, type Subject } from '../api/content'
import { DraftBadge } from '../components/DraftBadge'
import { SUBJECT_CATEGORIES } from '../components/subjectCategories'

function plural(count: number, word: string) {
  return `${count} ${word}${count > 1 ? 's' : ''}`
}

function SubjectLink({ subject }: { subject: Subject }) {
  return (
    <Link
      to={`/subjects/${subject.slug}`}
      className="flex h-full flex-col gap-1 rounded-xl border border-line bg-surface p-4 shadow-sm hover:border-brand-line"
    >
      <span className="flex items-center gap-2 font-semibold">
        {subject.title}
        {subject.status === 'DRAFT' && <DraftBadge />}
      </span>
      <span className="text-sm text-ink-muted">
        {plural(subject.themeCount, 'thème')} · {plural(subject.cardCount, 'carte')}
      </span>
    </Link>
  )
}

/** Un Tour à suivre : apprendre un langage page par page, en exécutant chaque exemple (ADR 0037). */
function TourLink({ subject }: { subject: Subject }) {
  return (
    <Link
      to={`/subjects/${subject.slug}/tour`}
      className="flex h-full items-center gap-3 rounded-xl bg-brand-soft p-4 hover:bg-brand-line"
    >
      <Path weight="bold" aria-hidden className="size-7 shrink-0 text-brand" />
      <span className="flex flex-col gap-0.5">
        <span className="flex items-center gap-2 font-semibold">
          Le Tour de {subject.title}
          {subject.status === 'DRAFT' && <DraftBadge />}
        </span>{' '}
        <span className="text-sm text-ink-soft">Pas à pas, en codant à chaque page.</span>
      </span>
    </Link>
  )
}

/**
 * Apprendre (ADR 0050) : les Tours d'abord, pour découvrir un langage, puis le catalogue des sujets (Java, Go...),
 * regroupés par catégorie. Une catégorie vide n'est pas affichée.
 */
export function SubjectsPage() {
  const subjects = useSubjects()
  const tours = subjects.data?.filter((subject) => subject.hasTour) ?? []

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Apprendre</h1>
        <p className="text-ink-muted">
          Découvre un langage dans son Tour, ou choisis un sujet pour réviser ses cartes.
        </p>
      </header>
      {subjects.isPending && <p>Chargement des sujets…</p>}
      {subjects.isError && <p role="alert">Impossible de charger les sujets.</p>}
      {tours.length > 0 && (
        <section aria-labelledby="tours-title" className="flex flex-col gap-3">
          <h2 id="tours-title" className="text-xl font-semibold">
            Le Tour
          </h2>
          <ul className="grid gap-3 sm:grid-cols-2">
            {tours.map((subject) => (
              <li key={subject.id}>
                <TourLink subject={subject} />
              </li>
            ))}
          </ul>
        </section>
      )}
      {subjects.isSuccess &&
        SUBJECT_CATEGORIES.map(({ category, label }) => {
          const inCategory = subjects.data.filter((subject) => subject.category === category)
          if (inCategory.length === 0) return null
          return (
            <section
              key={category}
              aria-labelledby={`category-${category}`}
              className="flex flex-col gap-3"
            >
              <h2 id={`category-${category}`} className="text-xl font-semibold">
                {label}
              </h2>
              <ul className="grid gap-3 sm:grid-cols-2">
                {inCategory.map((subject) => (
                  <li key={subject.id}>
                    <SubjectLink subject={subject} />
                  </li>
                ))}
              </ul>
            </section>
          )
        })}
    </section>
  )
}
