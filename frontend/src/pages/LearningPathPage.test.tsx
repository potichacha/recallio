import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_SUBJECTS } from '../test/render'

const SIGNED_IN = fakeSession({ status: 'authenticated', userName: 'Alice' })
const JAVA = { '/api/v1/subjects/java': { body: TEST_SUBJECTS[0] } }

const counts = (overrides = {}) => ({
  total: 0,
  discovered: 0,
  acquired: 0,
  mastered: 0,
  expert: 0,
  ...overrides,
})

const toDiscover = { level: 'DISCOVERED', cards: 0, cardsNeeded: 1, formats: 0, formatsNeeded: 0 }

function notion(slug: string, title: string, status: string, mastery: string, extra = {}) {
  return {
    slug,
    title,
    level: 'BEGINNER',
    priority: 1,
    status,
    cardCount: 6,
    seenCount: 0,
    mastery,
    nextLevel: { ...toDiscover, missingFormats: [] },
    ...extra,
  }
}

/** Deux domaines : « Les bases », où l'on en est, puis « Les collections », pas encore commencé. */
const PATH = {
  subjectSlug: 'java',
  track: 'LEARN',
  counts: counts({ total: 4, discovered: 2, acquired: 1, mastered: 1 }),
  domains: [
    {
      id: 'bases',
      title: 'Les bases',
      counts: counts({ total: 3, discovered: 2, acquired: 1, mastered: 1 }),
      chapters: [
        {
          id: 'premiers-pas',
          title: 'Premiers pas',
          notions: [
            notion('hello-world', 'Le premier programme', 'ACQUIRED', 'MASTERED', {
              seenCount: 6,
              nextLevel: {
                level: 'EXPERT',
                cards: 2,
                cardsNeeded: 5,
                formats: 2,
                formatsNeeded: 3,
                missingFormats: ['CODING_EXERCISE'],
              },
            }),
            notion('types-primitifs', 'Les types primitifs', 'IN_PROGRESS', 'DISCOVERED', {
              seenCount: 2,
              nextLevel: {
                level: 'ACQUIRED',
                cards: 1,
                cardsNeeded: 5,
                formats: 0,
                formatsNeeded: 0,
                missingFormats: [],
              },
            }),
          ],
        },
        {
          id: 'objet',
          title: 'Objet',
          notions: [notion('heritage', 'L’héritage', 'LOCKED', 'NEW')],
        },
      ],
    },
    {
      id: 'collections',
      title: 'Les collections',
      counts: counts({ total: 1 }),
      chapters: [
        {
          id: 'listes',
          title: 'Listes',
          notions: [notion('arraylist', 'ArrayList', 'AVAILABLE', 'NEW')],
        },
      ],
    },
  ],
}

describe('LearningPathPage', () => {
  it('maps the mastery of the subject, then of each domain and each notion', async () => {
    stubApi({ ...SIGNED_IN_API, ...JAVA, '/api/v1/learning-paths/java': { body: PATH } })

    renderAt('/subjects/java/path', SIGNED_IN)

    expect(
      await screen.findByRole('img', {
        name: 'Java : 1 / 4 notions acquises · 1 maîtrisée · 0 experte',
      }),
    ).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByText('Apprendre')).toBeInTheDocument()
    const basics = screen.getByRole('region', { name: 'Les bases' })
    expect(basics).toHaveTextContent('1 / 3 notions acquises · 1 maîtrisée · 0 experte')

    const firstSteps = within(basics).getByRole('region', { name: 'Premiers pas' })
    const primitives = within(firstSteps).getByText('Les types primitifs').closest('li')
    expect(primitives).toHaveTextContent('Découverte')
    expect(primitives).toHaveTextContent('2 / 6 cartes vues')
    expect(primitives).toHaveTextContent('Pour l’acquérir : 1 / 5 cartes réussies')
    expect(within(firstSteps).getByText('Le premier programme').closest('li')).toHaveTextContent(
      'Maîtrisée',
    )
    expect(within(firstSteps).getByText('Le premier programme').closest('li')).toHaveTextContent(
      'à réussir : l’exercice de code',
    )

    const object = within(basics).getByRole('region', { name: 'Objet' })
    expect(object).toHaveTextContent('Verrouillée')
    expect(object).not.toHaveTextContent('Pour la découvrir')
  })

  it('offers to share a chapter once each of its notions is mastered', async () => {
    const mastered = { ...PATH.domains[0].chapters[0] }
    mastered.notions = mastered.notions.map((entry) => ({ ...entry, mastery: 'MASTERED' }))
    const path = {
      ...PATH,
      domains: [{ ...PATH.domains[0], chapters: [mastered, ...PATH.domains[0].chapters.slice(1)] }],
    }
    stubApi({ ...SIGNED_IN_API, ...JAVA, '/api/v1/learning-paths/java': { body: path } })

    renderAt('/subjects/java/path', SIGNED_IN)

    const firstSteps = await screen.findByRole('region', { name: 'Premiers pas' })
    expect(firstSteps).toHaveTextContent('Maîtrisé')
    expect(within(firstSteps).getByRole('button', { name: 'Partager' })).toBeInTheDocument()
    const object = screen.getByRole('region', { name: 'Objet' })
    expect(within(object).queryByRole('button', { name: 'Partager' })).not.toBeInTheDocument()
  })

  it('unfolds the domain where the user stands, and folds the others', async () => {
    stubApi({ ...SIGNED_IN_API, ...JAVA, '/api/v1/learning-paths/java': { body: PATH } })

    renderAt('/subjects/java/path', SIGNED_IN)

    expect(await screen.findByRole('region', { name: 'Premiers pas' })).toBeVisible()
    const collections = screen.getByRole('region', { name: 'Les collections' })
    expect(collections).toHaveTextContent('1 chapitre · 1 notion')
    expect(
      within(collections).getByRole('region', { name: 'Listes', hidden: true }),
    ).not.toBeVisible()
  })

  it('unfolds only the chapter where the user stands in the open domain', async () => {
    stubApi({ ...SIGNED_IN_API, ...JAVA, '/api/v1/learning-paths/java': { body: PATH } })

    renderAt('/subjects/java/path', SIGNED_IN)

    const firstSteps = await screen.findByRole('region', { name: 'Premiers pas' })
    expect(within(firstSteps).getByRole('list', { name: 'Premiers pas' })).toBeVisible()
    const object = screen.getByRole('region', { name: 'Objet' })
    expect(within(object).getByRole('list', { name: 'Objet', hidden: true })).not.toBeVisible()
  })

  it('sends who learns to the Tour for a notion that waits to be validated there (ADR 0059)', async () => {
    const waiting = notion('boucles', 'Les boucles', 'TOUR_FIRST', 'NEW')
    const path = {
      ...PATH,
      domains: [
        {
          ...PATH.domains[0],
          chapters: [{ ...PATH.domains[0].chapters[0], notions: [waiting] }],
        },
      ],
    }
    stubApi({ ...SIGNED_IN_API, ...JAVA, '/api/v1/learning-paths/java': { body: path } })

    renderAt('/subjects/java/path', SIGNED_IN)

    const item = (await screen.findByText('Les boucles')).closest('li') as HTMLElement
    expect(item).toHaveTextContent('À lire dans le Tour')
    expect(
      within(item).getByRole('link', {
        name: 'La découvrir dans le Tour, pour la réviser ensuite',
      }),
    ).toHaveAttribute('href', '/subjects/java/tour/boucles')
    expect(item).not.toHaveTextContent('Pour la découvrir :')
  })

  it('explains that a subject without referential has no path yet', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...JAVA,
      '/api/v1/learning-paths/java': {
        body: { subjectSlug: 'java', track: 'PREPARE', counts: counts(), domains: [] },
      },
    })

    renderAt('/subjects/java/path', SIGNED_IN)

    expect(await screen.findByText(/pas encore de référentiel de notions/)).toBeInTheDocument()
  })
})
