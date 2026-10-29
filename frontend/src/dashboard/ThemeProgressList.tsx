import { Link } from 'react-router'
import type { ThemeStats } from '../api/stats'

/** Progression par thème : les thèmes commencés d'abord, puis les autres, dans l'ordre du programme. */
export function ThemeProgressList({ themes }: { themes: ThemeStats[] }) {
  const started = themes.filter((theme) => theme.counts.seen > 0)
  const notStarted = themes.filter((theme) => theme.counts.seen === 0 && theme.counts.total > 0)

  return (
    <section aria-labelledby="themes-title" className="flex flex-col gap-3">
      <h2 id="themes-title" className="text-lg font-semibold">
        Progression par thème
      </h2>
      {started.length === 0 && (
        <p className="text-slate-600">Aucun thème commencé : lancez une révision pour démarrer.</p>
      )}
      <ul className="flex flex-col gap-2">
        {[...started, ...notStarted].map((theme) => (
          <ThemeRow key={theme.slug} theme={theme} />
        ))}
      </ul>
    </section>
  )
}

function ThemeRow({ theme }: { theme: ThemeStats }) {
  const { total, seen, mastered, dueNow } = theme.counts
  return (
    <li className="flex flex-col gap-2 rounded-xl bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <Link to={`/themes/${theme.slug}`} className="font-medium hover:text-indigo-600">
          {theme.title}
        </Link>
        {dueNow > 0 && (
          <Link
            to={`/review?theme=${encodeURIComponent(theme.slug)}`}
            className="shrink-0 rounded-lg bg-indigo-50 px-2.5 py-1 text-sm font-medium text-indigo-700 hover:bg-indigo-100"
          >
            {dueNow} à revoir
          </Link>
        )}
      </div>
      <progress
        value={seen}
        max={total}
        aria-label={`${theme.title} : ${seen} cartes vues sur ${total}`}
        className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-indigo-500 [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:bg-indigo-500"
      />
      <p className="text-xs text-slate-500">
        {seen} / {total} vues · {mastered} maîtrisée{mastered > 1 ? 's' : ''}
      </p>
    </li>
  )
}
