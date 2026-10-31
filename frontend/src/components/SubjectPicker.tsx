import { useSubjects } from '../api/content'
import { SUBJECT_CATEGORIES } from './subjectCategories'

/**
 * Cases à cocher des sujets publiés, regroupés par catégorie. Les brouillons (visibles des admins) ne sont pas proposés :
 * on ne peut suivre qu'un sujet publié.
 */
export function SubjectPicker({
  selected,
  onChange,
}: {
  selected: string[]
  onChange: (selected: string[]) => void
}) {
  const subjects = useSubjects()

  if (subjects.isPending) return <p>Chargement des sujets…</p>
  if (subjects.isError) return <p role="alert">Impossible de charger les sujets.</p>

  const published = subjects.data.filter((subject) => subject.status === 'PUBLISHED')
  const toggle = (slug: string, checked: boolean) =>
    onChange(checked ? [...selected, slug] : selected.filter((other) => other !== slug))

  return (
    <div className="flex flex-col gap-4">
      {SUBJECT_CATEGORIES.map(({ category, label }) => {
        const inCategory = published.filter((subject) => subject.category === category)
        if (inCategory.length === 0) return null
        return (
          <fieldset key={category} className="flex flex-col gap-2">
            <legend className="mb-1 text-sm font-semibold text-slate-600">{label}</legend>
            {inCategory.map((subject) => (
              <label
                key={subject.slug}
                className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 has-checked:border-indigo-400"
              >
                <input
                  type="checkbox"
                  checked={selected.includes(subject.slug)}
                  onChange={(event) => toggle(subject.slug, event.target.checked)}
                  className="size-4 accent-indigo-600"
                />
                <span className="font-medium">{subject.title}</span>
                <span className="ml-auto text-sm text-slate-500">{subject.cardCount} cartes</span>
              </label>
            ))}
          </fieldset>
        )
      })}
    </div>
  )
}
