import { BookOpenText } from '@phosphor-icons/react'
import { Link } from 'react-router'

/**
 * « Revoir la leçon », sous une réponse fausse (audit de novembre 2026, élément 35) : la leçon de la notion, dans un
 * nouvel onglet pour ne pas perdre la session en cours.
 */
export function LessonLink({ subject, notion }: { subject: string; notion: string }) {
  return (
    <Link
      to={`/subjects/${encodeURIComponent(subject)}/course/${encodeURIComponent(notion)}`}
      target="_blank"
      rel="noopener"
      className="inline-flex items-center gap-1 self-start text-sm font-medium text-brand-strong hover:underline"
    >
      <BookOpenText weight="bold" aria-hidden className="size-4" />
      Revoir la leçon
      <span className="sr-only"> (nouvel onglet)</span>
    </Link>
  )
}
