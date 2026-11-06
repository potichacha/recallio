import {
  Brain,
  Cloud,
  Code,
  Database,
  GraduationCap,
  ShieldCheck,
  Stack,
  type Icon,
} from '@phosphor-icons/react'
import type { SubjectCategory } from '../api/content'

/** Les sujets qui ont un logo dans `public/logos` (voir son README) : celui de leur langage ou de leur outil. */
const SUBJECT_LOGOS: ReadonlySet<string> = new Set([
  'java',
  'go',
  'python',
  'typescript',
  'kotlin',
  'csharp',
  'c',
  'cpp',
  'rust',
  'spring-boot',
  'quarkus',
  'jpa-hibernate',
  'sql',
  'kafka',
  'docker',
  'kubernetes',
  'git',
])

/**
 * Un sujet sans logo prend l'icône de sa catégorie. Une icône par catégorie, pas par sujet : chaque icône Phosphor pèse
 * dans le JavaScript de l'ouverture, que son budget limite (ADR 0050).
 */
const CATEGORY_ICONS: Record<SubjectCategory, Icon> = {
  LANGUAGES: Code,
  FRAMEWORKS: Stack,
  CLOUD_DEVOPS: Cloud,
  DATA: Database,
  SECURITY: ShieldCheck,
  FUNDAMENTALS: GraduationCap,
  AI: Brain,
}

/** L'adresse du logo d'un sujet, s'il en a un. */
export function subjectLogo(slug: string): string | undefined {
  return SUBJECT_LOGOS.has(slug) ? `/logos/${slug}.svg` : undefined
}

/** L'icône d'un sujet sans logo : celle de sa catégorie. */
export function categoryIcon(category: SubjectCategory): Icon {
  return CATEGORY_ICONS[category]
}
