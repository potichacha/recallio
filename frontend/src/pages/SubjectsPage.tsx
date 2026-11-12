import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
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

/**
 * Toutes les cartes, sujet par sujet (ADR 0052) : le catalogue, ouvert depuis la page Cours, pour parcourir les thèmes
 * et les cartes d'un sujet. Les sujets sont regroupés par catégorie ; une catégorie vide n'est pas affichée.
 */
export function SubjectsPage() {
  const subjects = useSubjects()

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Toutes les cartes</h1>
        <p className="text-ink-muted">Les cartes de chaque sujet, thème par thème.</p>
      </header>
      {subjects.isPending && <LoadingState label="Chargement des sujets…" shape="tiles" />}
      {subjects.isError && (
        <ErrorState
          message="Impossible de charger les sujets."
          onRetry={() => void subjects.refetch()}
        />
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
