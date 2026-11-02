import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setAccessTokenProvider } from '../api/client'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_SUBJECTS } from '../test/render'

/** Un WebSocket factice : le test joue le rôle du serveur (messages, fermeture). */
class FakeWebSocket {
  static instances: FakeWebSocket[] = []
  onmessage?: (event: { data: string }) => void
  onclose?: (event: { code: number; reason: string }) => void
  closed = false

  readonly url: string
  readonly protocols: string[]

  constructor(url: string, protocols: string[]) {
    this.url = url
    this.protocols = protocols
    FakeWebSocket.instances.push(this)
  }

  close() {
    this.closed = true
  }

  receive(event: object) {
    act(() => this.onmessage?.({ data: JSON.stringify(event) }))
  }

  serverClose(code: number, reason = '') {
    act(() => this.onclose?.({ code, reason }))
  }
}

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const player = (userId: string, displayName: string, host = false) => ({
  userId,
  displayName,
  host,
})

const game = (players: object[]) => ({
  type: 'game',
  game: {
    id: 'game-1',
    groupId: 'g1',
    subject: 'java',
    hostId: 'u1',
    createdAt: '2026-11-02T18:00:00Z',
    status: 'LOBBY',
    players,
  },
})

/** La dernière connexion ouverte, une fois le jeton obtenu (asynchrone). */
async function socket() {
  await waitFor(() => expect(FakeWebSocket.instances.length).toBeGreaterThan(0))
  return FakeWebSocket.instances.at(-1)!
}

describe('LivePage', () => {
  beforeEach(() => {
    FakeWebSocket.instances = []
    vi.stubGlobal('WebSocket', FakeWebSocket)
    stubApi({ ...SIGNED_IN_API, '/api/v1/subjects': { body: TEST_SUBJECTS } })
  })

  afterEach(() => setAccessTokenProvider(async () => undefined))

  it('shows the players of the lobby, updated live', async () => {
    renderAt('/live/game-1', signedIn())

    const connection = await socket()
    expect(connection.url).toMatch(/^ws:\/\/.+\/api\/v1\/live\/game-1$/)
    connection.receive(game([player('u1', 'Alice', true)]))

    expect(await screen.findByRole('heading', { name: 'Partie live : Java' })).toBeInTheDocument()
    expect(screen.getByText(/Vous êtes l’hôte/)).toBeInTheDocument()
    const players = screen.getByRole('region', { name: 'Joueurs (1)' })
    expect(within(players).getByRole('listitem')).toHaveTextContent('Alice(vous)Hôte')

    connection.receive(game([player('u1', 'Alice', true), player('u2', 'Bob')]))
    expect(await screen.findByRole('region', { name: 'Joueurs (2)' })).toHaveTextContent('Bob')
  })

  it('carries the access token in the subprotocol, the browser cannot send headers', async () => {
    setAccessTokenProvider(async () => 'jeton')
    renderAt('/live/game-1', signedIn())

    const connection = await socket()
    expect(connection.protocols).toEqual([
      'bearer-token-carrier',
      encodeURIComponent('quarkus-http-upgrade#Authorization#Bearer jeton'),
    ])
  })

  it('explains a game that does not exist, or no longer', async () => {
    renderAt('/live/game-1', signedIn())

    ;(await socket()).serverClose(4404, 'Aucune partie')

    expect(await screen.findByRole('heading', { name: 'Partie introuvable' })).toBeInTheDocument()
  })

  it('offers to reconnect after a lost connection, with a new socket', async () => {
    renderAt('/live/game-1', signedIn())
    const lost = await socket()
    lost.serverClose(1006)

    await userEvent.click(await screen.findByRole('button', { name: 'Se reconnecter' }))

    await waitFor(() => expect(FakeWebSocket.instances).toHaveLength(2))
    expect(await screen.findByText('Connexion à la partie…')).toBeInTheDocument()
  })
})
