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

  it('opens the course of a subject, with a way to browse all its cards', async () => {
    stubApi(CATALOGUE)
    renderAt('/learn')

    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(within(languages).getByRole('link', { name: 'Java' })).toHaveAttribute(
      'href',
      '/subjects/java/course',
    )
    expect(within(languages).getByRole('button', { name: /Rust/ })).toHaveTextContent(
      'En préparation',
    )
    expect(
      screen.getByRole('link', { name: 'Parcourir toutes les cartes, thème par thème' }),
    ).toHaveAttribute('href', '/subjects')
  })

  it('lets an admin open a draft subject, to proofread it', async () => {
    stubApi({
      ...CATALOGUE,
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, roles: ['admin'] } },
    })
    renderAt('/learn', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(await within(languages).findByRole('link', { name: 'Rust' })).toHaveAttribute(
      'href',
      '/subjects/rust/course',
    )
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

  it('gathers the ranked mode and the groups under « Jouer »', () => {
    renderAt('/play')

    expect(screen.getByRole('link', { name: /Classé/ })).toHaveAttribute('href', '/ranked')
    expect(screen.getByRole('link', { name: /Groupes/ })).toHaveAttribute('href', '/groups')
  })
})
