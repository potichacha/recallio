import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const FIRST = 'a'.repeat(32)
const SECOND = 'b'.repeat(32)
const EXPIRES_AT = '2026-11-08T12:00:00Z'

function group(myRole: 'OWNER' | 'MEMBER') {
  return {
    id: 'g1',
    name: 'Équipe backend',
    createdAt: '2026-11-01T10:00:00Z',
    myRole,
    members: [
      {
        userId: 'u1',
        displayName: 'Alice',
        role: 'OWNER',
        joinedAt: '2026-11-01T10:00:00Z',
        me: myRole === 'OWNER',
      },
    ],
  }
}

const linkTo = (token: string) => `${window.location.origin}/join/${token}`

describe('InvitationPanel', () => {
  it('creates a link the owner can share', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('OWNER') },
      '/api/v1/groups/g1/invitation': { status: 404, body: { code: 'group.no-active-invitation' } },
      'POST /api/v1/groups/g1/invitation': {
        status: 201,
        body: { token: FIRST, expiresAt: EXPIRES_AT },
      },
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Inviter des membres' })
    await userEvent.click(
      await within(panel).findByRole('button', { name: "Créer un lien d'invitation" }),
    )

    expect(await within(panel).findByLabelText("Lien d'invitation")).toHaveValue(linkTo(FIRST))
    expect(panel).toHaveTextContent(/Valable jusqu'au \d+ novembre 2026/)
    expect(sentRequests(fetchMock).some((request) => request.method === 'POST')).toBe(true)
  })

  it('replaces the link, then revokes it', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('OWNER') },
      '/api/v1/groups/g1/invitation': { body: { token: FIRST, expiresAt: EXPIRES_AT } },
      'POST /api/v1/groups/g1/invitation': {
        status: 201,
        body: { token: SECOND, expiresAt: EXPIRES_AT },
      },
      'DELETE /api/v1/groups/g1/invitation': { status: 204, body: null },
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Inviter des membres' })
    const link = await within(panel).findByLabelText("Lien d'invitation")
    expect(link).toHaveValue(linkTo(FIRST))

    await userEvent.click(within(panel).getByRole('button', { name: 'Nouveau lien' }))
    expect(await within(panel).findByDisplayValue(linkTo(SECOND))).toBeInTheDocument()

    await userEvent.click(within(panel).getByRole('button', { name: 'Désactiver le lien' }))
    expect(
      await within(panel).findByRole('button', { name: "Créer un lien d'invitation" }),
    ).toBeInTheDocument()
    expect(within(panel).queryByLabelText("Lien d'invitation")).not.toBeInTheDocument()
  })

  it('is hidden from members, who cannot invite', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1': { body: group('MEMBER') },
    })
    renderAt('/groups/g1', signedIn())

    expect(await screen.findByRole('button', { name: 'Quitter le groupe' })).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Inviter des membres' })).not.toBeInTheDocument()
    expect(sentRequests(fetchMock).some((request) => request.url.endsWith('/invitation'))).toBe(
      false,
    )
  })
})
