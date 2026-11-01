import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_GROUP_SETTINGS,
  TEST_LEADERBOARD,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const GROUP = {
  id: 'g1',
  name: 'Équipe backend',
  createdAt: '2026-11-01T10:00:00Z',
  myRole: 'OWNER',
  settings: TEST_GROUP_SETTINGS,
  members: [
    {
      userId: 'u1',
      displayName: 'Alice',
      role: 'OWNER',
      joinedAt: '2026-11-01T10:00:00Z',
      me: true,
    },
  ],
}

describe('GroupsPage', () => {
  it('asks anonymous visitors to sign in', () => {
    renderAt('/groups')

    expect(screen.getByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
  })

  it('lists the groups of the user with their size and role', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups': {
        body: [
          { id: 'g1', name: 'Équipe backend', myRole: 'OWNER', memberCount: 4 },
          { id: 'g2', name: 'Promo 2026', myRole: 'MEMBER', memberCount: 1 },
        ],
      },
    })
    renderAt('/groups', signedIn())

    const owned = await screen.findByRole('link', { name: /Équipe backend/ })
    expect(owned).toHaveAttribute('href', '/groups/g1')
    expect(owned).toHaveTextContent('4 membres · Vous l’avez créé')
    expect(screen.getByRole('link', { name: /Promo 2026/ })).toHaveTextContent('1 membre')
  })

  it('invites to create a first group', async () => {
    stubApi({ ...SIGNED_IN_API, '/api/v1/groups': { body: [] } })
    renderAt('/groups', signedIn())

    expect(await screen.findByText(/Vous ne faites partie d'aucun groupe/)).toBeInTheDocument()
  })

  it('creates a group and opens it', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups': { body: [] },
      'POST /api/v1/groups': { status: 201, body: GROUP },
      '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
      '/api/v1/groups/g1': { body: GROUP },
      '/api/v1/groups/g1/invitation': { status: 404, body: { code: 'group.no-active-invitation' } },
    })
    const router = renderAt('/groups', signedIn())

    const create = await screen.findByRole('button', { name: 'Créer' })
    expect(create).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Nom du groupe'), 'Équipe backend')
    await userEvent.click(create)

    expect(await screen.findByRole('heading', { name: 'Équipe backend' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/groups/g1')
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    expect(await post?.json()).toEqual({ name: 'Équipe backend' })
  })
})
