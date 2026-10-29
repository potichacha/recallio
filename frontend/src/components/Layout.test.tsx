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

  it('shows the user name with a link to the profile, and signs out', async () => {
    stubApi(THEMES)
    const session = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })
    renderAt('/themes', session)

    expect(screen.getByRole('link', { name: 'Alice Martin' })).toHaveAttribute('href', '/profile')
    await userEvent.click(screen.getByRole('button', { name: 'Se déconnecter' }))

    expect(session.logout).toHaveBeenCalled()
  })
})
