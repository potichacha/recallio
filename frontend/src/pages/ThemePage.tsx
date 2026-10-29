import { Link, useParams, useSearchParams } from 'react-router'
import { useTheme, useThemeCards } from '../api/content'
import { ApiError } from '../api/errors'
import { CardItem } from './CardItem'

export function ThemePage() {
  const { slug = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Math.max(0, Number(searchParams.get('page') ?? 0) || 0)
  const theme = useTheme(slug)
  const cards = useThemeCards(slug, page)

  if (theme.error instanceof ApiError && theme.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Thème introuvable</h1>
        <Link to="/themes" className="text-indigo-600 hover:underline">
          Retour aux thèmes
        </Link>
      </section>
    )
  }

  const goTo = (target: number) => setSearchParams(target === 0 ? {} : { page: String(target) })

  return (
    <section className="flex flex-col gap-4">
      <Link to="/themes" className="text-sm text-indigo-600 hover:underline">
        ← Tous les thèmes
      </Link>
      <h1 className="text-2xl font-bold">{theme.data?.title ?? 'Chargement…'}</h1>
      {cards.isError && <p role="alert">Impossible de charger les cartes.</p>}
      {cards.isSuccess && (
        <>
          <ol className="flex flex-col gap-4">
            {cards.data.items.map((card) => (
              <li key={card.id}>
                <CardItem card={card} />
              </li>
            ))}
          </ol>
          {cards.data.totalPages > 1 && (
            <nav aria-label="Pagination" className="flex items-center justify-between">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => goTo(page - 1)}
                className="rounded-lg px-3 py-1.5 text-indigo-700 disabled:text-slate-300"
              >
                Précédent
              </button>
              <span className="text-sm text-slate-500">
                Page {page + 1} / {cards.data.totalPages}
              </span>
              <button
                type="button"
                disabled={page + 1 >= cards.data.totalPages}
                onClick={() => goTo(page + 1)}
                className="rounded-lg px-3 py-1.5 text-indigo-700 disabled:text-slate-300"
              >
                Suivant
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
