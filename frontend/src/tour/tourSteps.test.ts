import { describe, expect, it } from 'vitest'
import type { TourOutline } from '../api/tour'
import { neighbours, tourPath, tourSteps } from './tourSteps'

const OUTLINE: TourOutline = {
  subjectSlug: 'go',
  subjectTitle: 'Go',
  domains: [
    {
      id: 'bases',
      title: 'Bases du langage',
      chapters: [
        {
          id: 'premiers-pas',
          title: 'Premiers pas',
          notions: [
            { slug: 'hello-world', title: 'Le premier programme', pageCount: 1 },
            { slug: 'packages-imports', title: 'Packages et imports', pageCount: 2 },
          ],
        },
      ],
    },
    {
      id: 'concurrence',
      title: 'Concurrence',
      chapters: [
        {
          id: 'goroutines',
          title: 'Goroutines',
          notions: [{ slug: 'goroutines', title: 'Les goroutines', pageCount: 1 }],
        },
      ],
    },
  ],
}

describe('tourSteps', () => {
  it('lists every page of the Tour in the order of its outline', () => {
    expect(tourSteps(OUTLINE)).toEqual([
      { notion: 'hello-world', page: 1 },
      { notion: 'packages-imports', page: 1 },
      { notion: 'packages-imports', page: 2 },
      { notion: 'goroutines', page: 1 },
    ])
  })

  it('finds the previous and the next page, across notions and domains', () => {
    const steps = tourSteps(OUTLINE)

    expect(neighbours(steps, { notion: 'packages-imports', page: 2 })).toEqual({
      index: 2,
      previous: { notion: 'packages-imports', page: 1 },
      next: { notion: 'goroutines', page: 1 },
    })
    expect(neighbours(steps, { notion: 'hello-world', page: 1 }).previous).toBeUndefined()
    expect(neighbours(steps, { notion: 'goroutines', page: 1 }).next).toBeUndefined()
    expect(neighbours(steps, { notion: 'inconnue', page: 1 }).index).toBe(-1)
  })

  it('links to a page, with its rank beyond the first', () => {
    expect(tourPath('go', { notion: 'hello-world', page: 1 })).toBe('/subjects/go/tour/hello-world')
    expect(tourPath('go', { notion: 'packages-imports', page: 2 })).toBe(
      '/subjects/go/tour/packages-imports?page=2',
    )
  })
})
