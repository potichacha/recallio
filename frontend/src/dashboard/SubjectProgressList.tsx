import { Link } from 'react-router'
import type { CardCounts, SubjectStats, ThemeStats } from '../api/stats'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Progression par sujet suivi, dans l'ordre du catalogue, puis par thème dans chaque sujet (dépliable). Avec un seul
 * sujet, ses thèmes sont dépliés d'emblée.
 */
export function SubjectProgressList({ subjects }: { subjects: SubjectStats[] }) {
  return (
    <section aria-labelledby="subjects-progress-title" className="flex flex-col gap-3">
      <h2 id="subjects-progress-title" className="text-lg font-semibold">
        Progression par sujet
      </h2>
      <ul className="flex flex-col gap-3">
        {subjects.map((subject) => (
          <SubjectRow key={subject.slug} subject={subject} expanded={subjects.length === 1} />
        ))}
      </ul>
    </section>
  )
}

function SubjectRow({ subject, expanded }: { subject: SubjectStats; expanded: boolean }) {
  // Les thèmes commencés d'abord, puis les autres, chacun dans l'ordre du document.
  const started = subject.themes.filter((theme) => theme.counts.seen > 0)
  const notStarted = subject.themes.filter(
    (theme) => theme.counts.seen === 0 && theme.counts.total > 0,
  )

  return (
    <li className="flex flex-col gap-3 rounded-xl bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <Link
          to={`/subjects/${subject.slug}`}
          className="text-lg font-semibold hover:text-indigo-600"
        >
          {subject.title}
        </Link>
        <DueLink
          counts={subject.counts}
          to={`/review?subject=${encodeURIComponent(subject.slug)}`}
        />
      </div>
      <Progress label={subject.title} counts={subject.counts} />
      <details open={expanded} className="group">
        <summary className="cursor-pointer text-sm font-medium text-indigo-700">
          {plural(subject.themes.length, 'thème')}
          {started.length > 0 && ` · ${plural(started.length, 'commencé')}`}
        </summary>
        <ul aria-label={`Thèmes ${subject.title}`} className="mt-3 flex flex-col gap-2">
          {[...started, ...notStarted].map((theme) => (
            <ThemeRow key={theme.slug} theme={theme} />
          ))}
        </ul>
      </details>
    </li>
  )
}

function ThemeRow({ theme }: { theme: ThemeStats }) {
  return (
    <li className="flex flex-col gap-2 rounded-lg border border-slate-100 p-3">
      <div className="flex items-center justify-between gap-3">
        <Link to={`/themes/${theme.slug}`} className="font-medium hover:text-indigo-600">
          {theme.title}
        </Link>
        <DueLink counts={theme.counts} to={`/review?theme=${encodeURIComponent(theme.slug)}`} />
      </div>
      <Progress label={theme.title} counts={theme.counts} />
    </li>
  )
}

function DueLink({ counts, to }: { counts: CardCounts; to: string }) {
  if (counts.dueNow === 0) return null
  return (
    <Link
      to={to}
      className="shrink-0 rounded-lg bg-indigo-50 px-2.5 py-1 text-sm font-medium text-indigo-700 hover:bg-indigo-100"
    >
      {counts.dueNow} à revoir
    </Link>
  )
}

function Progress({ label, counts }: { label: string; counts: CardCounts }) {
  const { total, seen, mastered } = counts
  return (
    <div className="flex flex-col gap-1">
      <progress
        value={seen}
        max={total}
        aria-label={`${label} : ${seen} cartes vues sur ${total}`}
        className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-indigo-500 [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:bg-indigo-500"
      />
      <p className="text-xs text-slate-500">
        {seen} / {total} vues · {plural(mastered, 'maîtrisée')}
      </p>
    </div>
  )
}
