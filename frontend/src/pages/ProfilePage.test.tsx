import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, stubApi } from '../test/render'

const PROFILE = {
  id: 'u1',
  displayName: 'Alice Martin',
  timezone: 'Europe/Paris',
  roles: ['admin'],
}
const SIGNED_IN = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })

describe('ProfilePage', () => {
  it('asks anonymous visitors to sign in and comes back to the profile afterwards', async () => {
    const session = fakeSession()
    renderAt('/profile', session)

    await userEvent.click(screen.getAllByRole('button', { name: 'Se connecter' })[1])

    expect(session.login).toHaveBeenCalledWith('/profile')
  })

  it('loads the profile and sends the browser timezone for the first login', async () => {
    const fetchMock = stubApi({ '/api/v1/me': { body: PROFILE } })

    renderAt('/profile', SIGNED_IN)

    expect(await screen.findByLabelText('Nom affiché')).toHaveValue('Alice Martin')
    expect(screen.getByLabelText('Fuseau horaire')).toHaveValue('Europe/Paris')
    expect(screen.getByText('Vous êtes administrateur.')).toBeInTheDocument()
    expect(sentRequests(fetchMock)[0].headers.get('X-Timezone')).toBeTruthy()
  })

  it('saves the new preferences', async () => {
    const fetchMock = stubApi({
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me': { body: { ...PROFILE, displayName: 'Alice M.' } },
    })
    renderAt('/profile', SIGNED_IN)

    const name = await screen.findByLabelText('Nom affiché')
    await userEvent.clear(name)
    await userEvent.type(name, 'Alice M.')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Profil enregistré.')
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ displayName: 'Alice M.', timezone: 'Europe/Paris' })
  })

  it('shows the error returned by the API', async () => {
    stubApi({
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me': {
        status: 422,
        body: {
          type: 'about:blank',
          title: 'Requête refusée',
          status: 422,
          code: 'user.invalid-timezone',
          detail: 'Fuseau horaire inconnu.',
        },
      },
    })
    renderAt('/profile', SIGNED_IN)

    await userEvent.click(await screen.findByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Fuseau horaire inconnu.')
  })
})
