import { BookOpen, House, Lightning, Trophy, type Icon } from '@phosphor-icons/react'

/**
 * Les quatre destinations de l'application (ADR 0077, qui révise l'ADR 0052) : l'accueil et les trois portes. Le profil
 * est à part : il s'ouvre par l'avatar.
 */
export const DESTINATIONS: { to: string; label: string; icon: Icon }[] = [
  { to: '/', label: 'Accueil', icon: House },
  { to: '/learn', label: 'Apprendre', icon: BookOpen },
  { to: '/practice', label: 'S’entraîner', icon: Lightning },
  { to: '/play', label: 'Jouer', icon: Trophy },
]

/** Les pages d'un sujet rangées sous « S'entraîner » : l'entraînement d'une notion, l'examen blanc, le carnet. */
const PRACTICE_IN_SUBJECT = /^\/subjects\/[^/]+\/(practice|exam|mistakes)(\/|$)/
const PLAY_PAGES = ['/play', '/daily', '/ranked', '/groups', '/challenges', '/live', '/join']
/** Le catalogue, le cours, le Tour, le test de niveau et les révisions du jour sont sous « Apprendre ». */
const LEARN_PAGES = ['/learn', '/tours', '/review', '/subjects', '/themes']

const under = (path: string, pages: string[]) =>
  pages.some((page) => path === page || path.startsWith(`${page}/`))

/**
 * La destination qui reste allumée sur une page : chaque écran est rangé sous sa porte (ADR 0077). Le profil, les pages
 * légales et l'essai n'en ont pas.
 */
export function destinationOf(path: string): string | undefined {
  if (path === '/' || under(path, ['/progress'])) return '/'
  if (under(path, ['/practice']) || PRACTICE_IN_SUBJECT.test(path)) return '/practice'
  if (under(path, PLAY_PAGES)) return '/play'
  if (under(path, LEARN_PAGES)) return '/learn'
  return undefined
}
