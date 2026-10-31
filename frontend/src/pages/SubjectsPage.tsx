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
      className="flex h-full flex-col gap-1 rounded-xl border border-slate-200 bg-white p-4 shadow-sm hover:border-indigo-300"
    >
      <span className="flex items-center gap-2 font-semibold">
        {subject.title}
        {subject.status === 'DRAFT' && <DraftBadge />}
      </span>
      <span className="text-sm text-slate-500">
        {plural(subject.themeCount, 'thème')} · {plural(subject.cardCount, 'carte')}
      </span>
    </Link>
  )
}

/** Le catalogue : les sujets (Java, Go...), regroupés par catégorie. Une catégorie vide n'est pas affichée. */
export function SubjectsPage() {
  const subjects = useSubjects()

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Sujets</h1>
      {subjects.isPending && <p>Chargement des sujets…</p>}
      {subjects.isError && <p role="alert">Impossible de charger les sujets.</p>}
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
