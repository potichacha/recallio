import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, stubApi } from '../test/render'

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
      '/api/v1/me': {
        body: { id: 'u1', displayName: 'Alice M.', timezone: 'Europe/Paris', roles: [] },
      },
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
})
