import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_PROFILE } from '../test/render'

const THEMES = { '/api/v1/themes': { body: [] } }

describe('Layout', () => {
  it('offers to sign in, coming back to the current page', async () => {
    stubApi(THEMES)
    const session = fakeSession()
    renderAt('/themes', session)

    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(session.login).toHaveBeenCalledWith('/themes')
  })

  it('shows the display name of the profile with a link to it, and signs out', async () => {
    stubApi({
      ...THEMES,
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, displayName: 'Alice M.' } },
    })
    const session = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })
    renderAt('/themes', session)

    expect(await screen.findByRole('link', { name: 'Alice M.' })).toHaveAttribute(
      'href',
      '/profile',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Se déconnecter' }))

    expect(session.logout).toHaveBeenCalled()
  })

  it('shows the streak and the points of the signed-in user', async () => {
    stubApi({ ...THEMES, ...SIGNED_IN_API })
    renderAt('/themes', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    expect(await screen.findByLabelText('Série : 3 jours')).toBeInTheDocument()
    expect(screen.getByText('120 pts')).toBeInTheDocument()
  })
})
