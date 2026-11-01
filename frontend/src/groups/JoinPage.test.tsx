import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Bob' })

const TOKEN = 'a'.repeat(32)
const INVITATION = `/api/v1/invitations/${TOKEN}`

const invitation = (alreadyMember: boolean) => ({
  groupId: 'g1',
  groupName: 'Équipe backend',
  memberCount: 3,
  expiresAt: '2026-11-08T12:00:00Z',
  alreadyMember,
})

const JOINED_GROUP = {
  id: 'g1',
  name: 'Équipe backend',
  createdAt: '2026-11-01T10:00:00Z',
  myRole: 'MEMBER',
  members: [
    {
      userId: 'u1',
      displayName: 'Alice',
      role: 'OWNER',
      joinedAt: '2026-11-01T10:00:00Z',
      me: false,
    },
    {
      userId: 'u2',
      displayName: 'Bob',
      role: 'MEMBER',
      joinedAt: '2026-11-02T10:00:00Z',
      me: true,
    },
  ],
}

describe('JoinPage', () => {
  it('asks the guest to sign in first, then comes back to the invitation', async () => {
    const session = fakeSession()
    renderAt(`/join/${TOKEN}`, session)

    const prompt = screen.getByRole('heading', { name: 'Connexion requise' }).closest('section')
    await userEvent.click(within(prompt!).getByRole('button', { name: 'Se connecter' }))

    expect(session.login).toHaveBeenCalledWith(`/join/${TOKEN}`)
  })

  it('shows the group, then joins it and opens it', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [INVITATION]: { body: invitation(false) },
      [`POST ${INVITATION}/accept`]: { body: JOINED_GROUP },
    })
    const router = renderAt(`/join/${TOKEN}`, signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Rejoindre « Équipe backend »' }),
    ).toBeInTheDocument()
    expect(screen.getByText(/Ce groupe compte 3 membres/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Rejoindre le groupe' }))

    expect(await screen.findByRole('region', { name: 'Membres (2)' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/groups/g1')
  })

  it('links a member straight to the group', async () => {
    stubApi({ ...SIGNED_IN_API, [INVITATION]: { body: invitation(true) } })
    renderAt(`/join/${TOKEN}`, signedIn())

    expect(await screen.findByText('Vous faites déjà partie de ce groupe.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ouvrir le groupe' })).toHaveAttribute(
      'href',
      '/groups/g1',
    )
    expect(screen.queryByRole('button', { name: 'Rejoindre le groupe' })).not.toBeInTheDocument()
  })

  it('explains that an expired or replaced link no longer works', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [INVITATION]: { status: 404, body: { code: 'group.invitation-not-found' } },
    })
    renderAt(`/join/${TOKEN}`, signedIn())

    expect(await screen.findByRole('heading', { name: 'Invitation invalide' })).toBeInTheDocument()
  })

  it('tells the guest when the group is full', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [INVITATION]: { body: invitation(false) },
      [`POST ${INVITATION}/accept`]: {
        status: 409,
        body: { code: 'group.full', detail: 'Ce groupe compte déjà 100 membres, le maximum.' },
      },
    })
    renderAt(`/join/${TOKEN}`, signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Rejoindre le groupe' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ce groupe compte déjà 100 membres, le maximum.',
    )
  })
})
