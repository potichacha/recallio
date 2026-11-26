import type { ThemeStats } from '../api/stats'
import type { TourLastPage, TourOutline } from '../api/tour'
import { firstStepToDiscover, tourPath, tourSteps } from '../tour/tourSteps'

/**
 * Ce que la carte « Apprendre » de l'accueil montre d'un sujet (ADR 0077) : le chapitre où l'on en est, ce qui y est
 * fait, et où mène « Continuer ». Le vrai compte des leçons arrive avec le chemin (étape 3 de la refonte) ; d'ici là,
 * une leçon est une notion du Tour, et un sujet sans Tour compte ses chapitres.
 */
export type LearnStep = {
  /** Où mène le bouton de la carte. */
  to: string
  /** Le chapitre où l'on en est. */
  chapter: string
  /** Ce qui est fait, sur ce qu'il y a à faire : des leçons d'un chapitre du Tour, ou des chapitres d'un sujet. */
  done: number
  total: number
  unit: 'lesson' | 'chapter'
  /** La leçon que le bouton ouvre, quand c'en est une. */
  next?: string
  /** Faux tant que rien n'a été ouvert ni validé : le bouton dit « Commencer ». */
  started: boolean
}

type TourProgress = {
  validatedNotions: string[]
  knownNotions?: string[]
  lastPage?: TourLastPage | null
}

/**
 * L'étape d'un sujet qui a un Tour : la page où l'on s'était arrêté, là où mène « Reprendre le Tour », sinon la
 * première notion à découvrir. Le chapitre est celui de cette page ; ses leçons faites sont ses notions validées dans
 * le Tour, ou connues au test de niveau (ADR 0068). Rien si le Tour est vide.
 */
export function tourLearnStep(
  subject: string,
  outline: TourOutline,
  progress: TourProgress,
): LearnStep | undefined {
  const validated = new Set(progress.validatedNotions)
  const known = new Set(progress.knownNotions)
  const chapters = outline.domains.flatMap((domain) => domain.chapters)
  const inTour = (notion: string) =>
    chapters.some((chapter) => chapter.notions.some((entry) => entry.slug === notion))

  const lastPage = progress.lastPage && inTour(progress.lastPage.notion) ? progress.lastPage : null
  const step = lastPage ?? firstStepToDiscover(outline, validated, known) ?? tourSteps(outline)[0]
  const chapter = chapters.find((candidate) =>
    candidate.notions.some((entry) => entry.slug === step?.notion),
  )
  if (!step || !chapter) return undefined
  return {
    to: tourPath(subject, step),
    chapter: chapter.title,
    done: chapter.notions.filter((entry) => validated.has(entry.slug) || known.has(entry.slug))
      .length,
    total: chapter.notions.length,
    unit: 'lesson',
    next: chapter.notions.find((entry) => entry.slug === step.notion)?.title,
    started: lastPage !== null || validated.size > 0,
  }
}

/**
 * L'étape d'un sujet sans Tour : son cours, et le premier chapitre dont toutes les cartes n'ont pas été vues. Ce qui
 * est fait, ce sont les chapitres entièrement vus. Rien si le sujet n'a pas de carte.
 */
export function courseLearnStep(subject: string, themes: ThemeStats[]): LearnStep | undefined {
  const withCards = themes.filter((theme) => theme.counts.total > 0)
  if (withCards.length === 0) return undefined
  const finished = withCards.filter((theme) => theme.counts.seen >= theme.counts.total)
  const current =
    withCards.find((theme) => theme.counts.seen < theme.counts.total) ??
    withCards[withCards.length - 1]
  return {
    to: `/subjects/${encodeURIComponent(subject)}/course`,
    chapter: current.title,
    done: finished.length,
    total: withCards.length,
    unit: 'chapter',
    started: withCards.some((theme) => theme.counts.seen > 0),
  }
}
