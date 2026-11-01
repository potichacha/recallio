import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'

/** Le groupe n'a pas encore de lien d'invitation. */
const NO_INVITATION = { status: 404, body: { code: 'group.no-active-invitation' } }

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const member = (userId: string, displayName: string, overrides: Record<string, unknown> = {}) => ({
  userId,
  displayName,
  role: 'MEMBER',
  joinedAt: '2026-11-01T10:00:00Z',
  me: false,
  ...overrides,
})

function group(myRole: 'OWNER' | 'MEMBER') {
  return {
    id: 'g1',
    name: 'Équipe backend',
    createdAt: '2026-11-01T10:00:00Z',
    myRole,
    members: [
      member('u1', 'Alice', { role: 'OWNER', me: myRole === 'OWNER' }),
      member('u2', 'Bob', { me: myRole === 'MEMBER' }),
    ],
  }
}

describe('GroupPage', () => {
  it('lets the owner remove a member', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('OWNER') },
      '/api/v1/groups/g1/invitation': NO_INVITATION,
      'DELETE /api/v1/groups/g1/members/u2': { status: 204, body: null },
    })
    renderAt('/groups/g1', signedIn())

    const members = await screen.findByRole('region', { name: /Membres \(2\)/ })
    expect(within(members).getByText('Alice').closest('li')).toHaveTextContent(
      'Alice (vous)Créateur',
    )
    expect(within(members).queryByRole('button', { name: 'Retirer Alice' })).not.toBeInTheDocument()
    await userEvent.click(within(members).getByRole('button', { name: 'Retirer Bob' }))

    expect(
      sentRequests(fetchMock).some(
        (request) => request.method === 'DELETE' && request.url.endsWith('/members/u2'),
      ),
    ).toBe(true)
    expect(screen.queryByRole('button', { name: 'Quitter le groupe' })).not.toBeInTheDocument()
  })

  it('asks the owner to confirm before deleting the group, then goes back to the list', async () => {
    const api = {
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('OWNER') } as { status?: number; body: unknown },
      '/api/v1/groups/g1/invitation': NO_INVITATION,
      'DELETE /api/v1/groups/g1': { status: 204, body: null },
      '/api/v1/groups': { body: [] },
    }
    stubApi(api)
    const router = renderAt('/groups/g1', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Supprimer le groupe' }))
    // Comme le serveur : une fois supprimé, le groupe répond 404 (bug corrigé : la page restait sur « introuvable »).
    api['/api/v1/groups/g1'] = { status: 404, body: { status: 404, code: 'group.not-found' } }
    await userEvent.click(screen.getByRole('button', { name: 'Confirmer la suppression' }))

    expect(await screen.findByRole('heading', { name: 'Mes groupes' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/groups')
  })

  it('lets a member leave the group, without managing it', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('MEMBER') },
      'DELETE /api/v1/groups/g1/members/me': { status: 204, body: null },
      '/api/v1/groups': { body: [] },
    })
    const router = renderAt('/groups/g1', signedIn())

    await screen.findByRole('heading', { name: 'Équipe backend' })
    expect(screen.queryByRole('button', { name: /Retirer/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Supprimer le groupe' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Quitter le groupe' }))

    expect(await screen.findByRole('heading', { name: 'Mes groupes' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/groups')
  })

  it('shows a not-found message for a group the user does not belong to', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/inconnu': { status: 404, body: { status: 404, code: 'group.not-found' } },
    })
    renderAt('/groups/inconnu', signedIn())

    expect(await screen.findByRole('heading', { name: 'Groupe introuvable' })).toBeInTheDocument()
  })
})
