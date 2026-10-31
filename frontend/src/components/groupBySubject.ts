type BySubject = { subjectSlug: string; subjectTitle: string }

export type SubjectGroup<T> = { slug: string; title: string; items: T[] }

/** Regroupe des éléments par sujet, en gardant l'ordre reçu (l'API trie par sujet, puis dans l'ordre du document). */
export function groupBySubject<T extends BySubject>(items: T[]): SubjectGroup<T>[] {
  const groups: SubjectGroup<T>[] = []
  for (const item of items) {
    const last = groups.at(-1)
    if (last?.slug === item.subjectSlug) {
      last.items.push(item)
    } else {
      groups.push({ slug: item.subjectSlug, title: item.subjectTitle, items: [item] })
    }
  }
  return groups
}
