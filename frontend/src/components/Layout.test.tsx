import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_PROGRESS,
} from '../test/render'
import { destinationOf } from './destinations'

const SUBJECTS = { '/api/v1/subjects': { body: [] } }

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const navigation = () => screen.getByRole('navigation', { name: 'Navigation principale' })

describe('Layout', () => {
  it('offers to sign in, coming back to the current page', async () => {
    stubApi(SUBJECTS)
    const session = fakeSession()
    renderAt('/subjects', session)

    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(session.login).toHaveBeenCalledWith('/subjects')
  })

  it('opens the profile from the avatar, named after the user', async () => {
    stubApi({
      ...SUBJECTS,
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, displayName: 'Alice M.' } },
    })
    const session = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })
    renderAt('/subjects', session)

    const avatar = await screen.findByRole('link', { name: 'Alice M.' })
    expect(avatar).toHaveAttribute('href', '/profile')
    // L'initiale du nom, dans un rond.
    expect(avatar).toHaveTextContent(/^A$/)
    // Se déconnecter se fait depuis le profil.
    expect(screen.queryByRole('button', { name: 'Se déconnecter' })).not.toBeInTheDocument()
  })

  it('offers the four destinations in a single navigation, the current one marked (ADR 0077)', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/learn', signedIn())

    expect(screen.getAllByRole('navigation', { name: 'Navigation principale' })).toHaveLength(1)
    const links = within(navigation()).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual([
      'Accueil',
      'Apprendre',
      'S’entraîner',
      'Jouer',
    ])
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/',
      '/learn',
      '/practice',
      '/play',
    ])
    expect(within(navigation()).getByRole('link', { name: 'Apprendre' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(navigation()).getByRole('link', { name: 'Accueil' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('shows a visitor the same four destinations', async () => {
    stubApi(SUBJECTS)
    renderAt('/subjects')

    expect(
      within(navigation())
        .getAllByRole('link')
        .map((link) => link.textContent),
    ).toEqual(['Accueil', 'Apprendre', 'S’entraîner', 'Jouer'])
  })

  it('invites a visitor to sign in on a door that needs an account', async () => {
    const session = fakeSession()
    renderAt('/practice', session)

    expect(await screen.findByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
    expect(within(navigation()).getByRole('link', { name: 'S’entraîner' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('keeps a destination marked on the pages it opens', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/ranked', signedIn())

    expect(within(navigation()).getByRole('link', { name: 'Jouer' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(navigation()).getByRole('link', { name: 'Accueil' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  // Chaque écran existant est rangé sous sa porte (ADR 0077).
  it.each([
    ['/', '/'],
    ['/progress', '/'],
    ['/learn', '/learn'],
    ['/tours', '/learn'],
    ['/review', '/learn'],
    ['/subjects', '/learn'],
    ['/subjects/java', '/learn'],
    ['/subjects/java/course', '/learn'],
    ['/subjects/java/course/fonctions', '/learn'],
    ['/subjects/java/tour/bienvenue', '/learn'],
    ['/subjects/java/level-check', '/learn'],
    ['/themes/java-bases', '/learn'],
    ['/practice', '/practice'],
    ['/subjects/java/exam', '/practice'],
    ['/subjects/java/mistakes', '/practice'],
    ['/subjects/java/practice/fonctions', '/practice'],
    ['/play', '/play'],
    ['/daily/java', '/play'],
    ['/ranked/duels/1', '/play'],
    ['/groups', '/play'],
    ['/challenges/1', '/play'],
    ['/live/1', '/play'],
    ['/join/abc', '/play'],
  ])('ranges %s under the destination %s', (path, destination) => {
    expect(destinationOf(path)).toBe(destination)
  })

  it.each([
    '/profile',
    '/profile/subjects',
    '/welcome',
    '/try/go',
    '/mentions-legales',
    '/u/alice',
  ])('marks no destination on %s', (path) => {
    expect(destinationOf(path)).toBeUndefined()
  })

  it('shows the streak and the XP of the signed-in user, each leading to their progress', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/subjects', signedIn())

    const streak = await screen.findByRole('link', { name: 'Série : 3 jours' })
    expect(streak).toHaveTextContent('3 jours')
    expect(streak).toHaveAttribute('href', '/progress')
    // La flamme : le chiffre n'est jamais seul.
    expect(streak.querySelector('svg')).not.toBeNull()
    const xp = screen.getByRole('link', { name: '120 XP' })
    expect(xp).toHaveTextContent('120 XP')
    expect(xp).toHaveAttribute('href', '/progress')
  })

  it('writes a large XP the French way, and a streak of one day in the singular', async () => {
    stubApi({
      ...SUBJECTS,
      ...SIGNED_IN_API,
      '/api/v1/me/progress': {
        body: { ...TEST_PROGRESS, totalXp: 1240, streak: 1, streakFreezes: 0 },
      },
    })
    renderAt('/subjects', signedIn())

    expect(await screen.findByRole('link', { name: 'Série : 1 jour' })).toHaveTextContent('1 jour')
    // L'espace des milliers est une espace fine insécable.
    expect(screen.getByRole('link', { name: /^1\s240 XP$/ })).toBeInTheDocument()
  })

  it('shows a newcomer neither a streak nor an XP at zero', async () => {
    stubApi({
      ...SUBJECTS,
      ...SIGNED_IN_API,
      '/api/v1/me/progress': {
        body: { ...TEST_PROGRESS, totalXp: 0, streak: 0, streakFreezes: 0 },
      },
    })
    renderAt('/subjects', signedIn())

    await screen.findByRole('link', { name: 'Alice' })
    expect(screen.queryByRole('link', { name: /Série/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /XP$/ })).not.toBeInTheDocument()
  })

  it('keeps the skip link, the legal footer and the name of the application', async () => {
    stubApi(SUBJECTS)
    renderAt('/subjects')

    expect(screen.getByRole('link', { name: 'Aller au contenu' })).toHaveAttribute(
      'href',
      '#contenu',
    )
    expect(screen.getByRole('main')).toHaveAttribute('id', 'contenu')
    const legal = screen.getByRole('navigation', { name: 'Informations légales' })
    expect(within(legal).getByRole('link', { name: 'Mentions légales' })).toHaveAttribute(
      'href',
      '/mentions-legales',
    )
    // Le logo ramène à l'accueil : dans le menu latéral d'un ordinateur, et en tête d'un téléphone pour un visiteur.
    for (const brand of screen.getAllByRole('link', { name: 'Recallio' })) {
      expect(brand).toHaveAttribute('href', '/')
    }
  })
})
