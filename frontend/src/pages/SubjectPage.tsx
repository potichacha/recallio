import { Link, useParams } from 'react-router'
import { useSubject, useSubjectThemes } from '../api/content'
import { ApiError } from '../api/errors'
import { useTour } from '../api/tour'
import { DraftBadge } from '../components/DraftBadge'
import { categoryLabel } from '../components/subjectCategories'
import { button } from '../components/ui/button'

/**
 * Un sujet : ses thèmes (les chapitres de son document), dans l'ordre du document, et son Tour s'il en a un
 * (ADR 0037).
 */
export function SubjectPage() {
  const { slug = '' } = useParams()
  const subject = useSubject(slug)
  const themes = useSubjectThemes(slug)
  const tour = useTour(slug)
  const hasTour = (tour.data?.domains.length ?? 0) > 0

  if (subject.error instanceof ApiError && subject.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Sujet introuvable</h1>
        <Link to="/subjects" className="text-brand hover:underline">
          Retour aux sujets
        </Link>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <Link to="/subjects" className="text-sm text-brand hover:underline">
        ← Tous les sujets
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col">
          {subject.data && (
            <span className="text-sm font-medium text-brand">
              {categoryLabel(subject.data.category)}
            </span>
          )}
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            {subject.data?.title ?? 'Chargement…'}
            {subject.data?.status === 'DRAFT' && <DraftBadge />}
          </h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {hasTour && (
            <Link
              to={`/subjects/${encodeURIComponent(slug)}/tour`}
              className={button({ variant: 'secondary', size: 'sm' })}
            >
              Le Tour
            </Link>
          )}
          <Link
            to={`/subjects/${encodeURIComponent(slug)}/course`}
            className={button({ variant: 'secondary', size: 'sm' })}
          >
            Le cours
          </Link>
          <Link
            to={`/review?subject=${encodeURIComponent(slug)}`}
            className={button({ size: 'sm' })}
          >
            Réviser ce sujet
          </Link>
        </div>
      </div>
      {themes.isError && <p role="alert">Impossible de charger les thèmes.</p>}
      {themes.isSuccess && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {themes.data.map((theme) => (
            <li key={theme.id}>
              <Link
                to={`/themes/${theme.slug}`}
                className="flex h-full flex-col gap-1 rounded-xl border border-line bg-surface p-4 shadow-sm hover:border-brand-line"
              >
                <span className="font-semibold">{theme.title}</span>
                <span className="text-sm text-ink-muted">
                  {theme.cardCount} carte{theme.cardCount > 1 ? 's' : ''}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
