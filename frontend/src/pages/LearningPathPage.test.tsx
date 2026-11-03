import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_SUBJECTS } from '../test/render'

const SIGNED_IN = fakeSession({ status: 'authenticated', userName: 'Alice' })
const JAVA = { '/api/v1/subjects/java': { body: TEST_SUBJECTS[0] } }

function notion(slug: string, title: string, status: string, seenCount = 0) {
  return { slug, title, level: 'BEGINNER', priority: 1, status, cardCount: 6, seenCount }
}

describe('LearningPathPage', () => {
  it('shows each notion of each chapter with its status and the notions acquired', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...JAVA,
      '/api/v1/learning-paths/java': {
        body: {
          subjectSlug: 'java',
          track: 'LEARN',
          chapters: [
            {
              id: 'premiers-pas',
              title: 'Premiers pas',
              notions: [
                notion('hello-world', 'Le premier programme', 'ACQUIRED', 6),
                notion('types-primitifs', 'Les types primitifs', 'IN_PROGRESS', 2),
              ],
            },
            {
              id: 'objet',
              title: 'Objet',
              notions: [notion('heritage', 'L’héritage', 'LOCKED')],
            },
          ],
        },
      },
    })

    renderAt('/subjects/java/path', SIGNED_IN)

    expect(await screen.findByText(/1 \/ 3 notions acquises/)).toBeInTheDocument()
    expect(screen.getByText('Apprendre')).toBeInTheDocument()
    const firstSteps = screen.getByRole('region', { name: 'Premiers pas' })
    expect(within(firstSteps).getByText('Les types primitifs').closest('li')).toHaveTextContent(
      'En cours',
    )
    expect(within(firstSteps).getByText('Les types primitifs').closest('li')).toHaveTextContent(
      '2 / 6 cartes vues',
    )
    expect(screen.getByRole('region', { name: 'Objet' })).toHaveTextContent('Verrouillée')
  })

  it('explains that a subject without referential has no path yet', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...JAVA,
      '/api/v1/learning-paths/java': {
        body: { subjectSlug: 'java', track: 'PREPARE', chapters: [] },
      },
    })

    renderAt('/subjects/java/path', SIGNED_IN)

    expect(await screen.findByText(/pas encore de référentiel de notions/)).toBeInTheDocument()
  })
})
