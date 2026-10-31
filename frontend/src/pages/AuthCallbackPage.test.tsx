import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi } from '../test/render'

describe('AuthCallbackPage', () => {
  it('goes back to the page requested before signing in', async () => {
    stubApi({
      '/api/v1/subjects': { body: [] },
      ...SIGNED_IN_API,
    })

    const router = renderAt(
      '/auth/callback?code=abc&state=xyz',
      fakeSession({ status: 'authenticated', returnTo: '/subjects' }),
    )

    expect(await screen.findByRole('heading', { name: 'Sujets' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/subjects')
  })

  it('explains when the sign-in failed', () => {
    renderAt('/auth/callback', fakeSession({ status: 'anonymous', error: 'access_denied' }))

    expect(screen.getByRole('heading', { name: 'Connexion impossible' })).toBeInTheDocument()
    expect(screen.getByText('access_denied')).toBeInTheDocument()
  })
})
