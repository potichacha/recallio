import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'

/** Le catalogue des tests (Java et Go publiés avec leur Tour, Rust en brouillon), plus un framework sans Tour. */
const CATALOGUE = {
  '/api/v1/subjects': {
    body: [
      ...TEST_SUBJECTS,
      {
        ...TEST_SUBJECTS[0],
        id: 'spring-boot',
        slug: 'spring-boot',
        title: 'Spring Boot',
        category: 'FRAMEWORKS',
        hasTour: false,
      },
    ],
  },
}

/** Cinq cartes à revoir : seul ce que la porte « Apprendre » en lit. */
const DUE_TODAY = {
  overall: { total: 30, seen: 12, mastered: 4, dueNow: 5, dueToday: 7 },
  newToday: 8,
  subjects: [],
}

const LESSON = {
  slug: 'fonctions',
  title: 'Les fonctions',
  chapterTitle: 'Les bases',
  lesson: 'Une fonction **associe** une valeur à chaque argument.',
  inTour: true,
  previous: { slug: 'expressions', title: 'Les expressions' },
  next: null,
}

describe('the pages that open on the subjects (ADR 0052)', () => {
  it('opens the Tour of the subjects that have one, and announces the others', async () => {
    stubApi(CATALOGUE)
    renderAt('/tours')

    expect(await screen.findByRole('heading', { name: 'Le Tour' })).toBeInTheDocument()
    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(within(languages).getByRole('link', { name: 'Java' })).toHaveAttribute(
      'href',
      '/subjects/java/tour',
    )
    expect(within(languages).getByRole('link', { name: 'Go' })).toHaveAttribute(
      'href',
      '/subjects/go/tour',
    )
    const frameworks = screen.getByRole('region', { name: 'Frameworks' })
    const springBoot = within(frameworks).getByRole('button', { name: /Spring Boot/ })
    await userEvent.click(springBoot)
    expect(springBoot).toHaveTextContent('Work in progress')
    expect(screen.getByRole('status')).toHaveTextContent('Spring Boot : work in progress')
  })

  it('opens the door « Apprendre » on the subjects, each leading to its Tour, its course and its cards', async () => {
    stubApi(CATALOGUE)
    renderAt('/learn')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Tes sujets, pas à pas' }),
    ).toBeInTheDocument()
    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(within(languages).getByRole('link', { name: 'Le Tour de Java' })).toHaveAttribute(
      'href',
      '/subjects/java/tour',
    )
    expect(within(languages).getByRole('link', { name: 'Le cours de Java' })).toHaveAttribute(
      'href',
      '/subjects/java/course',
    )
    expect(within(languages).getByRole('link', { name: 'Les cartes de Java' })).toHaveAttribute(
      'href',
      '/subjects/java',
    )
    // Un sujet sans Tour n'en promet pas.
    const frameworks = screen.getByRole('region', { name: 'Frameworks' })
    expect(
      within(frameworks).getByRole('link', { name: 'Le cours de Spring Boot' }),
    ).toBeInTheDocument()
    expect(
      within(frameworks).queryByRole('link', { name: 'Le Tour de Spring Boot' }),
    ).not.toBeInTheDocument()
  })

  it('announces a subject in preparation, without opening it', async () => {
    stubApi(CATALOGUE)
    renderAt('/learn')

    const languages = await screen.findByRole('region', { name: 'Langages' })
    const rust = within(languages).getByRole('button', { name: /Rust/ })
    expect(rust).toHaveTextContent('En préparation')
    await userEvent.click(rust)
    expect(rust).toHaveTextContent('Work in progress')
    expect(screen.getByRole('status')).toHaveTextContent('Rust : work in progress')
    expect(within(languages).queryByRole('link', { name: /Rust/ })).not.toBeInTheDocument()
  })

  it('keeps the pages of the Tours and of all the cards one click away', async () => {
    stubApi(CATALOGUE)
    renderAt('/learn')

    const more = await screen.findByRole('navigation', { name: 'Aussi dans Apprendre' })
    expect(
      within(more).getByRole('link', { name: 'Tous les Tours, langage par langage' }),
    ).toHaveAttribute('href', '/tours')
    expect(
      within(more).getByRole('link', { name: 'Parcourir toutes les cartes, thème par thème' }),
    ).toHaveAttribute('href', '/subjects')
  })

  it('leads a visitor to the reviews of the day, which ask to sign in', async () => {
    stubApi(CATALOGUE)
    renderAt('/learn')

    const reviews = await screen.findByRole('link', { name: /^Révisions du jour/ })
    expect(reviews).toHaveAttribute('href', '/review')
    expect(reviews).toHaveTextContent('Les cartes à revoir aujourd’hui, pour ne rien oublier')
  })

  it('tells a signed-in user how many cards are due today', async () => {
    stubApi({ ...CATALOGUE, ...SIGNED_IN_API, '/api/v1/me/review-stats': { body: DUE_TODAY } })
    renderAt('/learn', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    expect(
      await screen.findByRole('link', { name: 'Révisions du jour 5 cartes à revoir' }),
    ).toHaveAttribute('href', '/review')
  })

  it('lets an admin open a draft subject, to proofread it', async () => {
    stubApi({
      ...CATALOGUE,
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, roles: ['admin'] } },
      '/api/v1/me/review-stats': { body: DUE_TODAY },
    })
    renderAt('/learn', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(
      await within(languages).findByRole('link', { name: 'Le cours de Rust' }),
    ).toHaveAttribute('href', '/subjects/rust/course')
  })

  it('reads the lesson of a notion, then leads to practice, the Tour and the next notions', async () => {
    stubApi({
      '/api/v1/subjects/java': { body: TEST_SUBJECTS[0] },
      '/api/v1/subjects/java/notions/fonctions': { body: LESSON },
    })
    renderAt('/subjects/java/course/fonctions')

    expect(await screen.findByRole('heading', { name: 'Les fonctions' })).toBeInTheDocument()
    expect(screen.getByText('associe')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'S’entraîner sur cette notion' })).toHaveAttribute(
      'href',
      '/subjects/java/practice/fonctions',
    )
    expect(screen.getByRole('link', { name: 'Sa page dans le Tour' })).toHaveAttribute(
      'href',
      '/subjects/java/tour/fonctions',
    )
    const neighbours = screen.getByRole('navigation', { name: 'Leçons voisines' })
    expect(within(neighbours).getByRole('link', { name: '← Les expressions' })).toHaveAttribute(
      'href',
      '/subjects/java/course/expressions',
    )
    expect(await screen.findByRole('link', { name: '← Le cours de Java' })).toHaveAttribute(
      'href',
      '/subjects/java/course',
    )
  })

  it('says so when the notion does not exist', async () => {
    stubApi({
      '/api/v1/subjects/java': { body: TEST_SUBJECTS[0] },
      '/api/v1/subjects/java/notions/inconnue': {
        status: 404,
        body: { status: 404, code: 'content.notion-not-found' },
      },
    })
    renderAt('/subjects/java/course/inconnue')

    expect(await screen.findByRole('heading', { name: 'Notion introuvable' })).toBeInTheDocument()
  })

  it('gathers the ranked mode and the groups under « Jouer »', async () => {
    renderAt('/play')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Mesure-toi aux autres' }),
    ).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: /Classé/ })).toHaveAttribute('href', '/ranked')
    expect(screen.getByRole('link', { name: /Groupes/ })).toHaveAttribute('href', '/groups')
  })
})
