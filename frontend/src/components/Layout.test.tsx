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

const SUBJECTS = { '/api/v1/subjects': { body: [] } }

describe('Layout', () => {
  it('offers to sign in, coming back to the current page', async () => {
    stubApi(SUBJECTS)
    const session = fakeSession()
    renderAt('/subjects', session)

    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(session.login).toHaveBeenCalledWith('/subjects')
  })

  it('links to the profile under the display name of the user', async () => {
    stubApi({
      ...SUBJECTS,
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, displayName: 'Alice M.' } },
    })
    const session = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })
    renderAt('/subjects', session)

    expect(await screen.findByRole('link', { name: 'Alice M.' })).toHaveAttribute(
      'href',
      '/profile',
    )
    // Se déconnecter se fait depuis le profil : l'en-tête garde la place pour la progression.
    expect(screen.queryByRole('button', { name: 'Se déconnecter' })).not.toBeInTheDocument()
  })

  it('offers the five destinations in a single navigation, the current one marked', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/learn', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const navigation = screen.getByRole('navigation', { name: 'Navigation principale' })
    expect(
      within(navigation)
        .getAllByRole('link')
        .map((link) => link.textContent),
    ).toEqual(['Accueil', 'Cours', 'Tour', 'Révisions', 'Jouer'])
    expect(within(navigation).getByRole('link', { name: 'Cours' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('keeps a destination marked on the pages it opens (ADR 0052)', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/ranked', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const navigation = screen.getByRole('navigation', { name: 'Navigation principale' })
    expect(within(navigation).getByRole('link', { name: 'Jouer' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(navigation).getByRole('link', { name: 'Accueil' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('shows the streak, its freezes and the XP of the signed-in user', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/subjects', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    // Deux pastilles (ADR 0058) : la série, avec sa flamme et son unité, et l'XP.
    const streak = await screen.findByLabelText('Série : 3 jours')
    expect(streak).toHaveTextContent('3 jours')
    expect(streak).toHaveClass('rounded-full', 'font-black')
    expect(streak.querySelector('svg')).not.toBeNull()
    expect(screen.getByLabelText('Gels de streak : 1')).toBeInTheDocument()
    expect(screen.getByText('120 XP')).toHaveClass('rounded-full', 'font-black')
  })

  it('writes a large XP the French way, and a streak of one day in the singular', async () => {
    stubApi({
      ...SUBJECTS,
      ...SIGNED_IN_API,
      '/api/v1/me/progress': {
        body: { ...TEST_PROGRESS, totalXp: 1240, streak: 1, streakFreezes: 0 },
      },
    })
    renderAt('/subjects', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    expect(await screen.findByLabelText('Série : 1 jour')).toHaveTextContent('1 jour')
    // L'espace des milliers est une espace fine insécable.
    expect(screen.getByText(/^1\s240 XP$/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/Gels de streak/)).not.toBeInTheDocument()
  })
})
