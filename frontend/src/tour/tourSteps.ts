import type { TourOutline } from '../api/tour'

/** Une page à sa place dans tout le Tour : sa notion, et son rang dans la notion, à partir de 1. */
export type TourStep = { notion: string; page: number }

/** Les pages du Tour, dans l'ordre du sommaire : une notion en compte autant que son `pageCount`. */
export function tourSteps(outline: TourOutline): TourStep[] {
  return outline.domains.flatMap((domain) =>
    domain.chapters.flatMap((chapter) =>
      chapter.notions.flatMap((notion) =>
        Array.from({ length: notion.pageCount }, (_, index) => ({
          notion: notion.slug,
          page: index + 1,
        })),
      ),
    ),
  )
}

/**
 * La première page de la première notion à découvrir : ni validée, ni connue au test de niveau (ADR 0068), qui saute
 * les bases qu'on connaît. Indéfinie si toutes le sont, ou si le Tour est vide.
 */
export function firstStepToDiscover(
  outline: TourOutline,
  validated: ReadonlySet<string>,
  known: ReadonlySet<string>,
): TourStep | undefined {
  return tourSteps(outline).find(
    (step) => step.page === 1 && !validated.has(step.notion) && !known.has(step.notion),
  )
}

/** Le lien d'une page : `/subjects/go/tour/hello-world`, avec `?page=2` au-delà de la première. */
export function tourPath(subject: string, step: TourStep): string {
  const base = `/subjects/${encodeURIComponent(subject)}/tour/${encodeURIComponent(step.notion)}`
  return step.page > 1 ? `${base}?page=${step.page}` : base
}

/**
 * La place d'une page dans le Tour, et ses voisines. `index` vaut -1 tant que le sommaire n'est pas chargé, ou si la
 * page n'y est pas.
 */
export function neighbours(steps: TourStep[], current: TourStep) {
  const index = steps.findIndex(
    (step) => step.notion === current.notion && step.page === current.page,
  )
  return {
    index,
    previous: index > 0 ? steps[index - 1] : undefined,
    next: index >= 0 && index < steps.length - 1 ? steps[index + 1] : undefined,
  }
}
