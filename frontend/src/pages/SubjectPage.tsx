import { Link, useParams } from 'react-router'
import { useSubject, useSubjectThemes } from '../api/content'
import { ApiError } from '../api/errors'
import { DraftBadge } from '../components/DraftBadge'
import { categoryLabel } from '../components/subjectCategories'

/** Un sujet : ses thèmes (les chapitres de son document), dans l'ordre du document. */
export function SubjectPage() {
  const { slug = '' } = useParams()
  const subject = useSubject(slug)
  const themes = useSubjectThemes(slug)

  if (subject.error instanceof ApiError && subject.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Sujet introuvable</h1>
        <Link to="/subjects" className="text-indigo-600 hover:underline">
          Retour aux sujets
        </Link>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <Link to="/subjects" className="text-sm text-indigo-600 hover:underline">
        ← Tous les sujets
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col">
          {subject.data && (
            <span className="text-sm font-medium text-indigo-600">
              {categoryLabel(subject.data.category)}
            </span>
          )}
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            {subject.data?.title ?? 'Chargement…'}
            {subject.data?.status === 'DRAFT' && <DraftBadge />}
          </h1>
        </div>
        <Link
          to={`/review?subject=${encodeURIComponent(slug)}`}
          className="rounded-lg bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700"
        >
          Réviser ce sujet
        </Link>
      </div>
      {themes.isError && <p role="alert">Impossible de charger les thèmes.</p>}
      {themes.isSuccess && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {themes.data.map((theme) => (
            <li key={theme.id}>
              <Link
                to={`/themes/${theme.slug}`}
                className="flex h-full flex-col gap-1 rounded-xl border border-slate-200 bg-white p-4 shadow-sm hover:border-indigo-300"
              >
                <span className="font-semibold">{theme.title}</span>
                <span className="text-sm text-slate-500">
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
