import type { SubjectCategory } from '../api/content'

/** Les catégories du catalogue (CLAUDE.md, section 2.5), dans l'ordre d'affichage. */
export const SUBJECT_CATEGORIES: { category: SubjectCategory; label: string }[] = [
  { category: 'LANGUAGES', label: 'Langages' },
  { category: 'FRAMEWORKS', label: 'Frameworks' },
  { category: 'CLOUD_DEVOPS', label: 'Cloud & DevOps' },
  { category: 'DATA', label: 'Données' },
  { category: 'FUNDAMENTALS', label: 'Fondamentaux' },
  { category: 'AI', label: 'IA' },
]

export function categoryLabel(category: SubjectCategory) {
  return SUBJECT_CATEGORIES.find((entry) => entry.category === category)?.label ?? category
}
