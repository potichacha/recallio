import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, stubApi } from '../test/render'

describe('AuthCallbackPage', () => {
  it('goes back to the page requested before signing in', async () => {
    stubApi({ '/api/v1/themes': { body: [] } })

    const router = renderAt(
      '/auth/callback?code=abc&state=xyz',
      fakeSession({ status: 'authenticated', returnTo: '/themes' }),
    )

    expect(await screen.findByRole('heading', { name: 'Thèmes' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/themes')
  })

  it('explains when the sign-in failed', () => {
    renderAt('/auth/callback', fakeSession({ status: 'anonymous', error: 'access_denied' }))

    expect(screen.getByRole('heading', { name: 'Connexion impossible' })).toBeInTheDocument()
    expect(screen.getByText('access_denied')).toBeInTheDocument()
  })
})
