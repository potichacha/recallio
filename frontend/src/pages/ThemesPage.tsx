import { Link } from 'react-router'
import { useThemes } from '../api/content'

export function ThemesPage() {
  const themes = useThemes()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Thèmes</h1>
      {themes.isPending && <p>Chargement des thèmes…</p>}
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
