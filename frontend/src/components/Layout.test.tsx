import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_PROFILE } from '../test/render'

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
    renderAt('/subjects', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const navigation = screen.getByRole('navigation', { name: 'Navigation principale' })
    expect(
      within(navigation)
        .getAllByRole('link')
        .map((link) => link.textContent),
    ).toEqual(['Accueil', 'Réviser', 'Apprendre', 'Classé', 'Groupes'])
    expect(within(navigation).getByRole('link', { name: 'Apprendre' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('shows the streak, its freezes and the XP of the signed-in user', async () => {
    stubApi({ ...SUBJECTS, ...SIGNED_IN_API })
    renderAt('/subjects', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    expect(await screen.findByLabelText('Série : 3 jours')).toBeInTheDocument()
    expect(screen.getByLabelText('Gels de streak : 1')).toBeInTheDocument()
    expect(screen.getByText('120 XP')).toBeInTheDocument()
  })
})
